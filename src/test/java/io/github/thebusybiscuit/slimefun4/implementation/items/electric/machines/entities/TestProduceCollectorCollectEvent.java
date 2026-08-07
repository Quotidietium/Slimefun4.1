package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.ProduceCollectorCollectEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the produce collector API expansion:
 * {@link ProduceCollectorCollectEvent}, exercised by driving the real
 * {@link ProduceCollector#findNextRecipe(BlockMenu)} against a {@link BlockStorage}-backed
 * collector block stocked with a bucket, with an adult cow standing in range.
 * <p>
 * A match consumes the input bucket and returns the milk {@link AnimalProduce}, so tests
 * assert the outcome end-to-end: a cancelled event keeps the bucket and yields no recipe.
 *
 * @author Zurker
 */
class TestProduceCollectorCollectEvent {

    private static final int INPUT_SLOT = 19;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ProduceCollector collector;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "produce_collector_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_PRODUCE_COLLECTOR", Material.DISPENSER, "&fTest Produce Collector");
        Slimefun.getItemCfg().setValue("TEST_PRODUCE_COLLECTOR.enabled", true);
        collector = new ProduceCollector(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        collector.setCapacity(128).setEnergyConsumption(4).setProcessingSpeed(1);
        collector.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    /**
     * Places the collector as a real block backed by {@link BlockStorage} with the given
     * item in its first input slot (may be {@code null} for an empty slot).
     */
    private BlockMenu placeCollector(int x, int z, ItemStack input) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", collector.getId(), true);

        BlockMenu menu = BlockStorage.getInventory(b);

        if (input != null) {
            menu.replaceExistingItem(INPUT_SLOT, input);
        }

        return menu;
    }

    private MachineRecipe findRecipe(BlockMenu menu) {
        return collector.findNextRecipe(menu);
    }

    /**
     * {@link BlockMenu#consumeItem(int)} leaves a zero-amount stack behind instead of clearing
     * the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertInputConsumed(BlockMenu menu) {
        ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
        Assertions.assertTrue(slot == null || slot.getAmount() == 0, "The input item must have been consumed, got: " + slot);
    }

    @Test
    @DisplayName("ProduceCollectorCollectEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        AnimalProduce produce = new AnimalProduce(new ItemStack(Material.BUCKET), new ItemStack(Material.MILK_BUCKET), n -> true);

        ProduceCollectorCollectEvent event = new ProduceCollectorCollectEvent(collector, b, produce);

        Assertions.assertEquals(collector, event.getCollector());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(produce, event.getProduce());
        Assertions.assertEquals(Material.MILK_BUCKET, event.getProduce().getOutput()[0].getType());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ProduceCollectorCollectEvent(null, b, produce));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ProduceCollectorCollectEvent(collector, null, produce));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ProduceCollectorCollectEvent(collector, b, null));
    }

    @Test
    @DisplayName("A bucket with a cow in range fires the event and matches the milk produce")
    void testMatchFiresEventAndConsumesBucket() {
        BlockMenu menu = placeCollector(10, 10, new ItemStack(Material.BUCKET));
        Block b = menu.getBlock();
        world.spawn(new Location(world, 11, 1, 10), Cow.class);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(ProduceCollectorCollectEvent event) {
                seen[0] = true;
                Assertions.assertEquals(collector, event.getCollector());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(Material.BUCKET, event.getProduce().getInput()[0].getType());
                Assertions.assertEquals(Material.MILK_BUCKET, event.getProduce().getOutput()[0].getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            MachineRecipe recipe = findRecipe(menu);

            Assertions.assertTrue(seen[0], "ProduceCollectorCollectEvent was not fired");
            Assertions.assertNotNull(recipe, "A matching produce must have been returned");
            Assertions.assertEquals(Material.MILK_BUCKET, recipe.getOutput()[0].getType());
            assertInputConsumed(menu);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ProduceCollectorCollectEvent keeps the bucket and yields no recipe")
    void testCancelKeepsBucketAndYieldsNothing() {
        BlockMenu menu = placeCollector(20, 20, new ItemStack(Material.BUCKET));
        world.spawn(new Location(world, 21, 1, 20), Cow.class);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCollect(ProduceCollectorCollectEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            MachineRecipe recipe = findRecipe(menu);

            Assertions.assertNull(recipe, "A vetoed produce must leave the collector without a recipe");
            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "A vetoed collection must keep the bucket");
            Assertions.assertEquals(Material.BUCKET, slot.getType());
            Assertions.assertEquals(1, slot.getAmount());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Collecting without listeners still consumes the bucket, preserving the old behavior")
    void testCollectWithoutListenersStillConsumes() {
        BlockMenu menu = placeCollector(30, 30, new ItemStack(Material.BUCKET));
        world.spawn(new Location(world, 31, 1, 30), Cow.class);

        MachineRecipe recipe = findRecipe(menu);

        Assertions.assertNotNull(recipe, "A matching produce must have been returned");
        assertInputConsumed(menu);
    }

    @Test
    @DisplayName("A bucket without an animal in range fires no event")
    void testNoAnimalNearbyFiresNothing() {
        BlockMenu menu = placeCollector(40, 40, new ItemStack(Material.BUCKET));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(ProduceCollectorCollectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            MachineRecipe recipe = findRecipe(menu);

            Assertions.assertNull(recipe, "No recipe must match without an animal in range");
            Assertions.assertFalse(seen[0], "No event must be fired without an animal in range");
            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "The bucket must have stayed put");
            Assertions.assertEquals(Material.BUCKET, slot.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A foreign input item fires no event")
    void testForeignInputFiresNothing() {
        BlockMenu menu = placeCollector(50, 50, new ItemStack(Material.DIRT));
        world.spawn(new Location(world, 51, 1, 50), Cow.class);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(ProduceCollectorCollectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            MachineRecipe recipe = findRecipe(menu);

            Assertions.assertNull(recipe, "No recipe must match a foreign input item");
            Assertions.assertFalse(seen[0], "No event must be fired for a foreign input item");
            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "The foreign item must have stayed put");
            Assertions.assertEquals(Material.DIRT, slot.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

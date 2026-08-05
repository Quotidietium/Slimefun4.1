package io.github.thebusybiscuit.slimefun4.implementation.items.electric.generators;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.GeneratorProduceByproductEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the generator byproduct API expansion:
 * {@link GeneratorProduceByproductEvent}, exercised by driving an {@link AGenerator}
 * straight into its finished-bucket path with an already-finished {@link FuelOperation}.
 *
 * @author Zurker
 */
class TestGeneratorProduceByproductEvent {

    private static final int OUTPUT_SLOT = 24;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AGenerator generator;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "generator_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_GENERATOR", Material.FURNACE, "&fTest Generator");
        Slimefun.getItemCfg().setValue("_TEST_GENERATOR.enabled", true);
        generator = new AGenerator(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ItemStack getProgressBar() {
                return new ItemStack(Material.FLINT_AND_STEEL);
            }

            @Override
            public void registerDefaultFuelTypes() {
                // No default fuels - the test drives a finished FuelOperation directly
            }
        };
        generator.setCapacity(512);
        generator.setEnergyProduction(10);
        generator.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private BlockMenu placeGenerator(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.FURNACE);
        BlockStorage.addBlockInfo(b, "id", generator.getId(), true);
        return BlockStorage.getInventory(b);
    }

    private void runFinishedBucketOperation(Block b) {
        Location l = b.getLocation();
        FuelOperation operation = new FuelOperation(new ItemStack(Material.LAVA_BUCKET), null, 1);
        operation.addProgress(1);

        generator.getMachineProcessor().startOperation(l, operation);

        Config data = BlockStorage.getLocationInfo(l);
        generator.getGeneratedOutput(l, data);
    }

    @Test
    @DisplayName("GeneratorProduceByproductEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location l = new Location(world, 1, 1, 1);
        ItemStack result = new ItemStack(Material.BUCKET);

        GeneratorProduceByproductEvent event = new GeneratorProduceByproductEvent(generator, l, result);

        Assertions.assertEquals(generator, event.getGenerator());
        Assertions.assertEquals(l, event.getLocation());
        Assertions.assertEquals(result, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.IRON_INGOT);
        event.setResult(swapped);
        Assertions.assertEquals(swapped, event.getResult());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GeneratorProduceByproductEvent(null, l, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GeneratorProduceByproductEvent(generator, null, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GeneratorProduceByproductEvent(generator, l, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("A finished bucketed fuel fires the event and pushes the empty bucket to the output")
    void testByproductFiresAndProduces() {
        Block b = world.getBlockAt(10, 1, 10);
        BlockMenu menu = placeGenerator(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProduce(GeneratorProduceByproductEvent event) {
                seen[0] = true;
                Assertions.assertEquals(generator, event.getGenerator());
                Assertions.assertEquals(Material.BUCKET, event.getResult().getType(), "The default byproduct is an empty bucket");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runFinishedBucketOperation(b);

            Assertions.assertTrue(seen[0], "GeneratorProduceByproductEvent was not fired");
            Assertions.assertEquals(Material.BUCKET, menu.getItemInSlot(OUTPUT_SLOT).getType(), "The empty bucket must have been pushed to the output slot");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling GeneratorProduceByproductEvent produces no byproduct")
    void testEventCancellationProducesNothing() {
        Block b = world.getBlockAt(20, 1, 20);
        BlockMenu menu = placeGenerator(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onProduce(GeneratorProduceByproductEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            runFinishedBucketOperation(b);

            ItemStack output = menu.getItemInSlot(OUTPUT_SLOT);
            Assertions.assertTrue(output == null || output.getType() == Material.AIR, "A cancelled byproduct must produce nothing");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the result via setResult produces the custom byproduct instead")
    void testResultSwapProducesCustomItem() {
        Block b = world.getBlockAt(30, 1, 30);
        BlockMenu menu = placeGenerator(30, 30);
        ItemStack custom = new ItemStack(Material.IRON_NUGGET);

        Listener swapping = new Listener() {
            @EventHandler
            public void onProduce(GeneratorProduceByproductEvent event) {
                event.setResult(custom);
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            runFinishedBucketOperation(b);

            Assertions.assertTrue(custom.isSimilar(menu.getItemInSlot(OUTPUT_SLOT)), "The custom byproduct must have been produced");
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Producing without listeners still pushes the bucket, preserving the old behavior")
    void testProduceWithoutListenersStillProduces() {
        Block b = world.getBlockAt(40, 1, 40);
        BlockMenu menu = placeGenerator(40, 40);

        runFinishedBucketOperation(b);

        Assertions.assertEquals(Material.BUCKET, menu.getItemInSlot(OUTPUT_SLOT).getType(), "The empty bucket must have been pushed to the output slot");
    }
}

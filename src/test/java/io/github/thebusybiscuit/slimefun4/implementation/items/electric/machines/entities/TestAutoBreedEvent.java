package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import java.util.UUID;

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
import org.opentest4j.TestAbortedException;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.CowMock;

import io.github.thebusybiscuit.slimefun4.api.events.AutoBreedEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the auto breeder API expansion: {@link AutoBreedEvent},
 * exercised through the real {@link AutoBreeder#tick(Block)} breeding path.
 *
 * @author Zurker
 */
class TestAutoBreedEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AutoBreeder breeder;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // A BlockTicker item stays DISABLED while tickers are off and non-configurable items
        // stay DISABLED unless Items.yml says otherwise, so enable both before registering.
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "auto_breeder_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_AUTO_BREEDER", Material.DISPENSER, "&eTest Auto Breeder");
        Slimefun.getCfg().setValue("URID.enable-tickers", true);
        Slimefun.getItemCfg().setValue("TEST_AUTO_BREEDER.enabled", true);
        breeder = new AutoBreeder(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        breeder.register(plugin);
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
     * Builds a charged auto breeder with organic food in its first input slot at the
     * given position.
     */
    private Block setupBreeder(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", breeder.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "128", false);

        BlockMenu menu = BlockStorage.getInventory(b);
        menu.replaceExistingItem(10, SlimefunItems.ORGANIC_FOOD.item().clone());
        return b;
    }

    /**
     * Spawns an adult cow next to the breeder and registers it with the server.
     */
    private CowMock spawnCow(int x, int z) {
        CowMock cow = new CowMock(server, UUID.randomUUID());
        cow.setAdult();
        cow.setLocation(new Location(world, x + 1, 1, z));
        server.registerEntity(cow);
        return cow;
    }

    /**
     * Ticks the breeder. MockBukkit does not implement the 8-argument
     * {@code World#spawnParticle} used for the heart effect, so the success path dies
     * with a {@link TestAbortedException} after the breeding happened - it is caught
     * and ignored here.
     */
    private void tick(Block b) {
        try {
            breeder.tick(b);
        } catch (TestAbortedException ignored) {
            // See the javadoc above
        }
    }

    private void removeCow(CowMock cow) {
        if (cow.isValid()) {
            cow.remove();
        }
        server.unregisterEntity(cow);
    }

    @Test
    @DisplayName("AutoBreedEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        CowMock cow = new CowMock(server, UUID.randomUUID());
        ItemStack food = SlimefunItems.ORGANIC_FOOD.item().clone();

        AutoBreedEvent event = new AutoBreedEvent(breeder, b, cow, food);

        Assertions.assertEquals(breeder, event.getBreeder());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(cow, event.getAnimal());
        Assertions.assertEquals(food, event.getFood());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBreedEvent(null, b, cow, food));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBreedEvent(breeder, null, cow, food));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBreedEvent(breeder, b, null, food));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBreedEvent(breeder, b, cow, null));
    }

    @Test
    @DisplayName("Breeding a cow fires the event and sets the animal into love mode")
    void testBreedFiresAndSetsLoveMode() {
        Block b = setupBreeder(10, 10);
        CowMock cow = spawnCow(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBreed(AutoBreedEvent event) {
                seen[0] = true;
                Assertions.assertEquals(breeder, event.getBreeder());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(cow, event.getAnimal());
                Assertions.assertTrue(SlimefunItems.ORGANIC_FOOD.item().isSimilar(event.getFood()), "The food must be organic food");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "AutoBreedEvent was not fired");
            Assertions.assertTrue(cow.isLoveMode(), "The cow must have entered love mode");
            Assertions.assertEquals(0, BlockStorage.getInventory(b).getItemInSlot(10).getAmount(), "The organic food must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("Cancelling AutoBreedEvent skips the whole breeding operation")
    void testEventCancellationSkipsBreeding() {
        Block b = setupBreeder(20, 20);
        CowMock cow = spawnCow(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBreed(AutoBreedEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            Assertions.assertFalse(cow.isLoveMode(), "A cancelled breed must leave the animal alone");
            Assertions.assertEquals(1, BlockStorage.getInventory(b).getItemInSlot(10).getAmount(), "A cancelled breed must keep the food");
        } finally {
            HandlerList.unregisterAll(cancelling);
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("Breeding without listeners still works, preserving the old behavior")
    void testBreedWithoutListenersStillBreeds() {
        Block b = setupBreeder(30, 30);
        CowMock cow = spawnCow(30, 30);

        try {
            tick(b);

            Assertions.assertTrue(cow.isLoveMode(), "The cow must have entered love mode");
        } finally {
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("A breeder without food fires no event")
    void testNoFoodFiresNothing() {
        Block b = world.getBlockAt(40, 1, 40);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", breeder.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "128", false);
        BlockStorage.getInventory(b);
        CowMock cow = spawnCow(40, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBreed(AutoBreedEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without food");
            Assertions.assertFalse(cow.isLoveMode(), "The cow must not enter love mode without food");
        } finally {
            HandlerList.unregisterAll(watcher);
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("A breeder without animals nearby fires no event")
    void testNoAnimalsFiresNothing() {
        Block b = setupBreeder(50, 50);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBreed(AutoBreedEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without animals");
            Assertions.assertEquals(1, BlockStorage.getInventory(b).getItemInSlot(10).getAmount(), "The food must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

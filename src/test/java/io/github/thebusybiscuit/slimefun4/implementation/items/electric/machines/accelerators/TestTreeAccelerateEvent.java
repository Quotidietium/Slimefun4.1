package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.TreeAccelerateEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the tree growth accelerator API expansion:
 * {@link TreeAccelerateEvent}, exercised through the real {@link TreeGrowthAccelerator#tick(Block)}
 * boost path. The unit test environment reports a Minecraft version of 1.17 or higher, so
 * the bonemeal simulation branch is what runs here.
 * <p>
 * The accelerator block is a Mockito hybrid over a real {@link BlockStorage}-registered
 * location with a real {@link BlockMenu}; its {@code getRelative()} scan finds a mocked
 * sapling at offset (1, 0, 0).
 *
 * @author Zurker
 */
class TestTreeAccelerateEvent {

    private static final int ENERGY_CONSUMPTION = 24;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static TreeGrowthAccelerator accelerator;

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
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "tree_accelerator_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_TREE_ACCELERATOR", Material.DISPENSER, "&2Test Tree Accelerator");
        Slimefun.getCfg().setValue("URID.enable-tickers", true);
        Slimefun.getItemCfg().setValue("TEST_TREE_ACCELERATOR.enabled", true);
        accelerator = new TreeGrowthAccelerator(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        accelerator.register(plugin);
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
     * Registers a charged accelerator with one fertilizer at the given position and
     * returns a mocked accelerator block whose {@code getRelative()} scan finds the
     * given sapling at offset (1, 0, 0). The inventory behind it is entirely real.
     */
    private Block setupAccelerator(int x, int z, Block sapling) {
        Block real = world.getBlockAt(x, 1, z);
        real.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(real, "id", accelerator.getId(), true);
        BlockStorage.addBlockInfo(real.getLocation(), "energy-charge", "1024", false);

        BlockMenu menu = BlockStorage.getInventory(real);
        menu.replaceExistingItem(10, SlimefunItems.FERTILIZER.item().clone());

        Location loc = real.getLocation();
        Block machine = Mockito.mock(Block.class);
        Mockito.when(machine.getType()).thenReturn(Material.DISPENSER);
        Mockito.when(machine.getLocation()).thenReturn(loc);
        Mockito.when(machine.getWorld()).thenReturn(world);
        Mockito.when(machine.getRelative(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);

            if (dx == 1 && dz == 0) {
                return sapling;
            }

            return world.getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
        });
        return machine;
    }

    /**
     * Creates a mocked oak sapling at offset (1, 0, 0) from the accelerator position.
     */
    private Block mockSapling(int x, int z) {
        Block sapling = Mockito.mock(Block.class);
        Mockito.when(sapling.getType()).thenReturn(Material.OAK_SAPLING);
        Mockito.when(sapling.getWorld()).thenReturn(world);
        Mockito.when(sapling.getLocation()).thenReturn(new Location(world, x + 1, 1, z));
        return sapling;
    }

    /**
     * Ticks the accelerator once. The boost path ends in a particle effect that
     * MockBukkit does not fully support, so a RuntimeException from that tail is
     * ignored here - the event was fired and the resources consumed beforehand.
     */
    private void tick(Block machine) {
        try {
            accelerator.tick(machine);
        } catch (RuntimeException ignored) {
            // See the javadoc above
        }
    }

    @Test
    @DisplayName("TreeAccelerateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        Block sapling = world.getBlockAt(2, 1, 1);
        ItemStack fertilizer = SlimefunItems.FERTILIZER.item().clone();

        TreeAccelerateEvent event = new TreeAccelerateEvent(accelerator, b, sapling, fertilizer);

        Assertions.assertEquals(accelerator, event.getAccelerator());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(sapling, event.getSapling());
        Assertions.assertEquals(fertilizer, event.getFertilizer());
        Assertions.assertEquals(1, event.getGrowthBoost(), "The growth boost must default to a single bonemeal application");
        Assertions.assertFalse(event.isCancelled());

        // The boost can be scaled or zeroed
        event.setGrowthBoost(3);
        Assertions.assertEquals(3, event.getGrowthBoost());
        event.setGrowthBoost(0);
        Assertions.assertEquals(0, event.getGrowthBoost());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setGrowthBoost(-1));

        // The boost is capped: an unbounded value would stall the tick thread in the bonemeal loop
        event.setGrowthBoost(TreeAccelerateEvent.MAX_GROWTH_BOOST);
        Assertions.assertEquals(TreeAccelerateEvent.MAX_GROWTH_BOOST, event.getGrowthBoost());
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setGrowthBoost(TreeAccelerateEvent.MAX_GROWTH_BOOST + 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setGrowthBoost(Integer.MAX_VALUE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TreeAccelerateEvent(null, b, sapling, fertilizer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TreeAccelerateEvent(accelerator, null, sapling, fertilizer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TreeAccelerateEvent(accelerator, b, null, fertilizer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TreeAccelerateEvent(accelerator, b, sapling, null));
    }

    @Test
    @DisplayName("Boosting a sapling fires the event, consumes energy and fertilizer and applies bonemeal")
    void testAccelerateFiresAndBoosts() {
        Block sapling = mockSapling(10, 10);
        Block machine = setupAccelerator(10, 10, sapling);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(TreeAccelerateEvent event) {
                seen[0] = true;
                Assertions.assertEquals(accelerator, event.getAccelerator());
                Assertions.assertEquals(machine, event.getBlock());
                Assertions.assertEquals(sapling, event.getSapling());
                Assertions.assertTrue(SlimefunItems.FERTILIZER.item().isSimilar(event.getFertilizer()), "The fertilizer must be Slimefun fertilizer");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(machine);

            Assertions.assertTrue(seen[0], "TreeAccelerateEvent was not fired");
            Assertions.assertEquals(1024 - ENERGY_CONSUMPTION, accelerator.getCharge(machine.getLocation()), "The energy must have been consumed");
            Assertions.assertEquals(0, menu.getItemInSlot(10).getAmount(), "The fertilizer must have been consumed");
            Mockito.verify(sapling).applyBoneMeal(BlockFace.UP);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling TreeAccelerateEvent skips the sapling and keeps the resources")
    void testEventCancellationSkipsSapling() {
        Block sapling = mockSapling(20, 20);
        Block machine = setupAccelerator(20, 20, sapling);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onAccelerate(TreeAccelerateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(machine);

            Assertions.assertEquals(1024, accelerator.getCharge(machine.getLocation()), "A cancelled acceleration must keep the energy");
            Assertions.assertEquals(1, menu.getItemInSlot(10).getAmount(), "A cancelled acceleration must keep the fertilizer");
            Mockito.verify(sapling, Mockito.never()).applyBoneMeal(Mockito.any());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Acceleration without listeners still boosts, preserving the old behavior")
    void testAccelerateWithoutListenersStillBoosts() {
        Block sapling = mockSapling(30, 30);
        Block machine = setupAccelerator(30, 30, sapling);

        tick(machine);

        Assertions.assertEquals(1024 - ENERGY_CONSUMPTION, accelerator.getCharge(machine.getLocation()), "The energy must have been consumed");
        Mockito.verify(sapling).applyBoneMeal(BlockFace.UP);
    }

    @Test
    @DisplayName("Scaling the growth boost applies bonemeal multiple times for one consumption")
    void testScaledGrowthBoost() {
        Block sapling = mockSapling(70, 70);
        Block machine = setupAccelerator(70, 70, sapling);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        Listener scaling = new Listener() {
            @EventHandler
            public void onAccelerate(TreeAccelerateEvent event) {
                Assertions.assertEquals(1, event.getGrowthBoost(), "The boost must default to one application");
                event.setGrowthBoost(3);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            tick(machine);

            Mockito.verify(sapling, Mockito.times(3)).applyBoneMeal(BlockFace.UP);
            Assertions.assertEquals(1024 - ENERGY_CONSUMPTION, accelerator.getCharge(machine.getLocation()), "The energy must have been consumed once");
            Assertions.assertEquals(0, menu.getItemInSlot(10).getAmount(), "The fertilizer must have been consumed once");
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("A zero growth boost still consumes the resources but applies no bonemeal")
    void testZeroGrowthBoost() {
        Block sapling = mockSapling(80, 80);
        Block machine = setupAccelerator(80, 80, sapling);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        Listener zeroing = new Listener() {
            @EventHandler
            public void onAccelerate(TreeAccelerateEvent event) {
                event.setGrowthBoost(0);
            }
        };
        server.getPluginManager().registerEvents(zeroing, plugin);

        try {
            tick(machine);

            Mockito.verify(sapling, Mockito.never()).applyBoneMeal(Mockito.any());
            Assertions.assertEquals(1024 - ENERGY_CONSUMPTION, accelerator.getCharge(machine.getLocation()), "The energy must still have been consumed");
            Assertions.assertEquals(0, menu.getItemInSlot(10).getAmount(), "The fertilizer must still have been consumed");
        } finally {
            HandlerList.unregisterAll(zeroing);
        }
    }

    @Test
    @DisplayName("An accelerator without fertilizer fires no event")
    void testNoFertilizerFiresNothing() {
        Block sapling = mockSapling(40, 40);
        Block machine = setupAccelerator(40, 40, sapling);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());
        menu.replaceExistingItem(10, null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(TreeAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(machine);

            Assertions.assertFalse(seen[0], "No event must be fired without fertilizer");
            Mockito.verify(sapling, Mockito.never()).applyBoneMeal(Mockito.any());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An accelerator without energy fires no event")
    void testNoEnergyFiresNothing() {
        Block sapling = mockSapling(50, 50);
        Block machine = setupAccelerator(50, 50, sapling);
        BlockStorage.addBlockInfo(machine.getLocation(), "energy-charge", "0", true);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(TreeAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(machine);

            Assertions.assertFalse(seen[0], "No event must be fired without energy");
            Assertions.assertEquals(1, menu.getItemInSlot(10).getAmount(), "The fertilizer must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An accelerator without saplings nearby fires no event")
    void testNoSaplingsFiresNothing() {
        Block real = world.getBlockAt(60, 1, 60);
        real.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(real, "id", accelerator.getId(), true);
        BlockStorage.addBlockInfo(real.getLocation(), "energy-charge", "1024", false);
        BlockStorage.getInventory(real).replaceExistingItem(10, SlimefunItems.FERTILIZER.item().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(TreeAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(real);

            Assertions.assertFalse(seen[0], "No event must be fired without saplings");
            Assertions.assertEquals(1, BlockStorage.getInventory(real).getItemInSlot(10).getAmount(), "The fertilizer must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

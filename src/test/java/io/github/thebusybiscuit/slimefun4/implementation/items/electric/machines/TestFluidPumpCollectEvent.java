package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
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

import io.github.thebusybiscuit.slimefun4.api.events.FluidPumpCollectEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the fluid pump API expansion: {@link FluidPumpCollectEvent},
 * exercised through the real {@link FluidPump#tick(Block)} pumping path.
 * <p>
 * MockBukkit has no {@link Levelled} block data for water, so both the pump and the
 * fluid are Mockito blocks placed over a BlockStorage-registered location; the
 * {@link BlockMenu} behind them stays real.
 *
 * @author Zurker
 */
class TestFluidPumpCollectEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static FluidPump pump;

    private Block fluidBlock;

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
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "fluid_pump_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_FLUID_PUMP", Material.DISPENSER, "&bTest Fluid Pump");
        Slimefun.getCfg().setValue("URID.enable-tickers", true);
        Slimefun.getItemCfg().setValue("TEST_FLUID_PUMP.enabled", true);
        pump = new FluidPump(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        pump.register(plugin);
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
     * Builds a charged fluid pump over a water source and returns the pump block;
     * the water block is kept in {@link #fluidBlock} for verifications.
     */
    private Block setupPump(int x, int z) {
        Location loc = new Location(world, x, 1, z);
        BlockStorage.addBlockInfo(loc, "id", pump.getId(), true);
        BlockStorage.addBlockInfo(loc, "energy-charge", "128", false);

        Levelled waterData = Mockito.mock(Levelled.class);
        Mockito.when(waterData.getLevel()).thenReturn(0);

        fluidBlock = Mockito.mock(Block.class);
        Mockito.when(fluidBlock.getType()).thenReturn(Material.WATER);
        Mockito.when(fluidBlock.isLiquid()).thenReturn(true);
        Mockito.when(fluidBlock.getBlockData()).thenReturn(waterData);

        Block b = Mockito.mock(Block.class);
        Mockito.when(b.getLocation()).thenReturn(loc);
        Mockito.when(b.getWorld()).thenReturn(world);
        Mockito.when(b.getRelative(BlockFace.DOWN)).thenReturn(fluidBlock);
        return b;
    }

    private BlockMenu menuOf(Block b) {
        return BlockStorage.getInventory(b);
    }

    @Test
    @DisplayName("FluidPumpCollectEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        ItemStack bucket = new ItemStack(Material.WATER_BUCKET);

        FluidPumpCollectEvent event = new FluidPumpCollectEvent(pump, b, b, bucket);

        Assertions.assertEquals(pump, event.getPump());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(b, event.getFluid());
        Assertions.assertEquals(bucket, event.getFilledContainer());
        Assertions.assertFalse(event.isCancelled());

        // The produced container can be replaced
        ItemStack replacement = new ItemStack(Material.LAVA_BUCKET);
        event.setFilledContainer(replacement);
        Assertions.assertEquals(replacement, event.getFilledContainer());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setFilledContainer(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FluidPumpCollectEvent(null, b, b, bucket));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FluidPumpCollectEvent(pump, null, b, bucket));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FluidPumpCollectEvent(pump, b, null, bucket));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FluidPumpCollectEvent(pump, b, b, null));
    }

    @Test
    @DisplayName("Pumping water into a bucket fires the event and drains the source")
    void testBucketPumpFiresAndDrains() {
        Block b = setupPump(10, 10);
        menuOf(b).replaceExistingItem(19, new ItemStack(Material.BUCKET));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(FluidPumpCollectEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pump, event.getPump());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(fluidBlock, event.getFluid());
                Assertions.assertEquals(Material.WATER_BUCKET, event.getFilledContainer().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            pump.tick(b);

            Assertions.assertTrue(seen[0], "FluidPumpCollectEvent was not fired");
            Mockito.verify(fluidBlock).setType(Material.AIR);
            Assertions.assertEquals(0, menuOf(b).getItemInSlot(19).getAmount(), "The empty bucket must have been consumed");
            Assertions.assertEquals(Material.WATER_BUCKET, menuOf(b).getItemInSlot(24).getType(), "A water bucket must have been produced");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling FluidPumpCollectEvent skips the whole pumping operation")
    void testEventCancellationSkipsOperation() {
        Block b = setupPump(20, 20);
        menuOf(b).replaceExistingItem(19, new ItemStack(Material.BUCKET));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCollect(FluidPumpCollectEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            pump.tick(b);

            Mockito.verify(fluidBlock, Mockito.never()).setType(Mockito.any());
            Assertions.assertEquals(Material.BUCKET, menuOf(b).getItemInSlot(19).getType(), "A cancelled pump must keep the empty bucket");
            Assertions.assertNull(menuOf(b).getItemInSlot(24), "A cancelled pump must not produce anything");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Pumping without listeners still works, preserving the old behavior")
    void testPumpWithoutListenersStillPumps() {
        Block b = setupPump(30, 30);
        menuOf(b).replaceExistingItem(19, new ItemStack(Material.BUCKET));

        pump.tick(b);

        Mockito.verify(fluidBlock).setType(Material.AIR);
        Assertions.assertEquals(Material.WATER_BUCKET, menuOf(b).getItemInSlot(24).getType(), "A water bucket must have been produced");
    }

    @Test
    @DisplayName("Pumping water into a bottle fires the event with a water bottle")
    void testBottlePumpFires() {
        Block b = setupPump(40, 40);
        menuOf(b).replaceExistingItem(19, new ItemStack(Material.GLASS_BOTTLE));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(FluidPumpCollectEvent event) {
                seen[0] = true;
                Assertions.assertEquals(Material.POTION, event.getFilledContainer().getType(), "Water must fill a bottle into a water bottle");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            pump.tick(b);

            Assertions.assertTrue(seen[0], "FluidPumpCollectEvent was not fired");
            Assertions.assertEquals(0, menuOf(b).getItemInSlot(19).getAmount(), "The empty bottle must have been consumed");
            Assertions.assertEquals(Material.POTION, menuOf(b).getItemInSlot(24).getType(), "A water bottle must have been produced");
            // The bottle path only drains the source on a 30% roll, so the drain is not verified
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Replacing the filled container redirects the produced output")
    void testSetFilledContainerRedirectsOutput() {
        Block b = setupPump(70, 70);
        menuOf(b).replaceExistingItem(19, new ItemStack(Material.BUCKET));

        ItemStack replacement = new ItemStack(Material.LAVA_BUCKET, 1);
        Listener redirecting = new Listener() {
            @EventHandler
            public void onCollect(FluidPumpCollectEvent event) {
                Assertions.assertEquals(Material.WATER_BUCKET, event.getFilledContainer().getType(), "The default container must be the filled bucket");
                event.setFilledContainer(replacement);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            pump.tick(b);

            Mockito.verify(fluidBlock).setType(Material.AIR);
            Assertions.assertEquals(0, menuOf(b).getItemInSlot(19).getAmount(), "The empty bucket must have been consumed");
            Assertions.assertEquals(Material.LAVA_BUCKET, menuOf(b).getItemInSlot(24).getType(), "The replacement container must have been produced");
            Assertions.assertNotEquals(Material.WATER_BUCKET, menuOf(b).getItemInSlot(24).getType(), "The original water bucket must not have been produced");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("A pump without energy fires no event")
    void testNoEnergyFiresNothing() {
        Block b = setupPump(50, 50);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "0", true);
        menuOf(b).replaceExistingItem(19, new ItemStack(Material.BUCKET));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(FluidPumpCollectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            pump.tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without energy");
            Assertions.assertEquals(Material.BUCKET, menuOf(b).getItemInSlot(19).getType(), "The empty bucket must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A pump above solid ground fires no event")
    void testNoFluidFiresNothing() {
        Block b = setupPump(60, 60);
        Mockito.when(fluidBlock.getType()).thenReturn(Material.STONE);
        Mockito.when(fluidBlock.isLiquid()).thenReturn(false);
        menuOf(b).replaceExistingItem(19, new ItemStack(Material.BUCKET));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(FluidPumpCollectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            pump.tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired above solid ground");
            Assertions.assertEquals(Material.BUCKET, menuOf(b).getItemInSlot(19).getType(), "The empty bucket must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

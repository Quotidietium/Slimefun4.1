package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceRecipe;
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
import be.seeseemelk.mockbukkit.inventory.FurnaceInventoryMock;

import io.github.thebusybiscuit.slimefun4.api.events.EnhancedFurnaceBurnEvent;
import io.github.thebusybiscuit.slimefun4.api.events.EnhancedFurnaceSmeltEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the enhanced furnace API expansion:
 * {@link EnhancedFurnaceBurnEvent} and {@link EnhancedFurnaceSmeltEvent}, exercised
 * through the real {@link EnhancedFurnaceListener} dispatch paths.
 *
 * @author Zurker
 */
class TestEnhancedFurnaceEvents {

    private static final int EFFICIENCY = 2;
    private static final ItemStack SMELTING = new ItemStack(Material.IRON_ORE);
    private static final ItemStack SMELT_RESULT = new ItemStack(Material.IRON_INGOT);

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static EnhancedFurnace furnace;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new EnhancedFurnaceListener(plugin);

        // Fortune 0 makes the output amount a constant 1, efficiency 3 becomes 2 internally.
        // A BlockTicker item stays DISABLED while tickers are off and non-configurable items
        // stay DISABLED unless Items.yml says otherwise, so enable both before registering.
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "enhanced_furnace_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_ENHANCED_FURNACE", Material.FURNACE, "&7Test Enhanced Furnace");
        Slimefun.getCfg().setValue("URID.enable-tickers", true);
        Slimefun.getItemCfg().setValue("TEST_ENHANCED_FURNACE.enabled", true);
        furnace = new EnhancedFurnace(itemGroup, 1, EFFICIENCY + 1, 0, stack, new ItemStack[9]);
        furnace.register(plugin);

        // The recipe service snapshot is only refreshed on plugin start, do it manually
        server.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "enhanced_furnace_test_smelt"), SMELT_RESULT, SMELTING.getType(), 0.7F, 200));
        Slimefun.getMinecraftRecipeService().refresh();
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
     * Places an enhanced furnace on the world grid at the given coordinates.
     */
    private Block setupFurnace(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.FURNACE);
        BlockStorage.addBlockInfo(b, "id", furnace.getId(), true);
        return b;
    }

    /**
     * Creates a hybrid furnace {@link Block}: MockBukkit has no Furnace block state, so a
     * Mockito block with a stubbed {@link Furnace} state is placed over a
     * BlockStorage-registered location (MockBukkit's PaperLib takes the Spigot branch and
     * calls {@code block.getState()}).
     */
    private Block mockFurnaceBlock(int x, int z, FurnaceInventoryMock inventory) {
        Location loc = new Location(world, x, 1, z);
        BlockStorage.addBlockInfo(loc, "id", furnace.getId(), true);

        Furnace state = Mockito.mock(Furnace.class);
        Mockito.when(state.getInventory()).thenReturn(inventory);

        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getType()).thenReturn(Material.FURNACE);
        Mockito.when(block.getWorld()).thenReturn(world);
        Mockito.when(block.getLocation()).thenReturn(loc);
        Mockito.when(block.getState()).thenReturn(state);
        return block;
    }

    // ---------- EnhancedFurnaceBurnEvent ----------

    @Test
    @DisplayName("EnhancedFurnaceBurnEvent exposes its fields and validates constructor arguments")
    void testBurnEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(b, new ItemStack(Material.COAL), 1600);

        EnhancedFurnaceBurnEvent event = new EnhancedFurnaceBurnEvent(furnace, b, burnEvent);

        Assertions.assertEquals(furnace, event.getFurnace());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(burnEvent, event.getBurnEvent());
        Assertions.assertEquals(EFFICIENCY, event.getFuelEfficiency());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnhancedFurnaceBurnEvent(null, b, burnEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnhancedFurnaceBurnEvent(furnace, null, burnEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnhancedFurnaceBurnEvent(furnace, b, null));
    }

    @Test
    @DisplayName("Burning fuel in an enhanced furnace fires the event and multiplies the burn time")
    void testBurnFiresAndMultiplies() {
        Block b = setupFurnace(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(EnhancedFurnaceBurnEvent event) {
                seen[0] = true;
                Assertions.assertEquals(furnace, event.getFurnace());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(EFFICIENCY, event.getFuelEfficiency());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(b, new ItemStack(Material.COAL), 1600);
            server.getPluginManager().callEvent(burnEvent);

            Assertions.assertTrue(seen[0], "EnhancedFurnaceBurnEvent was not fired");
            Assertions.assertEquals(1600 * EFFICIENCY, burnEvent.getBurnTime(), "The burn time must have been multiplied by the fuel efficiency");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling EnhancedFurnaceBurnEvent keeps the vanilla burn time")
    void testBurnCancellationKeepsVanillaBurnTime() {
        Block b = setupFurnace(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBurn(EnhancedFurnaceBurnEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(b, new ItemStack(Material.COAL), 1600);
            server.getPluginManager().callEvent(burnEvent);

            Assertions.assertEquals(1600, burnEvent.getBurnTime(), "A cancelled burn must keep the vanilla burn time");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Burning fuel without listeners still multiplies, preserving the old behavior")
    void testBurnWithoutListenersStillMultiplies() {
        Block b = setupFurnace(30, 30);

        FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(b, new ItemStack(Material.COAL), 1600);
        server.getPluginManager().callEvent(burnEvent);

        Assertions.assertEquals(1600 * EFFICIENCY, burnEvent.getBurnTime());
    }

    @Test
    @DisplayName("An addon-raised fuel efficiency cannot overflow the burn time to a wrong value")
    void testBurnEfficiencyOverflowClamped() {
        Block b = setupFurnace(80, 80);

        Listener boosting = new Listener() {
            @EventHandler
            public void onBurn(EnhancedFurnaceBurnEvent event) {
                event.setFuelEfficiency(65536);
            }
        };
        server.getPluginManager().registerEvents(boosting, plugin);

        try {
            // 65536 * 65536 overflows int to 0 (and other factors wrap negative); the long
            // multiplication must clamp the product to Short.MAX_VALUE - 1 instead.
            FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(b, new ItemStack(Material.COAL), 65536);
            server.getPluginManager().callEvent(burnEvent);

            Assertions.assertEquals(Short.MAX_VALUE - 1, burnEvent.getBurnTime(), "An overflowed efficiency product must be clamped, not wrapped");
        } finally {
            HandlerList.unregisterAll(boosting);
        }
    }

    // ---------- EnhancedFurnaceSmeltEvent ----------

    @Test
    @DisplayName("EnhancedFurnaceSmeltEvent exposes its fields and validates constructor arguments")
    void testSmeltEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        FurnaceSmeltEvent smeltEvent = new FurnaceSmeltEvent(b, SMELTING, SMELT_RESULT);

        EnhancedFurnaceSmeltEvent event = new EnhancedFurnaceSmeltEvent(furnace, b, smeltEvent, 1);

        Assertions.assertEquals(furnace, event.getFurnace());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(smeltEvent, event.getSmeltEvent());
        Assertions.assertEquals(1, event.getAmount());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnhancedFurnaceSmeltEvent(null, b, smeltEvent, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnhancedFurnaceSmeltEvent(furnace, null, smeltEvent, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnhancedFurnaceSmeltEvent(furnace, b, null, 1));
    }

    @Test
    @DisplayName("Smelting in an enhanced furnace fires the event and applies the bonus output")
    void testSmeltFiresAndAppliesBonus() {
        FurnaceInventoryMock inv = new FurnaceInventoryMock(null);
        inv.setSmelting(SMELTING.clone());
        Block block = mockFurnaceBlock(40, 40, inv);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSmelt(EnhancedFurnaceSmeltEvent event) {
                seen[0] = true;
                Assertions.assertEquals(furnace, event.getFurnace());
                Assertions.assertEquals(block, event.getBlock());
                Assertions.assertEquals(1, event.getAmount(), "A zero fortune furnace must roll a constant amount of 1");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            FurnaceSmeltEvent smeltEvent = new FurnaceSmeltEvent(block, SMELTING.clone(), SMELT_RESULT.clone());
            server.getPluginManager().callEvent(smeltEvent);

            Assertions.assertTrue(seen[0], "EnhancedFurnaceSmeltEvent was not fired");
            Assertions.assertEquals(1, smeltEvent.getResult().getAmount(), "The bonus output must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling EnhancedFurnaceSmeltEvent keeps the vanilla smelting result")
    void testSmeltCancellationKeepsVanillaResult() {
        FurnaceInventoryMock inv = new FurnaceInventoryMock(null);
        inv.setSmelting(SMELTING.clone());
        Block block = mockFurnaceBlock(50, 50, inv);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSmelt(EnhancedFurnaceSmeltEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            ItemStack vanillaResult = SMELT_RESULT.clone();
            FurnaceSmeltEvent smeltEvent = new FurnaceSmeltEvent(block, SMELTING.clone(), vanillaResult);
            server.getPluginManager().callEvent(smeltEvent);

            Assertions.assertSame(vanillaResult, smeltEvent.getResult(), "A cancelled smelt must keep the vanilla result");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Smelting without listeners still applies the bonus, preserving the old behavior")
    void testSmeltWithoutListenersStillAppliesBonus() {
        FurnaceInventoryMock inv = new FurnaceInventoryMock(null);
        inv.setSmelting(SMELTING.clone());
        Block block = mockFurnaceBlock(60, 60, inv);

        FurnaceSmeltEvent smeltEvent = new FurnaceSmeltEvent(block, SMELTING.clone(), SMELT_RESULT.clone());
        server.getPluginManager().callEvent(smeltEvent);

        Assertions.assertEquals(1, smeltEvent.getResult().getAmount(), "The bonus output must have been applied");
    }

    @Test
    @DisplayName("An addon-raised smelt amount is re-capped to the result's max stack size")
    void testSmeltAmountOverriddenIsCapped() {
        FurnaceInventoryMock inv = new FurnaceInventoryMock(null);
        inv.setSmelting(SMELTING.clone());
        Block block = mockFurnaceBlock(90, 90, inv);

        Listener overriding = new Listener() {
            @EventHandler
            public void onSmelt(EnhancedFurnaceSmeltEvent event) {
                event.setAmount(100);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            FurnaceSmeltEvent smeltEvent = new FurnaceSmeltEvent(block, SMELTING.clone(), SMELT_RESULT.clone());
            server.getPluginManager().callEvent(smeltEvent);

            Assertions.assertTrue(smeltEvent.getResult().getAmount() <= SMELT_RESULT.getMaxStackSize(), "An addon-set amount must be re-capped, got: " + smeltEvent.getResult().getAmount());
            Assertions.assertEquals(SMELT_RESULT.getMaxStackSize(), smeltEvent.getResult().getAmount(), "With an empty result slot the capped amount must equal the max stack size");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("A vanilla furnace fires no events")
    void testVanillaFurnaceFiresNoEvents() {
        Block b = world.getBlockAt(70, 1, 70);
        b.setType(Material.FURNACE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(EnhancedFurnaceBurnEvent event) {
                seen[0] = true;
            }

            @EventHandler
            public void onSmelt(EnhancedFurnaceSmeltEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(b, new ItemStack(Material.COAL), 1600);
            server.getPluginManager().callEvent(burnEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla furnace");
            Assertions.assertEquals(1600, burnEvent.getBurnTime(), "A vanilla furnace must keep the vanilla burn time");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.MultiToolModeSwitchEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the multi tool API expansion: {@link MultiToolModeSwitchEvent},
 * exercised by driving the real {@link MultiTool}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent} while the player is sneaking.
 * <p>
 * The switch path applies the new mode by updating the item lore, so tests can assert the
 * outcome end-to-end: a cancelled event leaves the lore untouched, a redirected switch
 * lands on the mode chosen via {@link MultiToolModeSwitchEvent#setNextIndex(int)}.
 *
 * @author Zurker
 */
class TestMultiToolModeSwitchEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static MultiTool multiTool;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "multi_tool_test");

        // The modes resolve their target items lazily via SlimefunItem#getById - register dummies.
        registerDummy(itemGroup, "_TEST_MT_MODE_A", "&cMode A");
        registerDummy(itemGroup, "_TEST_MT_MODE_B", "&6Mode B");
        registerDummy(itemGroup, "_TEST_MT_MODE_C", "&bMode C");

        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MULTI_TOOL", Material.DIAMOND_SWORD, "&bTest Multi Tool");
        Slimefun.getItemCfg().setValue("_TEST_MULTI_TOOL.enabled", true);
        multiTool = new MultiTool(itemGroup, stack, RecipeType.NULL, new ItemStack[9], 100F, "_TEST_MT_MODE_A", "_TEST_MT_MODE_B", "_TEST_MT_MODE_C");
        multiTool.register(plugin);
    }

    private static void registerDummy(ItemGroup itemGroup, String id, String name) {
        SlimefunItemStack stack = new SlimefunItemStack(id, Material.STONE, name);
        Slimefun.getItemCfg().setValue(id + ".enabled", true);
        new SlimefunItem(itemGroup, stack, RecipeType.NULL, new ItemStack[9]).register(plugin);
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
     * Right-clicks with a fresh multi tool via the real item use handler.
     *
     * @return the {@link ItemStack} the handler mutated, for lore assertions
     */
    private ItemStack use(Player player, boolean sneaking) {
        ItemStack tool = multiTool.getItem().clone();
        Block block = world.getBlockAt(0, 1, 0);
        block.setType(Material.STONE);

        player.setSneaking(sneaking);
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, tool, block, BlockFace.UP);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        multiTool.callItemHandler(ItemUseHandler.class, handler -> handler.onRightClick(event));

        return tool;
    }

    @Test
    @DisplayName("A corrupted mode index in the item's NBT falls back to the first mode")
    void testCorruptedModeIndexFallsBack() {
        Player player = server.addPlayer();
        ItemStack tool = multiTool.getItem().clone();

        // Tampered NBT (e.g. via an NBT editor): the mode index is out of range
        org.bukkit.inventory.meta.ItemMeta meta = tool.getItemMeta();
        io.github.bakedlibs.dough.data.persistent.PersistentDataAPI.setInt(meta, new org.bukkit.NamespacedKey(plugin, "multitool_mode"), 99);
        tool.setItemMeta(meta);

        Block block = world.getBlockAt(0, 1, 0);
        block.setType(Material.STONE);
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, tool, block, BlockFace.UP);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        player.setSneaking(false);
        Assertions.assertDoesNotThrow(() -> multiTool.callItemHandler(ItemUseHandler.class, handler -> handler.onRightClick(event)), "A corrupted mode index must not crash the use handler");

        player.setSneaking(true);
        Assertions.assertDoesNotThrow(() -> multiTool.callItemHandler(ItemUseHandler.class, handler -> handler.onRightClick(event)), "A corrupted mode index must not crash the mode switch");
    }

    @Test
    @DisplayName("MultiToolModeSwitchEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack tool = new ItemStack(Material.DIAMOND_SWORD);

        MultiToolModeSwitchEvent event = new MultiToolModeSwitchEvent(player, multiTool, tool, 0, 1, 3);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(multiTool, event.getMultiTool());
        Assertions.assertEquals(tool, event.getItem());
        Assertions.assertEquals(0, event.getPreviousIndex());
        Assertions.assertEquals(1, event.getNextIndex());
        Assertions.assertEquals(3, event.getModeCount());
        Assertions.assertFalse(event.isCancelled());

        event.setNextIndex(2);
        Assertions.assertEquals(2, event.getNextIndex());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultiToolModeSwitchEvent(player, null, tool, 0, 1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultiToolModeSwitchEvent(player, multiTool, null, 0, 1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultiToolModeSwitchEvent(player, multiTool, tool, 0, 1, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultiToolModeSwitchEvent(player, multiTool, tool, 3, 1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultiToolModeSwitchEvent(player, multiTool, tool, 0, 3, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNextIndex(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNextIndex(3));
    }

    @Test
    @DisplayName("Sneak-right-click fires the event and switches to the next mode")
    void testSneakSwitchFiresEvent() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSwitch(MultiToolModeSwitchEvent event) {
                seen[0] = true;
                Assertions.assertEquals(multiTool, event.getMultiTool());
                Assertions.assertEquals(0, event.getPreviousIndex());
                Assertions.assertEquals(1, event.getNextIndex());
                Assertions.assertEquals(3, event.getModeCount());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack tool = use(player, true);

            Assertions.assertTrue(seen[0], "MultiToolModeSwitchEvent was not fired");
            Assertions.assertTrue(tool.getItemMeta().hasLore(), "The switch must have updated the item lore");
            Assertions.assertTrue(tool.getItemMeta().getLore().get(0).contains("Mode B"), "The tool must have switched to mode 1");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MultiToolModeSwitchEvent keeps the current mode")
    void testCancelKeepsMode() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSwitch(MultiToolModeSwitchEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            ItemStack tool = use(player, true);

            Assertions.assertFalse(tool.getItemMeta().hasLore(), "A cancelled switch must leave the item untouched");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Redirecting via setNextIndex lands on the chosen mode")
    void testRedirectViaSetNextIndex() {
        Player player = server.addPlayer();

        Listener redirecting = new Listener() {
            @EventHandler
            public void onSwitch(MultiToolModeSwitchEvent event) {
                event.setNextIndex(2);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            ItemStack tool = use(player, true);

            Assertions.assertTrue(tool.getItemMeta().hasLore(), "The redirected switch must have updated the item lore");
            String modeLine = tool.getItemMeta().getLore().get(0);
            Assertions.assertTrue(modeLine.contains("Mode C"), "The tool must have switched to mode 2");
            Assertions.assertFalse(modeLine.contains("Mode B"), "The tool must not have switched to mode 1");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Right-clicking without sneaking fires no event")
    void testNonSneakFiresNothing() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSwitch(MultiToolModeSwitchEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player, false);

            Assertions.assertFalse(seen[0], "No event must be fired when not sneaking");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Sneak-right-click without listeners still switches, preserving the old behavior")
    void testSwitchWithoutListenersStillSwitches() {
        Player player = server.addPlayer();

        ItemStack tool = use(player, true);

        Assertions.assertTrue(tool.getItemMeta().hasLore(), "The switch must have updated the item lore");
        Assertions.assertTrue(tool.getItemMeta().getLore().get(0).contains("Mode B"), "The tool must have switched to mode 1");
    }
}

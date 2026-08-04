package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemUseEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunToolUseEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the item use API expansion: {@link SlimefunItemUseEvent}
 * and {@link SlimefunToolUseEvent}, exercised through the real {@link SlimefunItemInteractListener}
 * and {@link BlockListener} dispatch paths.
 *
 * @author Zurker
 */
class TestItemUseEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SlimefunItem itemUseItem;
    private static SlimefunItem toolItem;

    private static final AtomicBoolean itemUseHandlerCalled = new AtomicBoolean(false);
    private static final AtomicBoolean toolUseHandlerCalled = new AtomicBoolean(false);

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register them manually
        new BlockListener(plugin);
        new SlimefunItemInteractListener(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "item_use_test");

        SlimefunItemStack useStack = new SlimefunItemStack("TEST_ITEM_USE_STICK", Material.STICK, "&7Test Use Stick");
        itemUseItem = new SimpleSlimefunItem<ItemUseHandler>(itemGroup, useStack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ItemUseHandler getItemHandler() {
                return e -> itemUseHandlerCalled.set(true);
            }
        };
        itemUseItem.register(plugin);

        SlimefunItemStack toolStack = new SlimefunItemStack("TEST_TOOL_USE_PICKAXE", Material.DIAMOND_PICKAXE, "&7Test Tool Pickaxe");
        toolItem = new SimpleSlimefunItem<ToolUseHandler>(itemGroup, toolStack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ToolUseHandler getItemHandler() {
                return (e, tool, fortune, drops) -> toolUseHandlerCalled.set(true);
            }
        };
        toolItem.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
        itemUseHandlerCalled.set(false);
        toolUseHandlerCalled.set(false);
    }

    // ---------- SlimefunItemUseEvent ----------

    @Test
    @DisplayName("SlimefunItemUseEvent exposes its fields and validates constructor arguments")
    void testItemUseEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        ItemStack held = itemUseItem.getItem().clone();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, held, null, BlockFace.UP, EquipmentSlot.HAND);
        PlayerRightClickEvent rightClickEvent = new PlayerRightClickEvent(interactEvent);

        SlimefunItemUseEvent event = new SlimefunItemUseEvent(player, itemUseItem, held, rightClickEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(itemUseItem, event.getSlimefunItem());
        Assertions.assertEquals(held, event.getItem());
        Assertions.assertEquals(rightClickEvent, event.getRightClickEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemUseEvent(player, null, held, rightClickEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemUseEvent(player, itemUseItem, null, rightClickEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemUseEvent(player, itemUseItem, held, null));
    }

    @Test
    @DisplayName("Right-clicking with a Slimefun item fires SlimefunItemUseEvent and runs the ItemUseHandler")
    void testItemUseFiresAndRunsHandler() {
        PlayerMock player = server.addPlayer();
        ItemStack held = itemUseItem.getItem().clone();
        player.getInventory().setItemInMainHand(held);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onItemUse(SlimefunItemUseEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(itemUseItem, event.getSlimefunItem());
                Assertions.assertEquals(held, event.getItem());
                Assertions.assertNotNull(event.getRightClickEvent());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, held, null, BlockFace.UP, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertTrue(seen[0], "SlimefunItemUseEvent was not fired");
            Assertions.assertTrue(itemUseHandlerCalled.get(), "The ItemUseHandler was not called");
            Assertions.assertNotSame(Result.DENY, interactEvent.useItemInHand(), "The item use must not be denied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunItemUseEvent skips the ItemUseHandler and denies the item use")
    void testItemUseCancellationSkipsHandler() {
        PlayerMock player = server.addPlayer();
        ItemStack held = itemUseItem.getItem().clone();
        player.getInventory().setItemInMainHand(held);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onItemUse(SlimefunItemUseEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, held, null, BlockFace.UP, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertFalse(itemUseHandlerCalled.get(), "A cancelled item use must not call the ItemUseHandler");
            Assertions.assertSame(Result.DENY, interactEvent.useItemInHand(), "A cancelled item use must deny the underlying interaction");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("SlimefunItemUseEvent is not fired for vanilla items")
    void testItemUseNotFiredForVanillaItem() {
        PlayerMock player = server.addPlayer();
        ItemStack held = new ItemStack(Material.STICK);
        player.getInventory().setItemInMainHand(held);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onItemUse(SlimefunItemUseEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, held, null, BlockFace.UP, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla item");
            Assertions.assertNotSame(Result.DENY, interactEvent.useItemInHand(), "A vanilla item use must stay untouched");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    // ---------- SlimefunToolUseEvent ----------

    @Test
    @DisplayName("SlimefunToolUseEvent exposes its fields and validates constructor arguments")
    void testToolUseEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(new Location(world, 1, 64, 1));
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        ItemStack tool = toolItem.getItem().clone();
        List<ItemStack> drops = new ArrayList<>();

        SlimefunToolUseEvent event = new SlimefunToolUseEvent(player, toolItem, tool, breakEvent, 2, drops);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(toolItem, event.getSlimefunItem());
        Assertions.assertEquals(tool, event.getTool());
        Assertions.assertEquals(breakEvent, event.getBreakEvent());
        Assertions.assertEquals(2, event.getFortune());
        Assertions.assertSame(drops, event.getDrops(), "The drops list must be the live list the ToolUseHandler receives");
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunToolUseEvent(player, null, tool, breakEvent, 2, drops));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunToolUseEvent(player, toolItem, null, breakEvent, 2, drops));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunToolUseEvent(player, toolItem, tool, null, 2, drops));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunToolUseEvent(player, toolItem, tool, breakEvent, 2, null));
    }

    @Test
    @DisplayName("Breaking a block with a Slimefun tool fires SlimefunToolUseEvent and runs the ToolUseHandler")
    void testToolUseFiresAndRunsHandler() {
        PlayerMock player = server.addPlayer();
        ItemStack tool = toolItem.getItem().clone();
        player.getInventory().setItemInMainHand(tool);
        Block block = world.getBlockAt(10, 64, 10);
        block.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onToolUse(SlimefunToolUseEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(toolItem, event.getSlimefunItem());
                Assertions.assertEquals(tool, event.getTool());
                Assertions.assertEquals(block, event.getBreakEvent().getBlock());
                Assertions.assertEquals(1, event.getFortune(), "An unenchanted tool yields the baseline bonus of 1");
                Assertions.assertNotNull(event.getDrops());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            server.getPluginManager().callEvent(breakEvent);

            Assertions.assertTrue(seen[0], "SlimefunToolUseEvent was not fired");
            Assertions.assertTrue(toolUseHandlerCalled.get(), "The ToolUseHandler was not called");
            Assertions.assertFalse(breakEvent.isCancelled(), "The break must not be cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunToolUseEvent skips the ToolUseHandler without cancelling the break")
    void testToolUseCancellationSkipsHandler() {
        PlayerMock player = server.addPlayer();
        ItemStack tool = toolItem.getItem().clone();
        player.getInventory().setItemInMainHand(tool);
        Block block = world.getBlockAt(20, 64, 20);
        block.setType(Material.STONE);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onToolUse(SlimefunToolUseEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
            server.getPluginManager().callEvent(breakEvent);

            Assertions.assertFalse(toolUseHandlerCalled.get(), "A cancelled tool use must not call the ToolUseHandler");
            Assertions.assertFalse(breakEvent.isCancelled(), "A cancelled tool use must not cancel the underlying break");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
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
import be.seeseemelk.mockbukkit.inventory.InventoryMock;
import be.seeseemelk.mockbukkit.inventory.PlayerInventoryMock;
import be.seeseemelk.mockbukkit.inventory.SimpleInventoryViewMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemPickBlockEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the creative pick-block API expansion:
 * {@link SlimefunItemPickBlockEvent}, exercised through the real {@link MiddleClickListener}
 * give-and-swap paths.
 * <p>
 * {@code getTargetBlockExact} is unimplemented on MockBukkit players, so the player is a
 * Mockito hybrid: the targeted block is stubbed while the inventory is a real
 * {@link PlayerInventoryMock}.
 *
 * @author Zurker
 */
class TestSlimefunItemPickBlockEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SlimefunItem sfItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new MiddleClickListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        Slimefun.getItemCfg().setValue("TEST_PICK_BLOCK_ITEM.enabled", true);
        sfItem = TestUtilities.mockSlimefunItem(plugin, "TEST_PICK_BLOCK_ITEM", new ItemStack(Material.DISPENSER));
        sfItem.register(plugin);
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
     * Places the test item as a real block backed by {@link BlockStorage} and returns a
     * Mockito player that looks at it, with a real {@link PlayerInventoryMock}.
     */
    private Player setupPlayerLookingAtBlock(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", sfItem.getId(), true);

        Player player = Mockito.mock(Player.class);
        PlayerInventoryMock inventory = new PlayerInventoryMock(player);
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.getTargetBlockExact(5)).thenReturn(b);
        return player;
    }

    /**
     * Places a bare block without any {@link BlockStorage} info and returns a Mockito
     * player that looks at it. {@code BlockStorage.clearBlockInfo} only queues the
     * deletion for the next ticker tick, so a never-registered block is the only way
     * to get a genuine vanilla block in a unit test.
     */
    private Player setupPlayerLookingAtVanillaBlock(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);

        Player player = Mockito.mock(Player.class);
        PlayerInventoryMock inventory = new PlayerInventoryMock(player);
        Mockito.when(player.getInventory()).thenReturn(inventory);
        Mockito.when(player.getTargetBlockExact(5)).thenReturn(b);
        return player;
    }

    /**
     * {@code convertSlot} is unimplemented on the mock view; the events call it during
     * construction, so an identity override is provided here. Empty slots report AIR
     * because the listener dereferences the current item's type.
     */
    private SimpleInventoryViewMock openView(Player player) {
        InventoryMock top = new InventoryMock(null, 9, InventoryType.CHEST);
        return new SimpleInventoryViewMock(player, top, player.getInventory(), InventoryType.CRAFTING) {
            @Override
            public int convertSlot(int rawSlot) {
                return rawSlot;
            }

            @Override
            public Inventory getInventory(int rawSlot) {
                return rawSlot < getTopInventory().getSize() ? getTopInventory() : getBottomInventory();
            }

            @Override
            public ItemStack getItem(int rawSlot) {
                ItemStack item = rawSlot < getTopInventory().getSize() ? getTopInventory().getItem(rawSlot) : getBottomInventory().getItem(rawSlot - getTopInventory().getSize());
                return item == null ? new ItemStack(Material.AIR) : item;
            }
        };
    }

    /**
     * Fires a creative middle click whose cursor holds the targeted block's type, the
     * client signature of an outside-inventory pick-block the listener recognizes.
     */
    private InventoryCreativeEvent middleClick(Player player, Material cursorType) {
        SimpleInventoryViewMock view = openView(player);
        InventoryCreativeEvent event = new InventoryCreativeEvent(view, SlotType.QUICKBAR, 0, new ItemStack(cursorType));
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("SlimefunItemPickBlockEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = setupPlayerLookingAtBlock(1, 1);
        Block b = player.getTargetBlockExact(5);
        InventoryCreativeEvent inventoryEvent = new InventoryCreativeEvent(openView(player), SlotType.QUICKBAR, 0, new ItemStack(Material.DISPENSER));

        SlimefunItemPickBlockEvent event = new SlimefunItemPickBlockEvent(player, sfItem, b, inventoryEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(sfItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(inventoryEvent, event.getInventoryEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemPickBlockEvent(player, null, b, inventoryEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemPickBlockEvent(player, sfItem, null, inventoryEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemPickBlockEvent(player, sfItem, b, null));
    }

    @Test
    @DisplayName("Picking a Slimefun block not in the hotbar fires the event and gives the item")
    void testPickFiresAndGives() {
        Player player = setupPlayerLookingAtBlock(10, 10);
        Block b = player.getTargetBlockExact(5);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPick(SlimefunItemPickBlockEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(sfItem, event.getSlimefunItem());
                Assertions.assertEquals(b, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryCreativeEvent event = middleClick(player, Material.DISPENSER);

            Assertions.assertTrue(seen[0], "SlimefunItemPickBlockEvent was not fired");
            Assertions.assertTrue(sfItem.isItem(event.getCursor()), "The cursor must hold the picked SlimefunItem");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Picking a Slimefun block already in the hotbar swaps to its slot instead")
    void testPickFiresAndSwaps() {
        Player player = setupPlayerLookingAtBlock(20, 20);
        player.getInventory().setItem(3, sfItem.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPick(SlimefunItemPickBlockEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryCreativeEvent event = middleClick(player, Material.DISPENSER);

            Assertions.assertTrue(seen[0], "SlimefunItemPickBlockEvent was not fired");
            Assertions.assertEquals(3, player.getInventory().getHeldItemSlot(), "The held slot must have swapped to the existing item");
            Assertions.assertTrue(event.isCancelled(), "The creative event must be cancelled for the swap to stick");
            Assertions.assertFalse(sfItem.isItem(event.getCursor()), "No new item must have been given");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunItemPickBlockEvent neither gives nor swaps")
    void testEventCancellationSkipsPick() {
        Player player = setupPlayerLookingAtBlock(30, 30);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPick(SlimefunItemPickBlockEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            InventoryCreativeEvent event = middleClick(player, Material.DISPENSER);

            Assertions.assertFalse(sfItem.isItem(event.getCursor()), "A cancelled pick must not give the item");
            Assertions.assertEquals(Material.DISPENSER, event.getCursor().getType(), "A cancelled pick must leave the cursor alone");
            Assertions.assertEquals(0, player.getInventory().getHeldItemSlot(), "A cancelled pick must not switch slots");
            Assertions.assertFalse(event.isCancelled(), "A cancelled pick must leave the creative event alone");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Picking without listeners still gives, preserving the old behavior")
    void testPickWithoutListenersStillGives() {
        Player player = setupPlayerLookingAtBlock(40, 40);

        InventoryCreativeEvent event = middleClick(player, Material.DISPENSER);

        Assertions.assertTrue(sfItem.isItem(event.getCursor()), "The cursor must hold the picked SlimefunItem");
    }

    @Test
    @DisplayName("Picking a vanilla block fires no event")
    void testVanillaBlockFiresNothing() {
        Player player = setupPlayerLookingAtVanillaBlock(50, 50);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPick(SlimefunItemPickBlockEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryCreativeEvent event = middleClick(player, Material.DISPENSER);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla block");
            Assertions.assertEquals(Material.DISPENSER, event.getCursor().getType(), "The cursor must have been left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A click that is not an actual pick-block fires no event")
    void testNonPickClickFiresNothing() {
        Player player = setupPlayerLookingAtBlock(60, 60);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPick(SlimefunItemPickBlockEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // The cursor does not match the targeted block, so this cannot be an outside pick-block
            InventoryCreativeEvent event = middleClick(player, Material.STONE);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-pick creative click");
            Assertions.assertEquals(Material.STONE, event.getCursor().getType(), "The cursor must have been left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

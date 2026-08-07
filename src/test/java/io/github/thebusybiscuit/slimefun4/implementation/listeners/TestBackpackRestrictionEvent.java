package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.BackpackRestrictionEvent;
import io.github.thebusybiscuit.slimefun4.api.exceptions.TagMisconfigurationException;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for the backpack API expansion: {@link BackpackRestrictionEvent},
 * exercised by driving the real {@link BackpackListener} restriction paths while a
 * player is viewing a backpack.
 * <p>
 * The drop and drag paths use real Bukkit events (MockBukkit supports them). The
 * click paths go through a Mockito-stubbed {@link InventoryClickEvent} because
 * {@code InventoryViewMock.getItem} - which {@link InventoryClickEvent#getCurrentItem()}
 * delegates to - is not implemented under MockBukkit (the same limitation already
 * skips click tests in {@link TestBackpackListener}). The listener logic itself runs
 * unmodified; only the bukkit event is a controlled input, and cancellation is
 * verified with {@code verify(event).setCancelled(...)}.
 *
 * @author Zurker
 */
class TestBackpackRestrictionEvent {

    private static final int BACKPACK_SIZE = 9;

    private static ServerMock server;
    private static Slimefun plugin;
    private static BackpackListener listener;

    private SlimefunBackpack lastBackpack;

    @BeforeAll
    public static void load() throws TagMisconfigurationException, InterruptedException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        SlimefunTag.reloadAll();

        listener = new BackpackListener();
        listener.register(plugin);
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
     * Registers a backpack with the given id and material and puts the player into
     * the "viewing" state through the real open path. Returns the exact stack the
     * listener is tracking (SlimefunItemStack.item() clones per call, so a single
     * reference is captured and reused for the id, the open call and the caller).
     */
    private ItemStack openMockBackpack(Player player, String id, Material material) throws InterruptedException {
        SlimefunItemStack item = new SlimefunItemStack(id, material, "&4Mock Backpack", "", "&7Size: &e" + BACKPACK_SIZE, "&7ID: <ID>", "", "&7&eRight Click&7 to open");
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpack = profile.createBackpack(BACKPACK_SIZE);

        ItemStack tracked = item.item();
        listener.setBackpackId(player, tracked, 2, backpack.getId());

        ItemGroup itemGroup = new ItemGroup(new NamespacedKey(plugin, "restriction_test_" + id), CustomItemStack.create(material, "&4Test Backpacks"));
        lastBackpack = new SlimefunBackpack(BACKPACK_SIZE, itemGroup, item, RecipeType.NULL, new ItemStack[9]);
        lastBackpack.register(plugin);

        listener.openBackpack(player, tracked, lastBackpack);
        return tracked;
    }

    /**
     * Builds a Mockito-stubbed {@link InventoryClickEvent} for the click paths.
     */
    private InventoryClickEvent mockClick(Player player, ClickType click, InventoryType clickedType, ItemStack cursor, ItemStack current) {
        Inventory clickedInventory = mock(Inventory.class);
        when(clickedInventory.getType()).thenReturn(clickedType);

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getClick()).thenReturn(click);
        when(event.getClickedInventory()).thenReturn(clickedInventory);
        when(event.getCursor()).thenReturn(cursor);
        when(event.getCurrentItem()).thenReturn(current);
        when(event.getHotbarButton()).thenReturn(-1);
        // setCancelled is verified via Mockito; make it a no-op so the listener runs cleanly.
        doNothing().when(event).setCancelled(anyBoolean());

        return event;
    }

    @Test
    @DisplayName("BackpackRestrictionEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() throws InterruptedException {
        Player player = server.addPlayer();
        ItemStack viewed = openMockBackpack(player, "RESTRICTION_FIELDS_BACKPACK", Material.CHEST);
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);

        BackpackRestrictionEvent event = new BackpackRestrictionEvent(player, lastBackpack, viewed, shulker, BackpackRestrictionEvent.Reason.DISALLOWED_ITEM);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(lastBackpack, event.getBackpack());
        Assertions.assertEquals(viewed, event.getBackpackItem());
        Assertions.assertEquals(shulker, event.getItem());
        Assertions.assertEquals(BackpackRestrictionEvent.Reason.DISALLOWED_ITEM, event.getReason());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackRestrictionEvent(player, null, viewed, shulker, BackpackRestrictionEvent.Reason.DISALLOWED_ITEM));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackRestrictionEvent(player, lastBackpack, null, shulker, BackpackRestrictionEvent.Reason.DISALLOWED_ITEM));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackRestrictionEvent(player, lastBackpack, viewed, null, BackpackRestrictionEvent.Reason.DISALLOWED_ITEM));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackRestrictionEvent(player, lastBackpack, viewed, shulker, null));
    }

    @Test
    @DisplayName("Dropping a backpack while viewing one fires the event and blocks the drop")
    void testBackpackDropFiresAndBlocks() throws InterruptedException {
        Player player = server.addPlayer();
        ItemStack viewed = openMockBackpack(player, "RESTRICTION_DROP_BACKPACK", Material.ENDER_CHEST);

        Item drop = new ItemEntityMock(server, UUID.randomUUID(), viewed);
        PlayerDropItemEvent dropEvent = new PlayerDropItemEvent(player, drop);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(lastBackpack, event.getBackpack());
                Assertions.assertEquals(Material.ENDER_CHEST, event.getBackpackItem().getType());
                Assertions.assertEquals(BackpackRestrictionEvent.Reason.BACKPACK_DROP, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onItemDrop(dropEvent);

            Assertions.assertTrue(seen[0], "BackpackRestrictionEvent was not fired");
            Assertions.assertTrue(dropEvent.isCancelled(), "The backpack drop must have been blocked");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling BackpackRestrictionEvent allows the backpack drop")
    void testVetoAllowsBackpackDrop() throws InterruptedException {
        Player player = server.addPlayer();
        ItemStack viewed = openMockBackpack(player, "RESTRICTION_DROP_VETO_BACKPACK", Material.BARREL);

        Item drop = new ItemEntityMock(server, UUID.randomUUID(), viewed);
        PlayerDropItemEvent dropEvent = new PlayerDropItemEvent(player, drop);

        Listener vetoing = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(vetoing, plugin);

        try {
            listener.onItemDrop(dropEvent);

            Assertions.assertFalse(dropEvent.isCancelled(), "A vetoed restriction must let the drop proceed");
        } finally {
            HandlerList.unregisterAll(vetoing);
        }
    }

    @Test
    @DisplayName("Dragging a disallowed item across the backpack fires the event and blocks the drag")
    void testDragDisallowedItemFiresAndBlocks() throws InterruptedException {
        Player player = server.addPlayer();
        openMockBackpack(player, "RESTRICTION_DRAG_BACKPACK", Material.HOPPER);

        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        InventoryDragEvent drag = new InventoryDragEvent(player.getOpenInventory(), shulker.clone(), shulker, false, Map.of(0, shulker.clone()));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(Material.SHULKER_BOX, event.getItem().getType());
                Assertions.assertEquals(BackpackRestrictionEvent.Reason.DISALLOWED_ITEM, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onDrag(drag);

            Assertions.assertTrue(seen[0], "BackpackRestrictionEvent was not fired");
            Assertions.assertTrue(drag.isCancelled(), "The drag must have been blocked");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Off-hand swapping a disallowed item into the backpack fires the event and blocks it")
    void testOffhandSwapDisallowedItemFiresAndBlocks() throws InterruptedException {
        Player player = server.addPlayer();
        openMockBackpack(player, "RESTRICTION_OFFITEM_BACKPACK", Material.DROPPER);

        player.getInventory().setItemInOffHand(new ItemStack(Material.SHULKER_BOX));
        InventoryClickEvent click = new InventoryClickEvent(player.getOpenInventory(), SlotType.CONTAINER, 7, ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(Material.SHULKER_BOX, event.getItem().getType());
                Assertions.assertEquals(BackpackRestrictionEvent.Reason.DISALLOWED_ITEM, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onClick(click);

            Assertions.assertTrue(seen[0], "BackpackRestrictionEvent was not fired");
            Assertions.assertTrue(click.isCancelled(), "The off-hand swap must have been blocked");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Clicking a disallowed item into the backpack fires the event and blocks the click")
    void testClickDisallowedItemFiresAndBlocks() throws InterruptedException {
        Player player = server.addPlayer();
        openMockBackpack(player, "RESTRICTION_CLICK_BACKPACK", Material.TRAPPED_CHEST);

        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        InventoryClickEvent click = mockClick(player, ClickType.LEFT, InventoryType.CHEST, null, shulker);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(lastBackpack, event.getBackpack());
                Assertions.assertEquals(Material.SHULKER_BOX, event.getItem().getType());
                Assertions.assertEquals(BackpackRestrictionEvent.Reason.DISALLOWED_ITEM, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onClick(click);

            Assertions.assertTrue(seen[0], "BackpackRestrictionEvent was not fired");
            verify(click).setCancelled(true);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling BackpackRestrictionEvent allows the disallowed item into the backpack")
    void testVetoAllowsDisallowedItem() throws InterruptedException {
        Player player = server.addPlayer();
        openMockBackpack(player, "RESTRICTION_CLICK_VETO_BACKPACK", Material.FURNACE);

        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        InventoryClickEvent click = mockClick(player, ClickType.LEFT, InventoryType.CHEST, null, shulker);

        Listener vetoing = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(vetoing, plugin);

        try {
            listener.onClick(click);

            verify(click, never()).setCancelled(anyBoolean());
        } finally {
            HandlerList.unregisterAll(vetoing);
        }
    }

    @Test
    @DisplayName("Swapping the viewed backpack to the off hand fires the event and blocks the swap")
    void testBackpackOffhandSwapFiresAndBlocks() throws InterruptedException {
        Player player = server.addPlayer();
        ItemStack viewed = openMockBackpack(player, "RESTRICTION_OFFHAND_BACKPACK", Material.DISPENSER);

        InventoryClickEvent click = mockClick(player, ClickType.SWAP_OFFHAND, InventoryType.PLAYER, null, viewed);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(lastBackpack, event.getBackpack());
                Assertions.assertEquals(BackpackRestrictionEvent.Reason.BACKPACK_OFFHAND, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onClick(click);

            Assertions.assertTrue(seen[0], "BackpackRestrictionEvent was not fired");
            verify(click).setCancelled(true);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Clicking an allowed item fires no event and is not blocked")
    void testAllowedItemFiresNothing() throws InterruptedException {
        Player player = server.addPlayer();
        openMockBackpack(player, "RESTRICTION_ALLOWED_BACKPACK", Material.BOOKSHELF);

        InventoryClickEvent click = mockClick(player, ClickType.LEFT, InventoryType.CHEST, null, new ItemStack(Material.DIAMOND));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestriction(BackpackRestrictionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onClick(click);

            Assertions.assertFalse(seen[0], "No event must be fired for an allowed item");
            verify(click, never()).setCancelled(anyBoolean());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.inventory.InventoryMock;
import be.seeseemelk.mockbukkit.inventory.SimpleInventoryViewMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemVillagerTradeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the villager trading API expansion:
 * {@link SlimefunItemVillagerTradeEvent}, exercised through the real
 * {@link VillagerTradingListener} click and drag paths.
 *
 * @author Zurker
 */
class TestVillagerTradingEvents {

    private static ServerMock server;
    private static Slimefun plugin;

    private static SlimefunItem tradeItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new VillagerTradingListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first.
        // A disabled item is ignored by the trading restriction (isUnallowed check).
        Slimefun.getItemCfg().setValue("TEST_VILLAGER_TRADE_ITEM.enabled", true);
        tradeItem = TestUtilities.mockSlimefunItem(plugin, "TEST_VILLAGER_TRADE_ITEM", new org.bukkit.inventory.ItemStack(Material.EMERALD));
        tradeItem.register(plugin);
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
     * Opens a merchant view: MockBukkit ships no merchant inventory, so a plain
     * {@link InventoryMock} with {@link InventoryType#MERCHANT} serves as the top half.
     * {@code convertSlot} is unimplemented on the mock view; the events call it during
     * construction, so an identity override is provided here.
     */
    private SimpleInventoryViewMock openMerchantView(Player player) {
        InventoryMock top = new InventoryMock(null, 3, InventoryType.MERCHANT);
        return new SimpleInventoryViewMock(player, top, player.getInventory(), InventoryType.MERCHANT) {
            @Override
            public int convertSlot(int rawSlot) {
                return rawSlot;
            }

            @Override
            public org.bukkit.inventory.Inventory getInventory(int rawSlot) {
                return rawSlot < getTopInventory().getSize() ? getTopInventory() : getBottomInventory();
            }

            @Override
            public ItemStack getItem(int rawSlot) {
                if (rawSlot < getTopInventory().getSize()) {
                    return getTopInventory().getItem(rawSlot);
                }
                return getBottomInventory().getItem(rawSlot - getTopInventory().getSize());
            }
        };
    }

    private InventoryClickEvent clickOnMerchantSlot(SimpleInventoryViewMock view, ItemStack cursor) {
        view.setCursor(cursor);
        InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("SlimefunItemVillagerTradeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);
        InventoryClickEvent clickEvent = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        SlimefunItemVillagerTradeEvent event = new SlimefunItemVillagerTradeEvent(player, tradeItem, tradeItem.getItem(), clickEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(tradeItem, event.getSlimefunItem());
        Assertions.assertEquals(tradeItem.getItem(), event.getItemStack());
        Assertions.assertEquals(clickEvent, event.getInventoryEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemVillagerTradeEvent(player, null, tradeItem.getItem(), clickEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemVillagerTradeEvent(player, tradeItem, null, clickEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemVillagerTradeEvent(player, tradeItem, tradeItem.getItem(), null));
    }

    @Test
    @DisplayName("Clicking a SlimefunItem into the merchant inventory fires the event and blocks the trade")
    void testClickOnMerchantSlotFiresAndBlocks() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTrade(SlimefunItemVillagerTradeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(tradeItem, event.getSlimefunItem());
                Assertions.assertTrue(event.getInventoryEvent() instanceof InventoryClickEvent);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryClickEvent clickEvent = clickOnMerchantSlot(view, tradeItem.getItem().clone());

            Assertions.assertTrue(seen[0], "SlimefunItemVillagerTradeEvent was not fired");
            Assertions.assertTrue(clickEvent.isCancelled(), "The trade must have been blocked");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Clicking a merchant result with a SlimefunItem in the player inventory fires the event")
    void testClickFromPlayerInventoryFiresAndBlocks() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);
        player.getInventory().setItem(0, tradeItem.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTrade(SlimefunItemVillagerTradeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(tradeItem, event.getSlimefunItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // Raw slot 3 lies past the three merchant slots, inside the player inventory half
            InventoryClickEvent clickEvent = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 3, ClickType.LEFT, InventoryAction.PICKUP_ALL);
            server.getPluginManager().callEvent(clickEvent);

            Assertions.assertTrue(seen[0], "SlimefunItemVillagerTradeEvent was not fired");
            Assertions.assertTrue(clickEvent.isCancelled(), "The trade must have been blocked");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunItemVillagerTradeEvent allows the trade")
    void testEventCancellationAllowsTrade() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onTrade(SlimefunItemVillagerTradeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            InventoryClickEvent clickEvent = clickOnMerchantSlot(view, tradeItem.getItem().clone());

            Assertions.assertFalse(clickEvent.isCancelled(), "A cancelled SlimefunItemVillagerTradeEvent must allow the trade");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Dragging a SlimefunItem across the merchant slots fires the event and blocks the trade")
    void testDragFiresAndBlocks() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);
        ItemStack dragged = tradeItem.getItem().clone();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTrade(SlimefunItemVillagerTradeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(tradeItem, event.getSlimefunItem());
                Assertions.assertTrue(event.getInventoryEvent() instanceof InventoryDragEvent);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryDragEvent dragEvent = new InventoryDragEvent(view, null, dragged, false, Map.of(0, dragged));
            server.getPluginManager().callEvent(dragEvent);

            Assertions.assertTrue(seen[0], "SlimefunItemVillagerTradeEvent was not fired");
            Assertions.assertTrue(dragEvent.isCancelled(), "The drag must have been blocked");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunItemVillagerTradeEvent allows the drag")
    void testDragEventCancellationAllowsDrag() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);
        ItemStack dragged = tradeItem.getItem().clone();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onTrade(SlimefunItemVillagerTradeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            InventoryDragEvent dragEvent = new InventoryDragEvent(view, null, dragged, false, Map.of(0, dragged));
            server.getPluginManager().callEvent(dragEvent);

            Assertions.assertFalse(dragEvent.isCancelled(), "A cancelled SlimefunItemVillagerTradeEvent must allow the drag");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A vanilla item fires no event and stays allowed")
    void testVanillaItemStaysAllowed() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTrade(SlimefunItemVillagerTradeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryClickEvent clickEvent = clickOnMerchantSlot(view, new ItemStack(Material.EMERALD));

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla item");
            Assertions.assertFalse(clickEvent.isCancelled(), "A vanilla item must stay tradeable");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A trade without listeners is still blocked, preserving the old behavior")
    void testTradeWithoutListenersStillBlocked() {
        Player player = server.addPlayer();
        SimpleInventoryViewMock view = openMerchantView(player);

        InventoryClickEvent clickEvent = clickOnMerchantSlot(view, tradeItem.getItem().clone());

        Assertions.assertTrue(clickEvent.isCancelled(), "The trade must have been blocked");
    }
}

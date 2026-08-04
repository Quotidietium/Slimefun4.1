package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;
import be.seeseemelk.mockbukkit.inventory.InventoryMock;

import io.github.thebusybiscuit.slimefun4.api.events.ItemPickupPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.events.ItemPickupPreventEvent.PreventReason;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * Regression coverage for the item pickup prevention API expansion:
 * {@link ItemPickupPreventEvent}, exercised through the real {@link ItemPickupListener}
 * no-pickup-flag and altar-probe paths, for both entity and hopper pickups.
 *
 * @author Zurker
 */
class TestItemPickupPreventEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Unit test startups do not register the listeners, register it manually
        new ItemPickupListener(plugin);
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
     * Creates a dropped item entity carrying the given stack.
     */
    private Item dropItem(ItemStack stack) {
        return new ItemEntityMock(server, UUID.randomUUID(), stack);
    }

    /**
     * Creates a dropped altar probe: the display name prefix is the marker the listener
     * recognizes pedestal probe and display items by.
     */
    private Item dropAltarProbe() {
        ItemStack stack = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(AncientPedestal.ITEM_PREFIX + "Test Probe");
        stack.setItemMeta(meta);
        return dropItem(stack);
    }

    /**
     * Lets the player pick up the item through the real event pipeline and returns the
     * event for assertions.
     */
    private EntityPickupItemEvent pickup(Player player, Item item) {
        EntityPickupItemEvent event = new EntityPickupItemEvent(player, item, 0);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("ItemPickupPreventEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Item item = dropItem(new ItemStack(Material.DIAMOND));

        ItemPickupPreventEvent event = new ItemPickupPreventEvent(item, PreventReason.NO_PICKUP_FLAG);

        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(PreventReason.NO_PICKUP_FLAG, event.getReason());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ItemPickupPreventEvent(null, PreventReason.NO_PICKUP_FLAG));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ItemPickupPreventEvent(item, null));
    }

    @Test
    @DisplayName("Picking up a flagged item fires the event and prevents the pickup")
    void testNoPickupFlagFiresAndPrevents() {
        Player player = server.addPlayer();
        Item item = dropItem(new ItemStack(Material.DIAMOND));
        SlimefunUtils.markAsNoPickup(item, "test");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(ItemPickupPreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(item, event.getItem());
                Assertions.assertEquals(PreventReason.NO_PICKUP_FLAG, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityPickupItemEvent event = pickup(player, item);

            Assertions.assertTrue(seen[0], "ItemPickupPreventEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The pickup must have been prevented");
            Assertions.assertTrue(item.isValid(), "A flagged item must not be removed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ItemPickupPreventEvent vetoes the flag prevention")
    void testEventCancellationVetoesFlagPrevention() {
        Player player = server.addPlayer();
        Item item = dropItem(new ItemStack(Material.DIAMOND));
        SlimefunUtils.markAsNoPickup(item, "test");

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPrevent(ItemPickupPreventEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityPickupItemEvent event = pickup(player, item);

            Assertions.assertFalse(event.isCancelled(), "A vetoed prevention must allow the pickup");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Picking up an altar probe fires the event, prevents the pickup and removes the item")
    void testAltarProbeFiresPreventsAndRemoves() {
        Player player = server.addPlayer();
        Item item = dropAltarProbe();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(ItemPickupPreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(item, event.getItem());
                Assertions.assertEquals(PreventReason.ALTAR_PROBE, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityPickupItemEvent event = pickup(player, item);

            Assertions.assertTrue(seen[0], "ItemPickupPreventEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The pickup must have been prevented");
            Assertions.assertFalse(item.isValid(), "The altar probe must have been removed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ItemPickupPreventEvent vetoes the probe removal too")
    void testEventCancellationVetoesProbeRemoval() {
        Player player = server.addPlayer();
        Item item = dropAltarProbe();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPrevent(ItemPickupPreventEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityPickupItemEvent event = pickup(player, item);

            Assertions.assertFalse(event.isCancelled(), "A vetoed prevention must allow the pickup");
            Assertions.assertTrue(item.isValid(), "A vetoed prevention must keep the altar probe alive");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A hopper picking up a flagged item fires the event and prevents the pickup")
    void testHopperPickupFiresAndPrevents() {
        Item item = dropItem(new ItemStack(Material.DIAMOND));
        SlimefunUtils.markAsNoPickup(item, "test");
        InventoryMock hopper = new InventoryMock(null, InventoryType.HOPPER);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(ItemPickupPreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(item, event.getItem());
                Assertions.assertEquals(PreventReason.NO_PICKUP_FLAG, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryPickupItemEvent event = new InventoryPickupItemEvent(hopper, item);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0], "ItemPickupPreventEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The hopper pickup must have been prevented");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A regular item fires no event and is picked up normally")
    void testRegularItemFiresNothing() {
        Player player = server.addPlayer();
        Item item = dropItem(new ItemStack(Material.DIAMOND));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(ItemPickupPreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityPickupItemEvent event = pickup(player, item);

            Assertions.assertFalse(seen[0], "No event must be fired for a regular item");
            Assertions.assertFalse(event.isCancelled(), "A regular item must be picked up normally");
            Assertions.assertTrue(item.isValid(), "A regular item must not be removed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Prevention without listeners still applies, preserving the old behavior")
    void testPreventionWithoutListenersStillApplies() {
        Player player = server.addPlayer();
        Item flagged = dropItem(new ItemStack(Material.DIAMOND));
        SlimefunUtils.markAsNoPickup(flagged, "test");
        Item probe = dropAltarProbe();

        EntityPickupItemEvent flagEvent = pickup(player, flagged);
        EntityPickupItemEvent probeEvent = pickup(player, probe);

        Assertions.assertTrue(flagEvent.isCancelled(), "The pickup of a flagged item must have been prevented");
        Assertions.assertTrue(probeEvent.isCancelled(), "The pickup of an altar probe must have been prevented");
        Assertions.assertFalse(probe.isValid(), "The altar probe must have been removed");
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
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
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;

import io.github.thebusybiscuit.slimefun4.api.events.PiglinBarterPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PiglinBarterPreventEvent.Reason;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

/**
 * Regression coverage for the piglin barter API expansion:
 * {@link PiglinBarterPreventEvent}, exercised through the real {@link PiglinListener}
 * pickup and barter prevention paths. MockBukkit has no piglin mock, so the piglin is a
 * Mockito mock.
 *
 * @author Zurker
 */
class TestPiglinBarterPreventEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static SlimefunItem fakeGold;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Unit test startups do not register the listeners, register it manually
        new io.github.thebusybiscuit.slimefun4.implementation.listeners.entity.PiglinListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "piglin_test");
        Slimefun.getItemCfg().setValue("TEST_FAKE_GOLD.enabled", true);
        fakeGold = new MockSlimefunItem(itemGroup, new ItemStack(Material.GOLD_INGOT), "TEST_FAKE_GOLD");
        fakeGold.register(plugin);
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
     * Creates a Mockito piglin, the only thing the listener needs from it is its type
     * and validity.
     */
    private Piglin mockPiglin() {
        Piglin piglin = Mockito.mock(Piglin.class);
        Mockito.when(piglin.getType()).thenReturn(EntityType.PIGLIN);
        Mockito.when(piglin.isValid()).thenReturn(true);
        return piglin;
    }

    @Test
    @DisplayName("PiglinBarterPreventEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Piglin piglin = mockPiglin();
        ItemStack item = fakeGold.getItem().clone();

        PiglinBarterPreventEvent event = new PiglinBarterPreventEvent(fakeGold, piglin, item, null, Reason.PICKUP);

        Assertions.assertEquals(fakeGold, event.getSlimefunItem());
        Assertions.assertEquals(piglin, event.getPiglin());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertNull(event.getPlayer());
        Assertions.assertEquals(Reason.PICKUP, event.getReason());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PiglinBarterPreventEvent(null, piglin, item, null, Reason.PICKUP));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PiglinBarterPreventEvent(fakeGold, null, item, null, Reason.PICKUP));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PiglinBarterPreventEvent(fakeGold, piglin, null, null, Reason.PICKUP));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PiglinBarterPreventEvent(fakeGold, piglin, item, null, null));
    }

    @Test
    @DisplayName("A piglin trying to pick up Slimefun gold fires the event and is cancelled")
    void testPickupFiresAndCancels() {
        Piglin piglin = mockPiglin();
        ItemEntityMock drop = new ItemEntityMock(server, UUID.randomUUID(), fakeGold.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(PiglinBarterPreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(fakeGold, event.getSlimefunItem());
                Assertions.assertEquals(piglin, event.getPiglin());
                Assertions.assertEquals(Reason.PICKUP, event.getReason());
                Assertions.assertNull(event.getPlayer());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityPickupItemEvent event = new EntityPickupItemEvent(piglin, drop, 0);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0], "PiglinBarterPreventEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The pickup must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling the pickup prevention lets the piglin pick the gold up")
    void testPickupCancellationAllows() {
        Piglin piglin = mockPiglin();
        ItemEntityMock drop = new ItemEntityMock(server, UUID.randomUUID(), fakeGold.getItem().clone());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPrevent(PiglinBarterPreventEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityPickupItemEvent event = new EntityPickupItemEvent(piglin, drop, 0);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(event.isCancelled(), "A vetoed prevention must allow the pickup");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A player bartering Slimefun gold fires the event and is cancelled")
    void testBarterFiresAndCancels() {
        Player player = server.addPlayer();
        player.getInventory().setItemInMainHand(fakeGold.getItem().clone());
        Piglin piglin = mockPiglin();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(PiglinBarterPreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(piglin, event.getPiglin());
                Assertions.assertEquals(Reason.BARTER, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, piglin, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0], "PiglinBarterPreventEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The barter must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling the barter prevention allows the barter")
    void testBarterCancellationAllows() {
        Player player = server.addPlayer();
        player.getInventory().setItemInMainHand(fakeGold.getItem().clone());
        Piglin piglin = mockPiglin();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPrevent(PiglinBarterPreventEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, piglin, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(event.isCancelled(), "A vetoed prevention must allow the barter");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Bartering with vanilla gold fires no event")
    void testVanillaGoldFiresNothing() {
        Player player = server.addPlayer();
        player.getInventory().setItemInMainHand(new ItemStack(Material.GOLD_INGOT));
        Piglin piglin = mockPiglin();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(PiglinBarterPreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, piglin, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for vanilla gold");
            Assertions.assertFalse(event.isCancelled(), "A vanilla barter must be allowed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("The pickup path blocks any Slimefun item, not just gold")
    void testNonGoldPickupStillFires() {
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "piglin_test_other");
        Slimefun.getItemCfg().setValue("TEST_PIGLIN_OTHER.enabled", true);
        SlimefunItem otherItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.DIAMOND), "TEST_PIGLIN_OTHER");
        otherItem.register(plugin);

        Piglin piglin = mockPiglin();
        ItemEntityMock drop = new ItemEntityMock(server, UUID.randomUUID(), otherItem.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(PiglinBarterPreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityPickupItemEvent event = new EntityPickupItemEvent(piglin, drop, 0);
            server.getPluginManager().callEvent(event);

            // The pickup path blocks ANY Slimefun item, not just gold - so the event fires
            // with the diamond Slimefun item. This documents that behavior.
            Assertions.assertTrue(seen[0], "The pickup path blocks any Slimefun item, so the event fires");
            Assertions.assertTrue(event.isCancelled(), "The pickup of any Slimefun item must be cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Prevention without listeners still applies, preserving the old behavior")
    void testPreventionWithoutListenersStillApplies() {
        Piglin piglin = mockPiglin();
        ItemEntityMock drop = new ItemEntityMock(server, UUID.randomUUID(), fakeGold.getItem().clone());

        EntityPickupItemEvent event = new EntityPickupItemEvent(piglin, drop, 0);
        server.getPluginManager().callEvent(event);

        Assertions.assertTrue(event.isCancelled(), "The pickup must have been cancelled");
    }
}

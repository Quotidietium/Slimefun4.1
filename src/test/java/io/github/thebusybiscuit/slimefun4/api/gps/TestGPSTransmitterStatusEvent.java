package io.github.thebusybiscuit.slimefun4.api.gps;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.GPSTransmitterStatusEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the GPS network API expansion:
 * {@link GPSTransmitterStatusEvent}, exercised by driving the public
 * {@link GPSNetwork#updateTransmitter(Location, UUID, boolean)} choke point.
 * <p>
 * The event only fires on real status flips: the ticker re-asserts the status
 * every tick, so idempotent updates must stay silent. The transmitter set is
 * observable through {@link GPSNetwork#getTransmitters(UUID)}, so both the event
 * and the state transition are asserted.
 *
 * @author Zurker
 */
class TestGPSTransmitterStatusEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static GPSNetwork network;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = server.addSimpleWorld("gps_status");
        network = Slimefun.getGPSNetwork();
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("GPSTransmitterStatusEvent exposes its fields, resolves the owner and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Location l = new Location(world, 1, 64, 1);

        GPSTransmitterStatusEvent event = new GPSTransmitterStatusEvent(l, player.getUniqueId(), true);

        Assertions.assertEquals(l, event.getLocation());
        Assertions.assertEquals(player.getUniqueId(), event.getOwner());
        Assertions.assertTrue(event.isOnline());
        Assertions.assertEquals(player, event.getOwnerPlayer(), "An online owner must resolve to their Player");

        GPSTransmitterStatusEvent offline = new GPSTransmitterStatusEvent(l, UUID.randomUUID(), false);
        Assertions.assertFalse(offline.isOnline());
        Assertions.assertNull(offline.getOwnerPlayer(), "An offline owner must resolve to null");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GPSTransmitterStatusEvent(null, player.getUniqueId(), true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GPSTransmitterStatusEvent(l, null, true));
    }

    @Test
    @DisplayName("Coming online fires the event and registers the transmitter")
    void testOnlineFlipFiresEvent() {
        UUID owner = server.addPlayer().getUniqueId();
        Location l = new Location(world, 10, 64, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStatus(GPSTransmitterStatusEvent event) {
                seen[0] = true;
                Assertions.assertEquals(l, event.getLocation());
                Assertions.assertEquals(owner, event.getOwner());
                Assertions.assertTrue(event.isOnline());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            network.updateTransmitter(l, owner, true);

            Assertions.assertTrue(seen[0], "GPSTransmitterStatusEvent was not fired");
            Assertions.assertTrue(network.getTransmitters(owner).contains(l), "The transmitter must have been registered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Going offline fires the event and unregisters the transmitter")
    void testOfflineFlipFiresEvent() {
        UUID owner = server.addPlayer().getUniqueId();
        Location l = new Location(world, 20, 64, 20);
        network.updateTransmitter(l, owner, true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStatus(GPSTransmitterStatusEvent event) {
                seen[0] = true;
                Assertions.assertEquals(l, event.getLocation());
                Assertions.assertEquals(owner, event.getOwner());
                Assertions.assertFalse(event.isOnline());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            network.updateTransmitter(l, owner, false);

            Assertions.assertTrue(seen[0], "GPSTransmitterStatusEvent was not fired");
            Assertions.assertFalse(network.getTransmitters(owner).contains(l), "The transmitter must have been unregistered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Re-asserting an online transmitter fires no event")
    void testIdempotentOnlineStaysSilent() {
        UUID owner = server.addPlayer().getUniqueId();
        Location l = new Location(world, 30, 64, 30);
        network.updateTransmitter(l, owner, true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStatus(GPSTransmitterStatusEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            network.updateTransmitter(l, owner, true);

            Assertions.assertFalse(seen[0], "An idempotent update must not fire the event");
            Assertions.assertTrue(network.getTransmitters(owner).contains(l), "The transmitter must still be registered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Taking an offline transmitter offline fires no event")
    void testNeverOnlineOfflineStaysSilent() {
        UUID owner = server.addPlayer().getUniqueId();
        Location l = new Location(world, 40, 64, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStatus(GPSTransmitterStatusEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            network.updateTransmitter(l, owner, false);

            Assertions.assertFalse(seen[0], "Removing an absent transmitter must not fire the event");
            Assertions.assertTrue(network.getTransmitters(owner).isEmpty(), "No transmitter must have been registered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Status flips without listeners still update the network, preserving the old behavior")
    void testFlipsWithoutListenersUpdateNetwork() {
        UUID owner = server.addPlayer().getUniqueId();
        Location l = new Location(world, 50, 64, 50);

        network.updateTransmitter(l, owner, true);
        Assertions.assertTrue(network.getTransmitters(owner).contains(l), "The transmitter must have been registered");

        network.updateTransmitter(l, owner, false);
        Assertions.assertFalse(network.getTransmitters(owner).contains(l), "The transmitter must have been unregistered");
    }
}

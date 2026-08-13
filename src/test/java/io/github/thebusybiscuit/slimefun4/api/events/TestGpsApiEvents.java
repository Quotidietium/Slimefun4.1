package io.github.thebusybiscuit.slimefun4.api.events;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Location;
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

import io.github.thebusybiscuit.slimefun4.api.gps.Waypoint;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.FileUtils;

/**
 * Regression coverage for the GPS API expansion: {@link TeleportationStartEvent},
 * {@link TeleportationCompleteEvent} and {@link WaypointRemoveEvent}.
 *
 * @author Zurker
 */
class TestGpsApiEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static File dataFolder;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        dataFolder = new File("data-storage/Slimefun/waypoints");
        dataFolder.mkdirs();
    }

    @AfterAll
    public static void unload() throws IOException {
        MockBukkit.unmock();
        FileUtils.deleteDirectory(dataFolder);
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("TeleportationStartEvent exposes uuid, complexity, endpoints, resistance and cancellation")
    void testTeleportationStartEventFields() {
        Player player = server.addPlayer();
        UUID uuid = player.getUniqueId();
        Location source = player.getLocation();
        Location destination = player.getLocation().add(100, 0, 100);

        TeleportationStartEvent event = new TeleportationStartEvent(uuid, 500, source, destination, true);

        Assertions.assertEquals(uuid, event.getUUID());
        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(500, event.getComplexity());
        Assertions.assertEquals(source, event.getSource());
        Assertions.assertEquals(destination, event.getDestination());
        Assertions.assertTrue(event.hasResistance());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        // An offline UUID yields a null Player
        TeleportationStartEvent offline = new TeleportationStartEvent(UUID.randomUUID(), 100, source, destination, false);
        Assertions.assertNull(offline.getPlayer());
        Assertions.assertFalse(offline.hasResistance());
    }

    @Test
    @DisplayName("TeleportationStartEvent defaults to the computed time and validates overrides")
    void testTeleportationTimeOverride() {
        Player player = server.addPlayer();
        Location source = player.getLocation();
        Location destination = player.getLocation().add(100, 0, 100);

        TeleportationStartEvent event = new TeleportationStartEvent(player.getUniqueId(), 500, source, destination, false);

        Assertions.assertEquals(-1, event.getTeleportationTime(), "The time must default to the computed value");

        event.setTeleportationTime(2);
        Assertions.assertEquals(2, event.getTeleportationTime());

        event.setTeleportationTime(1);
        Assertions.assertEquals(1, event.getTeleportationTime(), "A time of 1 is the effectively-instant minimum");

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTeleportationTime(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTeleportationTime(-5));
    }

    @Test
    @DisplayName("TeleportationStartEvent.setDestination rejects worldless and non-finite destinations")
    void testSetDestinationValidation() {
        Player player = server.addPlayer();
        Location source = player.getLocation();
        Location destination = player.getLocation().add(5, 0, 5);

        TeleportationStartEvent event = new TeleportationStartEvent(player.getUniqueId(), 500, source, destination, false);

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDestination(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDestination(new Location(null, 1, 64, 1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDestination(new Location(player.getWorld(), Double.NaN, 64, 1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDestination(new Location(player.getWorld(), 1, Double.POSITIVE_INFINITY, 1)));

        // A valid destination is still accepted
        Location valid = player.getLocation().add(10, 0, 10);
        Assertions.assertDoesNotThrow(() -> event.setDestination(valid));
        Assertions.assertEquals(valid, event.getDestination());
    }

    @Test
    @DisplayName("TeleportationCompleteEvent exposes uuid, destination and resistance and is not cancellable")
    void testTeleportationCompleteEventFields() {
        Player player = server.addPlayer();
        Location destination = player.getLocation();

        TeleportationCompleteEvent event = new TeleportationCompleteEvent(player.getUniqueId(), destination, false);

        Assertions.assertEquals(player.getUniqueId(), event.getUUID());
        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(destination, event.getDestination());
        Assertions.assertFalse(event.hasResistance());
        Assertions.assertFalse(event instanceof org.bukkit.event.Cancellable);
    }

    @Test
    @DisplayName("Cancelling WaypointRemoveEvent keeps the waypoint in the profile")
    void testWaypointRemoveEventCancellation() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        Waypoint waypoint = new Waypoint(player.getUniqueId(), "keep-me", player.getLocation(), "KEEP_ME");
        profile.addWaypoint(waypoint);
        Assertions.assertEquals(1, profile.getWaypoints().size());

        Listener listener = new Listener() {
            @EventHandler
            public void onRemove(WaypointRemoveEvent event) {
                Assertions.assertEquals(profile, event.getProfile());
                Assertions.assertEquals(player.getUniqueId(), event.getUUID());
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(waypoint, event.getWaypoint());
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            profile.removeWaypoint(waypoint);
            // Cancellation prevented the removal
            Assertions.assertEquals(1, profile.getWaypoints().size());
            Assertions.assertEquals(waypoint, profile.getWaypoints().get(0));
        } finally {
            HandlerList.unregisterAll(listener);
        }

        // Without a listener the removal behaves exactly as before
        profile.removeWaypoint(waypoint);
        Assertions.assertTrue(profile.getWaypoints().isEmpty());
    }

    @Test
    @DisplayName("TeleportationManager.isTeleporting reports only players with an active teleporter session")
    void testIsTeleporting() {
        Player player = server.addPlayer();

        Assertions.assertFalse(Slimefun.getGPSNetwork().getTeleportationManager().isTeleporting(player.getUniqueId()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Slimefun.getGPSNetwork().getTeleportationManager().isTeleporting(null));
    }

    @Test
    @DisplayName("Teleportation events are dispatchable to listeners")
    void testTeleportationEventDispatch() {
        Player player = server.addPlayer();
        Location source = player.getLocation();
        Location destination = player.getLocation().add(50, 0, 50);

        TeleportationListener listener = new TeleportationListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            server.getPluginManager().callEvent(new TeleportationStartEvent(player.getUniqueId(), 200, source, destination, false));
            server.getPluginManager().callEvent(new TeleportationCompleteEvent(player.getUniqueId(), destination, false));

            Assertions.assertTrue(listener.startSeen);
            Assertions.assertTrue(listener.completeSeen);
            Assertions.assertEquals(200, listener.seenComplexity);
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    private static class TeleportationListener implements Listener {
        boolean startSeen;
        boolean completeSeen;
        int seenComplexity;

        @EventHandler
        public void onStart(TeleportationStartEvent event) {
            startSeen = true;
            seenComplexity = event.getComplexity();
        }

        @EventHandler
        public void onComplete(TeleportationCompleteEvent event) {
            completeSeen = true;
        }
    }
}

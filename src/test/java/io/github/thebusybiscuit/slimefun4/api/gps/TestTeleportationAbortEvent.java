package io.github.thebusybiscuit.slimefun4.api.gps;

import java.util.ArrayList;
import java.util.List;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.TeleportationAbortEvent;
import io.github.thebusybiscuit.slimefun4.api.events.TeleportationAbortEvent.AbortReason;
import io.github.thebusybiscuit.slimefun4.api.events.TeleportationStartEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the teleportation lifecycle API expansion:
 * {@link TeleportationAbortEvent}, exercised through the real
 * {@link TeleportationManager#teleport} and {@link TeleportationManager#onTeleport}
 * paths.
 * <p>
 * The abort paths are fully observable under MockBukkit because they never reach the
 * PORTAL particles of a progressing teleport (which MockBukkit does not implement):
 * a player standing away from the source fails the validity poll immediately, and the
 * asynchronous-teleport failure path is driven directly through the package-private
 * {@code onTeleport}.
 *
 * @author Zurker
 */
class TestTeleportationAbortEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static TeleportationManager manager;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        manager = Slimefun.getGPSNetwork().getTeleportationManager();
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
     * A source/destination pair far away from the world spawn, so a freshly added
     * player always fails the validity poll of the teleport sequence.
     */
    private Location source() {
        return new Location(world, 1000.5, 72, 1000.5);
    }

    private Location destination() {
        return new Location(world, 1100.5, 72, 1100.5);
    }

    @Test
    @DisplayName("TeleportationAbortEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Location destination = destination();

        TeleportationAbortEvent event = new TeleportationAbortEvent(player.getUniqueId(), destination, AbortReason.INTERRUPTED);

        Assertions.assertEquals(player.getUniqueId(), event.getUUID());
        Assertions.assertEquals(player, event.getPlayer(), "An online player must resolve through their UUID");
        Assertions.assertEquals(destination, event.getDestination());
        Assertions.assertEquals(AbortReason.INTERRUPTED, event.getReason());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new TeleportationAbortEvent(null, destination, AbortReason.INTERRUPTED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TeleportationAbortEvent(player.getUniqueId(), null, AbortReason.INTERRUPTED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TeleportationAbortEvent(player.getUniqueId(), destination, null));
    }

    @Test
    @DisplayName("Moving away from the teleporter aborts with INTERRUPTED after the start event")
    void testMovedAwayAbortsInterrupted() {
        Player player = server.addPlayer();
        Location before = player.getLocation();

        List<String> sequence = new ArrayList<>();
        TeleportationAbortEvent[] aborted = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStart(TeleportationStartEvent event) {
                sequence.add("start");
            }

            @EventHandler
            public void onAbort(TeleportationAbortEvent event) {
                sequence.add("abort");
                aborted[0] = event;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            manager.teleport(player.getUniqueId(), 100, source(), destination(), false);

            Assertions.assertEquals(List.of("start", "abort"), sequence, "The abort must follow the start event");

            Assertions.assertNotNull(aborted[0], "TeleportationAbortEvent was not fired");
            Assertions.assertEquals(player.getUniqueId(), aborted[0].getUUID());
            Assertions.assertEquals(destination(), aborted[0].getDestination());
            Assertions.assertEquals(AbortReason.INTERRUPTED, aborted[0].getReason());

            Assertions.assertEquals(before, player.getLocation(), "An aborted teleportation must not move the player");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A failed asynchronous teleport aborts with TELEPORT_FAILED")
    void testTeleportFailedAbortsWithFailedReason() {
        // nextTitle() lives on PlayerMock, not on the Bukkit Player interface
        PlayerMock player = server.addPlayer();
        Location before = player.getLocation();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAbort(TeleportationAbortEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player.getUniqueId(), event.getUUID());
                Assertions.assertEquals(destination(), event.getDestination());
                Assertions.assertEquals(AbortReason.TELEPORT_FAILED, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            manager.onTeleport(player, destination(), false, false);

            Assertions.assertTrue(seen[0], "TeleportationAbortEvent was not fired");
            Assertions.assertEquals(before, player.getLocation(), "A failed teleportation must not move the player");
            Assertions.assertNotNull(player.nextTitle(), "The player must have been notified about the cancellation");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An offline player's teleportation aborts without a title and still fires the event")
    void testOfflinePlayerAborts() {
        UUID offline = UUID.randomUUID();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAbort(TeleportationAbortEvent event) {
                seen[0] = true;
                Assertions.assertEquals(offline, event.getUUID());
                Assertions.assertNull(event.getPlayer(), "An offline player must not resolve");
                Assertions.assertEquals(AbortReason.INTERRUPTED, event.getReason());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            manager.teleport(offline, 100, source(), destination(), false);

            Assertions.assertTrue(seen[0], "TeleportationAbortEvent was not fired for an offline player");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Aborting without listeners still tears the teleportation down, preserving the old behavior")
    void testAbortWithoutListeners() {
        // nextTitle() lives on PlayerMock, not on the Bukkit Player interface
        PlayerMock player = server.addPlayer();
        Location before = player.getLocation();

        manager.teleport(player.getUniqueId(), 100, source(), destination(), false);

        Assertions.assertEquals(before, player.getLocation(), "An aborted teleportation must not move the player");
        Assertions.assertNotNull(player.nextTitle(), "The player must have been notified about the cancellation");
    }
}

package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.block.Block;
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
import be.seeseemelk.mockbukkit.WorldMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorFloor;

/**
 * Regression coverage for the elevator API expansion: {@link ElevatorTeleportEvent}
 * and the now-public {@link ElevatorFloor}.
 *
 * @author Zurker
 */
class TestElevatorTeleportEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = server.addSimpleWorld("elevators");
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
    @DisplayName("ElevatorFloor exposes name, number, altitude and location")
    void testElevatorFloorAccessors() {
        Block block = world.getBlockAt(5, 70, 5);
        ElevatorFloor floor = new ElevatorFloor("&bLobby", 2, block);

        Assertions.assertEquals("&bLobby", floor.getName());
        Assertions.assertEquals(2, floor.getNumber());
        Assertions.assertEquals(70, floor.getAltitude());
        Assertions.assertEquals(block.getLocation(), floor.getLocation());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElevatorFloor(null, 0, block));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElevatorFloor("Floor", 0, null));
    }

    @Test
    @DisplayName("ElevatorTeleportEvent exposes player, floor and cancellation")
    void testEventFieldsAndCancellation() {
        Player player = server.addPlayer();
        ElevatorFloor floor = new ElevatorFloor("&cRoof", 3, world.getBlockAt(1, 90, 1));

        ElevatorTeleportEvent event = new ElevatorTeleportEvent(player, floor);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(floor, event.getFloor());
        Assertions.assertFalse(event.isCancelled());

        ElevatorFloor redirect = new ElevatorFloor("&eSecret Lab", 7, world.getBlockAt(9, 120, 9));
        event.setFloor(redirect);
        Assertions.assertEquals(redirect, event.getFloor());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElevatorTeleportEvent(player, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setFloor(null));
    }

    @Test
    @DisplayName("setFloor rejects a floor from another world and accepts a same-world redirect")
    void testSetFloorRejectsOtherWorld() {
        Player player = server.addPlayer();
        WorldMock foreign = server.addSimpleWorld("elevators_foreign");

        ElevatorFloor homeFloor = new ElevatorFloor("&bHome", 1, player.getWorld().getBlockAt(1, 70, 1));
        ElevatorTeleportEvent event = new ElevatorTeleportEvent(player, homeFloor);

        // A floor from another world would drop the player at unchecked coordinates
        ElevatorFloor foreignFloor = new ElevatorFloor("&bForeign", 2, foreign.getBlockAt(2, 70, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setFloor(foreignFloor));

        ElevatorFloor sameWorldFloor = new ElevatorFloor("&bOther", 3, player.getWorld().getBlockAt(3, 70, 3));
        Assertions.assertDoesNotThrow(() -> event.setFloor(sameWorldFloor));
        Assertions.assertEquals(sameWorldFloor, event.getFloor());
    }

    @Test
    @DisplayName("ElevatorTeleportEvent is dispatchable to listeners")
    void testEventDispatch() {
        Player player = server.addPlayer();
        ElevatorFloor floor = new ElevatorFloor("&aBasement", 0, world.getBlockAt(2, 40, 2));

        ElevatorListener listener = new ElevatorListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            ElevatorTeleportEvent event = new ElevatorTeleportEvent(player, floor);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(listener.seen);
            Assertions.assertEquals(floor, listener.seenFloor);
            Assertions.assertTrue(event.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    private static class ElevatorListener implements Listener {
        boolean seen;
        ElevatorFloor seenFloor;

        @EventHandler
        public void onTeleport(ElevatorTeleportEvent event) {
            seen = true;
            seenFloor = event.getFloor();
            event.setCancelled(true);
        }
    }
}

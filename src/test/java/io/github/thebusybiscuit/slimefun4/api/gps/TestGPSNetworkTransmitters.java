package io.github.thebusybiscuit.slimefun4.api.gps;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

class TestGPSNetworkTransmitters {

    private static ServerMock server;
    private static GPSNetwork network;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
        network = Slimefun.getGPSNetwork();
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test transmitters being registered and unregistered")
    void testUpdateTransmitter() {
        World world = server.addSimpleWorld("gps-test");
        UUID owner = UUID.randomUUID();
        Location one = new Location(world, 10, 70, 10);
        Location two = new Location(world, 20, 70, 20);

        network.updateTransmitter(one, owner, true);
        network.updateTransmitter(two, owner, true);

        Assertions.assertEquals(2, network.countTransmitters(owner));
        Assertions.assertEquals(2, network.getTransmitters(owner).size());

        network.updateTransmitter(one, owner, false);

        Assertions.assertEquals(1, network.countTransmitters(owner));
        Assertions.assertFalse(network.getTransmitters(owner).contains(one));
        Assertions.assertTrue(network.getTransmitters(owner).contains(two));

        network.updateTransmitter(two, owner, false);

        Assertions.assertEquals(0, network.countTransmitters(owner));
        Assertions.assertTrue(network.getTransmitters(owner).isEmpty());
    }

    @Test
    @DisplayName("Test that owners without transmitters do not linger in the map")
    void testEmptyEntriesAreDropped() {
        World world = server.addSimpleWorld("gps-test-2");
        UUID owner = UUID.randomUUID();
        Location location = new Location(world, 10, 70, 10);

        // An "offline" update for an unknown owner must be a no-op
        network.updateTransmitter(location, owner, false);
        Assertions.assertEquals(0, network.countTransmitters(owner));

        network.updateTransmitter(location, owner, true);
        network.updateTransmitter(location, owner, false);

        Assertions.assertEquals(0, network.countTransmitters(owner));

        // Re-registering after the entry was dropped must still work
        network.updateTransmitter(location, owner, true);
        Assertions.assertEquals(1, network.countTransmitters(owner));
        network.updateTransmitter(location, owner, false);
    }

}

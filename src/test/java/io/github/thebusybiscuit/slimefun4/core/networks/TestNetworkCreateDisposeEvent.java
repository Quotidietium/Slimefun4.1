package io.github.thebusybiscuit.slimefun4.core.networks;

import org.bukkit.Location;
import org.bukkit.World;
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

import io.github.thebusybiscuit.slimefun4.api.events.NetworkCreateEvent;
import io.github.thebusybiscuit.slimefun4.api.events.NetworkDisposeEvent;
import io.github.thebusybiscuit.slimefun4.api.network.Network;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the network lifecycle API expansion:
 * {@link NetworkCreateEvent} and {@link NetworkDisposeEvent}, exercised by driving the
 * real {@link NetworkManager#registerNetwork} / {@link NetworkManager#unregisterNetwork}
 * paths through the {@link CargoNet}/{@link EnergyNet} factory methods.
 * <p>
 * Both events are informational (not cancellable): the network has already been added to
 * or removed from the manager. The factory creates a fresh network the first time and
 * returns the cached one afterwards, so create fires once per location until disposal.
 *
 * @author Zurker
 */
class TestNetworkCreateDisposeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Location loc(int x, int z) {
        return new Location(world, x, 60, z);
    }

    @Test
    @DisplayName("NetworkCreateEvent exposes its fields and validates constructor arguments")
    void testCreateEventFieldsAndValidation() {
        CargoNet network = CargoNet.getNetworkFromLocationOrCreate(loc(1, 1));

        NetworkCreateEvent event = new NetworkCreateEvent(network);
        Assertions.assertEquals(network, event.getNetwork());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new NetworkCreateEvent(null));
    }

    @Test
    @DisplayName("NetworkDisposeEvent exposes its fields and validates constructor arguments")
    void testDisposeEventFieldsAndValidation() {
        CargoNet network = CargoNet.getNetworkFromLocationOrCreate(loc(2, 2));

        NetworkDisposeEvent event = new NetworkDisposeEvent(network);
        Assertions.assertEquals(network, event.getNetwork());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new NetworkDisposeEvent(null));
    }

    @Test
    @DisplayName("Both events declare their thread context correctly (async off the main thread)")
    void testAsyncDeclaration() throws InterruptedException {
        CargoNet network = CargoNet.getNetworkFromLocationOrCreate(loc(3, 3));

        java.util.concurrent.atomic.AtomicBoolean createAsync = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean disposeAsync = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread worker = new Thread(() -> {
            createAsync.set(new NetworkCreateEvent(network).isAsynchronous());
            disposeAsync.set(new NetworkDisposeEvent(network).isAsynchronous());
        });
        worker.start();
        worker.join(5000);

        Assertions.assertTrue(createAsync.get(), "Constructed off the main thread (e.g. async cargo ticker), the event must declare itself asynchronous");
        Assertions.assertTrue(disposeAsync.get(), "Constructed off the main thread, the event must declare itself asynchronous");
        Assertions.assertFalse(new NetworkCreateEvent(network).isAsynchronous(), "Constructed on the main thread, the event must declare itself synchronous");
        Assertions.assertFalse(new NetworkDisposeEvent(network).isAsynchronous(), "Constructed on the main thread, the event must declare itself synchronous");
    }

    @Test
    @DisplayName("Creating a cargo network fires NetworkCreateEvent")
    void testCargoNetCreateFiresEvent() {
        Location l = loc(10, 10);

        Network[] captured = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCreate(NetworkCreateEvent event) {
                captured[0] = event.getNetwork();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            CargoNet network = CargoNet.getNetworkFromLocationOrCreate(l);

            Assertions.assertNotNull(captured[0], "NetworkCreateEvent was not fired");
            Assertions.assertSame(network, captured[0], "The event must carry the created network");
            Assertions.assertTrue(network instanceof CargoNet);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Creating an energy network fires NetworkCreateEvent")
    void testEnergyNetCreateFiresEvent() {
        Location l = loc(20, 20);

        Network[] captured = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCreate(NetworkCreateEvent event) {
                captured[0] = event.getNetwork();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EnergyNet network = EnergyNet.getNetworkFromLocationOrCreate(l);

            Assertions.assertNotNull(captured[0], "NetworkCreateEvent was not fired");
            Assertions.assertSame(network, captured[0]);
            Assertions.assertTrue(network instanceof EnergyNet);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A cached network does not fire NetworkCreateEvent again")
    void testCachedNetworkDoesNotRefire() {
        Location l = loc(30, 30);

        // First creation registers and (with a listener present now) fires once
        CargoNet first = CargoNet.getNetworkFromLocationOrCreate(l);

        int[] count = { 0 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCreate(NetworkCreateEvent event) {
                count[0]++;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            CargoNet second = CargoNet.getNetworkFromLocationOrCreate(l);

            Assertions.assertSame(first, second, "The cached network must be returned");
            Assertions.assertEquals(0, count[0], "No create event must fire for an already-registered network");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Disposing of a network fires NetworkDisposeEvent")
    void testDisposeFiresEvent() {
        Location l = loc(40, 40);
        CargoNet network = CargoNet.getNetworkFromLocationOrCreate(l);

        Network[] captured = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDispose(NetworkDisposeEvent event) {
                captured[0] = event.getNetwork();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Slimefun.getNetworkManager().unregisterNetwork(network);

            Assertions.assertNotNull(captured[0], "NetworkDisposeEvent was not fired");
            Assertions.assertSame(network, captured[0]);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Re-creating a disposed network fires NetworkCreateEvent again")
    void testRecreateAfterDisposeFiresAgain() {
        Location l = loc(50, 50);
        CargoNet first = CargoNet.getNetworkFromLocationOrCreate(l);
        Slimefun.getNetworkManager().unregisterNetwork(first);

        Network[] captured = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCreate(NetworkCreateEvent event) {
                captured[0] = event.getNetwork();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            CargoNet second = CargoNet.getNetworkFromLocationOrCreate(l);

            Assertions.assertNotNull(captured[0], "NetworkCreateEvent was not fired on re-creation");
            Assertions.assertNotSame(first, second, "A brand-new network must have been created");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Creating a network without listeners still registers it, preserving the old behavior")
    void testCreateWithoutListenersRegisters() {
        Location l = loc(60, 60);
        CargoNet network = CargoNet.getNetworkFromLocationOrCreate(l);

        Assertions.assertTrue(Slimefun.getNetworkManager().getNetworkList().contains(network), "The network must have been registered");
    }
}

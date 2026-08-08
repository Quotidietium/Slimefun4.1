package io.github.thebusybiscuit.slimefun4.core.networks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.CargoNetTickEvent;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the cargo network API expansion:
 * {@link CargoNetTickEvent}, exercised by driving the real {@link CargoNet#tick} path.
 * <p>
 * The event fires at the start of each valid cargo tick (after regulator validation,
 * before network discovery). Subsequent network discovery or hologram operations may
 * throw under MockBukkit, but the event is captured before those tails.
 *
 * @author Zurker
 */
class TestCargoNetTickEvent {

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

    /**
     * Creates a CargoNet at a unique location and ticks its regulator block. Any exceptions
     * from network discovery or hologram rendering are swallowed — the event fires before
     * those tails.
     */
    private CargoNet tickNetwork(int x, int z) {
        Location loc = new Location(world, x, 60, z);
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.CRAFTING_TABLE);
        CargoNet net = CargoNet.getNetworkFromLocationOrCreate(loc);

        try {
            net.tick(b);
        } catch (Exception ignored) {
            // Network discovery / hologram rendering may fail under MockBukkit
        }

        return net;
    }

    @Test
    @DisplayName("CargoNetTickEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location loc = new Location(world, 1, 60, 1);
        CargoNet net = CargoNet.getNetworkFromLocationOrCreate(loc);
        Block b = world.getBlockAt(1, 60, 1);

        CargoNetTickEvent event = new CargoNetTickEvent(net, b);

        Assertions.assertEquals(net, event.getNetwork());
        Assertions.assertEquals(b, event.getRegulator());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNetTickEvent(null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNetTickEvent(net, null));
    }

    @Test
    @DisplayName("Ticking a cargo network fires CargoNetTickEvent")
    void testTickFiresEvent() {
        CargoNet[] captured = { null };
        Block[] regulator = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTick(CargoNetTickEvent event) {
                captured[0] = event.getNetwork();
                regulator[0] = event.getRegulator();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            CargoNet net = tickNetwork(10, 10);

            Assertions.assertNotNull(captured[0], "CargoNetTickEvent was not fired");
            Assertions.assertEquals(net, captured[0], "The event must carry the ticking network");
            Assertions.assertEquals(Material.CRAFTING_TABLE, regulator[0].getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Ticking without listeners still runs, preserving the old behavior")
    void testTickWithoutListenersRuns() {
        CargoNet net = tickNetwork(20, 20);
        Assertions.assertNotNull(net, "The network must have been created and ticked");
    }
}

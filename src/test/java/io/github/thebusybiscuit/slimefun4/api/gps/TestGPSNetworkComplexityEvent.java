package io.github.thebusybiscuit.slimefun4.api.gps;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.GPSNetworkComplexityEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.gps.GPSTransmitter;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the GPS network API expansion:
 * {@link GPSNetworkComplexityEvent}, exercised by placing and removing a
 * {@link GPSTransmitter} via the real {@link GPSNetwork#updateTransmitter} path.
 *
 * @author Zurker
 */
class TestGPSNetworkComplexityEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static GPSTransmitter transmitter;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "gps_complexity_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_GPS_TRANSMITTER", Material.IRON_BARS, "&fTest GPS Transmitter");
        Slimefun.getItemCfg().setValue("_TEST_GPS_TRANSMITTER.enabled", true);
        transmitter = new GPSTransmitter(itemGroup, 1, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public int getMultiplier(int y) {
                return y + 100;
            }

            @Override
            public int getEnergyConsumption() {
                return 10;
            }
        };
        transmitter.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Location placeTransmitter(int x, int z) {
        Block b = world.getBlockAt(x, 64, z);
        b.setType(Material.IRON_BARS);
        BlockStorage.addBlockInfo(b, "id", transmitter.getId());
        return b.getLocation();
    }

    @Test
    @DisplayName("GPSNetworkComplexityEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        UUID uuid = UUID.randomUUID();

        GPSNetworkComplexityEvent event = new GPSNetworkComplexityEvent(uuid, 100, 200);

        Assertions.assertEquals(uuid, event.getUUID());
        Assertions.assertEquals(100, event.getOldComplexity());
        Assertions.assertEquals(200, event.getNewComplexity());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GPSNetworkComplexityEvent(null, 100, 200));
    }

    @Test
    @DisplayName("GPSNetworkComplexityEvent declares its thread context correctly (async off the main thread)")
    void testAsyncDeclaration() throws InterruptedException {
        java.util.concurrent.atomic.AtomicBoolean async = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread worker = new Thread(() -> async.set(new GPSNetworkComplexityEvent(java.util.UUID.randomUUID(), 100, 200).isAsynchronous()));
        worker.start();
        worker.join(5000);

        Assertions.assertTrue(async.get(), "Constructed on the transmitter ticker thread, the event must declare itself asynchronous");
        Assertions.assertFalse(new GPSNetworkComplexityEvent(java.util.UUID.randomUUID(), 100, 200).isAsynchronous(), "Constructed on the main thread, the event must declare itself synchronous");
    }

    @Test
    @DisplayName("Adding a transmitter fires GPSNetworkComplexityEvent with increased complexity")
    void testAddTransmitterFiresEvent() {
        UUID uuid = UUID.randomUUID();
        Location loc = placeTransmitter(10, 10);

        int[] oldComp = { -1 };
        int[] newComp = { -1 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onComplexity(GPSNetworkComplexityEvent event) {
                if (event.getUUID().equals(uuid)) {
                    oldComp[0] = event.getOldComplexity();
                    newComp[0] = event.getNewComplexity();
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Slimefun.getGPSNetwork().updateTransmitter(loc, uuid, true);

            Assertions.assertTrue(newComp[0] > 0, "The new complexity must be positive");
            Assertions.assertTrue(newComp[0] > oldComp[0], "The complexity must have increased");
        } finally {
            // Clean up: remove the transmitter so it doesn't affect other tests
            Slimefun.getGPSNetwork().updateTransmitter(loc, uuid, false);
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Removing a transmitter fires GPSNetworkComplexityEvent with decreased complexity")
    void testRemoveTransmitterFiresEvent() {
        UUID uuid = UUID.randomUUID();
        Location loc = placeTransmitter(20, 20);

        int[] oldComp = { -1 };
        int[] newComp = { -1 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onComplexity(GPSNetworkComplexityEvent event) {
                if (event.getUUID().equals(uuid)) {
                    oldComp[0] = event.getOldComplexity();
                    newComp[0] = event.getNewComplexity();
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // Add with listener active so the complexity cache is populated
            Slimefun.getGPSNetwork().updateTransmitter(loc, uuid, true);
            int addedComplexity = newComp[0];
            Assertions.assertTrue(addedComplexity > 0, "Precondition: complexity must be positive after adding");

            // Reset for removal
            oldComp[0] = -1;
            newComp[0] = -1;

            Slimefun.getGPSNetwork().updateTransmitter(loc, uuid, false);

            Assertions.assertEquals(addedComplexity, oldComp[0], "The old complexity must match the value before removal");
            Assertions.assertEquals(0, newComp[0], "The new complexity must be 0 after removing the only transmitter");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Without listeners the update still works, preserving the old behavior")
    void testUpdateWithoutListenersWorks() {
        UUID uuid = UUID.randomUUID();
        Location loc = placeTransmitter(30, 30);

        Slimefun.getGPSNetwork().updateTransmitter(loc, uuid, true);
        Assertions.assertTrue(Slimefun.getGPSNetwork().getNetworkComplexity(uuid) > 0);

        Slimefun.getGPSNetwork().updateTransmitter(loc, uuid, false);
        Assertions.assertEquals(0, Slimefun.getGPSNetwork().getNetworkComplexity(uuid));
    }
}

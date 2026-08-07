package io.github.thebusybiscuit.slimefun4.implementation.items.geo;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.GEOScanEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.gps.GPSTransmitter;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the geo scan API expansion: {@link GEOScanEvent}, exercised by
 * driving the real {@link GEOScanner}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler} with a constructed
 * {@link PlayerRightClickEvent} after giving the player a GPS network above the complexity
 * threshold.
 * <p>
 * The scan ends in a results menu being opened, so an {@link InventoryOpenEvent} tracker is
 * the observable signal: a cancelled scan opens nothing, the no-listener path opens the menu
 * as before.
 *
 * @author Zurker
 */
class TestGEOScanEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static GEOScanner scanner;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "geo_scan_test");

        SlimefunItemStack scannerStack = new SlimefunItemStack("_TEST_GEO_SCANNER", Material.OBSERVER, "&eTest GEO Scanner");
        Slimefun.getItemCfg().setValue("_TEST_GEO_SCANNER.enabled", true);
        scanner = new GEOScanner(itemGroup, scannerStack, RecipeType.NULL, new ItemStack[9]);
        scanner.register(plugin);

        // A single transmitter with a multiplier above the complexity threshold of 600
        SlimefunItemStack transmitterStack = new SlimefunItemStack("_TEST_GPS_TRANSMITTER", Material.IRON_BLOCK, "&7Test GPS Transmitter");
        Slimefun.getItemCfg().setValue("_TEST_GPS_TRANSMITTER.enabled", true);
        new TestTransmitter(itemGroup, transmitterStack).register(plugin);
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
     * Puts a BlockStorage-backed transmitter online for the player, lifting their network
     * complexity above the scan threshold.
     */
    private void giveNetwork(Player player) {
        Block transmitter = world.getBlockAt(100, 64, 100);
        transmitter.setType(Material.IRON_BLOCK);
        BlockStorage.addBlockInfo(transmitter, "id", "_TEST_GPS_TRANSMITTER");

        Slimefun.getGPSNetwork().updateTransmitter(transmitter.getLocation(), player.getUniqueId(), true);
    }

    /**
     * Right-clicks the block with the GEO scanner via the real block use handler.
     */
    private void scan(Player player, int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.STONE);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, scanner.getItem().clone(), b, BlockFace.UP);
        scanner.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));
    }

    /**
     * Tracks whether a results menu was opened for the player.
     */
    private boolean[] trackMenuOpens(Player player) {
        boolean[] opened = { false };
        Listener tracker = new Listener() {
            @EventHandler
            public void onOpen(InventoryOpenEvent event) {
                if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    opened[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(tracker, plugin);
        return opened;
    }

    @Test
    @DisplayName("GEOScanEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(1, 1, 1);

        GEOScanEvent event = new GEOScanEvent(player, b, 0);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(0, event.getPage());
        Assertions.assertFalse(event.isCancelled());

        event.setPage(2);
        Assertions.assertEquals(2, event.getPage());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOScanEvent(player, null, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOScanEvent(player, b, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setPage(-1));
    }

    @Test
    @DisplayName("Scanning fires the event and opens the results menu")
    void testScanFiresEventAndOpensMenu() {
        Player player = server.addPlayer();
        giveNetwork(player);
        boolean[] opened = trackMenuOpens(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onScan(GEOScanEvent event) {
                seen[0] = true;
                Assertions.assertEquals(world.getBlockAt(10, 1, 10), event.getBlock());
                Assertions.assertEquals(0, event.getPage());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            scan(player, 10, 10);

            Assertions.assertTrue(seen[0], "GEOScanEvent was not fired");
            Assertions.assertTrue(opened[0], "The results menu must have been opened");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling GEOScanEvent opens no results menu")
    void testCancelSkipsMenu() {
        Player player = server.addPlayer();
        giveNetwork(player);
        boolean[] opened = trackMenuOpens(player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onScan(GEOScanEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            scan(player, 20, 20);

            Assertions.assertFalse(opened[0], "A cancelled scan must not open the results menu");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Redirecting via setPage changes the displayed page")
    void testSetPageRedirect() {
        Player player = server.addPlayer();
        giveNetwork(player);

        boolean[] seen = { false };
        Listener redirecting = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onScan(GEOScanEvent event) {
                event.setPage(1);
            }
        };
        Listener checking = new Listener() {
            @EventHandler(priority = EventPriority.NORMAL)
            public void onScan(GEOScanEvent event) {
                seen[0] = true;
                Assertions.assertEquals(1, event.getPage(), "The redirected page must be visible to later listeners");
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);
        server.getPluginManager().registerEvents(checking, plugin);

        try {
            scan(player, 30, 30);

            Assertions.assertTrue(seen[0], "GEOScanEvent was not fired");
        } finally {
            HandlerList.unregisterAll(redirecting);
            HandlerList.unregisterAll(checking);
        }
    }

    @Test
    @DisplayName("Scanning below the complexity threshold fires no event")
    void testInsufficientComplexityFiresNothing() {
        Player player = server.addPlayer();
        boolean[] opened = trackMenuOpens(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onScan(GEOScanEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            scan(player, 40, 40);

            Assertions.assertFalse(seen[0], "No event must be fired below the complexity threshold");
            Assertions.assertFalse(opened[0], "No menu must be opened below the complexity threshold");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Scanning without listeners still opens the menu, preserving the old behavior")
    void testScanWithoutListenersStillOpensMenu() {
        Player player = server.addPlayer();
        giveNetwork(player);
        boolean[] opened = trackMenuOpens(player);

        scan(player, 50, 50);

        Assertions.assertTrue(opened[0], "The results menu must have been opened");
    }

    /**
     * Minimal {@link GPSTransmitter} with a fixed multiplier above the scan threshold.
     */
    private static class TestTransmitter extends GPSTransmitter {

        TestTransmitter(ItemGroup itemGroup, SlimefunItemStack item) {
            super(itemGroup, 1, item, RecipeType.NULL, new ItemStack[9]);
        }

        @Override
        public int getMultiplier(int y) {
            return 700;
        }

        @Override
        public int getEnergyConsumption() {
            return 8;
        }
    }
}

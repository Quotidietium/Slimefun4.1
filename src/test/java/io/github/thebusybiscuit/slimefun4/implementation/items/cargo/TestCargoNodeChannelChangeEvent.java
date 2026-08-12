package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeChannelChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the cargo API expansion: {@link CargoNodeChannelChangeEvent},
 * exercised by driving the real {@link AbstractCargoNode#applyChannelChange} channel
 * application path that the node GUIs' channel selectors delegate to.
 * <p>
 * The channel selector clicks cannot be simulated under MockBukkit, so the tests drive
 * the extracted application method directly (the wrap-around arithmetic stays in the
 * click handlers; the final target channel is passed in). The outcome is asserted
 * end-to-end through the stored BlockStorage frequency. The node is placed without
 * a menu update so no menu is built: building one would need player head textures,
 * which MockBukkit does not provide.
 *
 * @author Zurker
 */
class TestCargoNodeChannelChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static CargoInputNode node;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "cargo_channel_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CARGO_INPUT_NODE", Material.BARREL, "&fTest Cargo Input Node");
        Slimefun.getItemCfg().setValue("_TEST_CARGO_INPUT_NODE.enabled", true);
        node = new CargoInputNode(itemGroup, stack, RecipeType.NULL, new ItemStack[9], null);
        node.register(plugin);
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
     * Places a node block backed by {@link BlockStorage} with the given frequency
     * (or none at all when negative). The info is written without a menu update:
     * no menu is needed for the channel application path and building one would
     * require head textures MockBukkit does not implement.
     */
    private Block placeNode(int x, int z, int frequency) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.BARREL);
        BlockStorage.addBlockInfo(b, "id", node.getId());
        BlockStorage.addBlockInfo(b, "owner", "00000000-0000-0000-0000-000000000000");

        if (frequency >= 0) {
            BlockStorage.addBlockInfo(b, AbstractCargoNode.FREQUENCY, String.valueOf(frequency));
        }

        return b;
    }

    private String storedFrequency(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), AbstractCargoNode.FREQUENCY);
    }

    @Test
    @DisplayName("CargoNodeChannelChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeNode(1, 1, 3);

        CargoNodeChannelChangeEvent event = new CargoNodeChannelChangeEvent(player, node, b, 3, 4);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(node, event.getCargoNode());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(3, event.getPreviousChannel());
        Assertions.assertEquals(4, event.getNewChannel());
        Assertions.assertFalse(event.isCancelled());

        event.setNewChannel(12);
        Assertions.assertEquals(12, event.getNewChannel());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeChannelChangeEvent(player, null, b, 3, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeChannelChangeEvent(player, node, null, 3, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeChannelChangeEvent(player, node, b, -1, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeChannelChangeEvent(player, node, b, 17, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeChannelChangeEvent(player, node, b, 3, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeChannelChangeEvent(player, node, b, 3, 16));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNewChannel(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNewChannel(16));
    }

    @Test
    @DisplayName("Changing the channel fires the event and stores the new frequency")
    void testChangeFiresEventAndStoresChannel() {
        Player player = server.addPlayer();
        Block b = placeNode(10, 10, 3);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChannelChange(CargoNodeChannelChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(node, event.getCargoNode());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(3, event.getPreviousChannel());
                Assertions.assertEquals(4, event.getNewChannel());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = node.applyChannelChange(player, b, 4);

            Assertions.assertTrue(applied, "The change must have been applied");
            Assertions.assertTrue(seen[0], "CargoNodeChannelChangeEvent was not fired");
            Assertions.assertEquals("4", storedFrequency(b), "The stored frequency must have been updated");
            Assertions.assertEquals(4, node.getSelectedChannel(b), "The selected channel must read back as 4");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Decreasing channel 0 wraps to channel 15 (the click handler's wrap-around target)")
    void testWrapAroundTargetStored() {
        Player player = server.addPlayer();
        Block b = placeNode(20, 20, 0);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChannelChange(CargoNodeChannelChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(0, event.getPreviousChannel());
                Assertions.assertEquals(15, event.getNewChannel());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // The wrap-around arithmetic itself lives in the click handler; its final target is what arrives here
            boolean applied = node.applyChannelChange(player, b, 15);

            Assertions.assertTrue(applied);
            Assertions.assertTrue(seen[0], "CargoNodeChannelChangeEvent was not fired");
            Assertions.assertEquals("15", storedFrequency(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling CargoNodeChannelChangeEvent keeps the stored frequency")
    void testCancelKeepsFrequency() {
        Player player = server.addPlayer();
        Block b = placeNode(30, 30, 3);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onChannelChange(CargoNodeChannelChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean applied = node.applyChannelChange(player, b, 7);

            Assertions.assertFalse(applied, "A vetoed change must not be applied");
            Assertions.assertEquals("3", storedFrequency(b), "A vetoed change must keep the old frequency");
            Assertions.assertEquals(3, node.getSelectedChannel(b));
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Redirecting the change via setNewChannel stores the redirected channel")
    void testSetNewChannelRedirectsStored() {
        Player player = server.addPlayer();
        Block b = placeNode(35, 35, 3);

        Listener redirecting = new Listener() {
            @EventHandler
            public void onChannelChange(CargoNodeChannelChangeEvent event) {
                Assertions.assertEquals(7, event.getNewChannel(), "The new channel must default to the picked channel");
                event.setNewChannel(12);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            boolean applied = node.applyChannelChange(player, b, 7);

            Assertions.assertTrue(applied, "The change must have been applied");
            Assertions.assertEquals("12", storedFrequency(b), "The redirected channel must have been stored");
            Assertions.assertEquals(12, node.getSelectedChannel(b), "The selected channel must read back as 12");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Changing the channel without listeners still stores it, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeNode(40, 40, 3);

        boolean applied = node.applyChannelChange(player, b, 9);

        Assertions.assertTrue(applied);
        Assertions.assertEquals("9", storedFrequency(b), "The stored frequency must have been updated");
    }

    @Test
    @DisplayName("A node on the chest-terminal channel 16 reports it as the previous channel")
    void testPreviousChannelSixteen() {
        Player player = server.addPlayer();
        Block b = placeNode(50, 50, 16);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChannelChange(CargoNodeChannelChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(16, event.getPreviousChannel(), "The chest-terminal display state must be reported as channel 16");
                Assertions.assertEquals(15, event.getNewChannel());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = node.applyChannelChange(player, b, 15);

            Assertions.assertTrue(applied);
            Assertions.assertTrue(seen[0], "CargoNodeChannelChangeEvent was not fired");
            Assertions.assertEquals("15", storedFrequency(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A node without a stored frequency defaults the previous channel to 0")
    void testMissingFrequencyDefaultsToZero() {
        Player player = server.addPlayer();
        Block b = placeNode(60, 60, -1);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChannelChange(CargoNodeChannelChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(0, event.getPreviousChannel(), "A missing frequency must read as channel 0");
                Assertions.assertEquals(1, event.getNewChannel());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = node.applyChannelChange(player, b, 1);

            Assertions.assertTrue(applied);
            Assertions.assertTrue(seen[0], "CargoNodeChannelChangeEvent was not fired");
            Assertions.assertEquals("1", storedFrequency(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

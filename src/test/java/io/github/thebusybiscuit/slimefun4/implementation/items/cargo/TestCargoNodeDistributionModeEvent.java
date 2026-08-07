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

import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeDistributionModeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the cargo distribution API expansion:
 * {@link CargoNodeDistributionModeEvent}, exercised by driving the real
 * {@link CargoInputNode#applyDistributionChange} mode application path that the
 * round-robin and smart-fill toggles delegate to.
 * <p>
 * The toggle clicks cannot be simulated under MockBukkit, so the tests drive the
 * extracted application method directly. The outcome is asserted end-to-end through
 * the mode stored in {@link BlockStorage}.
 *
 * @author Zurker
 */
class TestCargoNodeDistributionModeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static CargoInputNode node;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "cargo_distribution_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CARGO_DISTRIBUTION_NODE", Material.BARREL, "&fTest Cargo Distribution Node");
        Slimefun.getItemCfg().setValue("_TEST_CARGO_DISTRIBUTION_NODE.enabled", true);
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

    private Block placeNode(int x, int z, String key, boolean enabled) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.BARREL);
        BlockStorage.addBlockInfo(b, "id", node.getId());
        BlockStorage.addBlockInfo(b, "owner", "00000000-0000-0000-0000-000000000000");

        if (key != null) {
            BlockStorage.addBlockInfo(b, key, String.valueOf(enabled));
        }

        return b;
    }

    private boolean storedEnabled(Block b, String key) {
        String value = BlockStorage.getLocationInfo(b.getLocation(), key);
        return value != null && value.equals(String.valueOf(true));
    }

    @Test
    @DisplayName("CargoNodeDistributionModeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeNode(1, 1, "round-robin", false);

        CargoNodeDistributionModeEvent event = new CargoNodeDistributionModeEvent(player, node, b, CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, false, true);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(node, event.getCargoNode());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, event.getReason());
        Assertions.assertFalse(event.getPreviousValue());
        Assertions.assertTrue(event.getNewValue());
        Assertions.assertFalse(event.isCancelled());

        event.setNewValue(false);
        Assertions.assertFalse(event.getNewValue());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeDistributionModeEvent(player, null, b, CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, false, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeDistributionModeEvent(player, node, null, CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, false, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeDistributionModeEvent(player, node, b, null, false, true));
    }

    @Test
    @DisplayName("Toggling round-robin fires the event and stores the new value")
    void testRoundRobinChangeFiresAndStores() {
        Player player = server.addPlayer();
        Block b = placeNode(10, 10, "round-robin", false);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onModeChange(CargoNodeDistributionModeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(node, event.getCargoNode());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, event.getReason());
                Assertions.assertFalse(event.getPreviousValue());
                Assertions.assertTrue(event.getNewValue());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = node.applyDistributionChange(player, b, "round-robin", CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, true);

            Assertions.assertTrue(applied, "The change must have been applied");
            Assertions.assertTrue(seen[0], "CargoNodeDistributionModeEvent was not fired");
            Assertions.assertTrue(storedEnabled(b, "round-robin"), "The stored mode must have switched to enabled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Toggling smart-fill fires the event and stores the new value")
    void testSmartFillChangeFiresAndStores() {
        Player player = server.addPlayer();
        Block b = placeNode(20, 20, "smart-fill", false);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onModeChange(CargoNodeDistributionModeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(CargoNodeDistributionModeEvent.Reason.SMART_FILL, event.getReason());
                Assertions.assertFalse(event.getPreviousValue());
                Assertions.assertTrue(event.getNewValue());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = node.applyDistributionChange(player, b, "smart-fill", CargoNodeDistributionModeEvent.Reason.SMART_FILL, true);

            Assertions.assertTrue(applied);
            Assertions.assertTrue(seen[0], "CargoNodeDistributionModeEvent was not fired");
            Assertions.assertTrue(storedEnabled(b, "smart-fill"), "The stored mode must have switched to enabled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling CargoNodeDistributionModeEvent keeps the stored value")
    void testCancelKeepsStoredValue() {
        Player player = server.addPlayer();
        Block b = placeNode(30, 30, "round-robin", false);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onModeChange(CargoNodeDistributionModeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean applied = node.applyDistributionChange(player, b, "round-robin", CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, true);

            Assertions.assertFalse(applied, "A vetoed change must not be applied");
            Assertions.assertFalse(storedEnabled(b, "round-robin"), "A vetoed change must keep the old value");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the value via setNewValue stores the override")
    void testSetNewValueOverride() {
        Player player = server.addPlayer();
        Block b = placeNode(40, 40, "smart-fill", true);

        Listener overriding = new Listener() {
            @EventHandler
            public void onModeChange(CargoNodeDistributionModeEvent event) {
                // Force it back to disabled even though the toggle wanted enabled
                event.setNewValue(false);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            boolean applied = node.applyDistributionChange(player, b, "smart-fill", CargoNodeDistributionModeEvent.Reason.SMART_FILL, true);

            Assertions.assertTrue(applied);
            Assertions.assertFalse(storedEnabled(b, "smart-fill"), "The overridden value must have been stored");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Changing the mode without listeners still stores it, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeNode(50, 50, "round-robin", false);

        boolean applied = node.applyDistributionChange(player, b, "round-robin", CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, true);

        Assertions.assertTrue(applied);
        Assertions.assertTrue(storedEnabled(b, "round-robin"), "The stored mode must have been updated");
    }

    @Test
    @DisplayName("A node without a stored mode reports the default previous value (disabled)")
    void testMissingModeDefaultsDisabled() {
        Player player = server.addPlayer();
        Block b = placeNode(60, 60, null, false);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onModeChange(CargoNodeDistributionModeEvent event) {
                seen[0] = true;
                Assertions.assertFalse(event.getPreviousValue(), "A missing mode must default to disabled (false)");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            node.applyDistributionChange(player, b, "round-robin", CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, true);
            Assertions.assertTrue(seen[0], "CargoNodeDistributionModeEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

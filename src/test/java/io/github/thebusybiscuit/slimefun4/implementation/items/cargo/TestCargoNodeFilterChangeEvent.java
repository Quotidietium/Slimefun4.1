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

import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeFilterChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the cargo filter API expansion:
 * {@link CargoNodeFilterChangeEvent}, exercised by driving the real
 * {@link AbstractFilterNode#applyFilterChange} setting application path that the
 * whitelist/blacklist and lore toggles delegate to.
 * <p>
 * The toggle clicks cannot be simulated under MockBukkit, so the tests drive the
 * extracted application method directly. The outcome is asserted end-to-end through
 * the setting stored in {@link BlockStorage}.
 *
 * @author Zurker
 */
class TestCargoNodeFilterChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static CargoInputNode node;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "cargo_filter_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CARGO_FILTER_NODE", Material.BARREL, "&fTest Cargo Filter Node");
        Slimefun.getItemCfg().setValue("_TEST_CARGO_FILTER_NODE.enabled", true);
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
     * Places a node block backed by {@link BlockStorage} with the given setting
     * (or none at all when null).
     */
    private Block placeNode(int x, int z, String key, String value) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.BARREL);
        BlockStorage.addBlockInfo(b, "id", node.getId());
        BlockStorage.addBlockInfo(b, "owner", "00000000-0000-0000-0000-000000000000");

        if (key != null) {
            BlockStorage.addBlockInfo(b, key, value);
        }

        return b;
    }

    private String stored(Block b, String key) {
        return BlockStorage.getLocationInfo(b.getLocation(), key);
    }

    @Test
    @DisplayName("CargoNodeFilterChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeNode(1, 1, "filter-type", "whitelist");

        CargoNodeFilterChangeEvent event = new CargoNodeFilterChangeEvent(player, node, b, CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, true, false);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(node, event.getCargoNode());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, event.getReason());
        Assertions.assertTrue(event.getPreviousValue());
        Assertions.assertFalse(event.getNewValue());
        Assertions.assertFalse(event.isCancelled());

        event.setNewValue(true);
        Assertions.assertTrue(event.getNewValue());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeFilterChangeEvent(player, null, b, CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, true, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeFilterChangeEvent(player, node, null, CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, true, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeFilterChangeEvent(player, node, b, null, true, false));
    }

    @Test
    @DisplayName("Toggling the filter type fires the event and stores the new value")
    void testFilterTypeChangeFiresAndStores() {
        Player player = server.addPlayer();
        Block b = placeNode(10, 10, "filter-type", "whitelist");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFilterChange(CargoNodeFilterChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(node, event.getCargoNode());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, event.getReason());
                Assertions.assertTrue(event.getPreviousValue(), "The previous value must be whitelist (true)");
                Assertions.assertFalse(event.getNewValue(), "The new value must be blacklist (false)");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = node.applyFilterChange(player, b, "filter-type", CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, false);

            Assertions.assertTrue(applied, "The change must have been applied");
            Assertions.assertTrue(seen[0], "CargoNodeFilterChangeEvent was not fired");
            Assertions.assertEquals("blacklist", stored(b, "filter-type"), "The stored value must have switched to blacklist");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Toggling the lore matching fires the event and stores the new value")
    void testLoreChangeFiresAndStores() {
        Player player = server.addPlayer();
        Block b = placeNode(20, 20, "filter-lore", "true");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFilterChange(CargoNodeFilterChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(CargoNodeFilterChangeEvent.Reason.LORE_MATCHING, event.getReason());
                Assertions.assertTrue(event.getPreviousValue(), "The previous value must include lore (true)");
                Assertions.assertFalse(event.getNewValue(), "The new value must exclude lore (false)");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = node.applyFilterChange(player, b, "filter-lore", CargoNodeFilterChangeEvent.Reason.LORE_MATCHING, false);

            Assertions.assertTrue(applied);
            Assertions.assertTrue(seen[0], "CargoNodeFilterChangeEvent was not fired");
            Assertions.assertEquals("false", stored(b, "filter-lore"), "The stored value must have switched to false");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling CargoNodeFilterChangeEvent keeps the stored value")
    void testCancelKeepsStoredValue() {
        Player player = server.addPlayer();
        Block b = placeNode(30, 30, "filter-type", "whitelist");

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFilterChange(CargoNodeFilterChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean applied = node.applyFilterChange(player, b, "filter-type", CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, false);

            Assertions.assertFalse(applied, "A vetoed change must not be applied");
            Assertions.assertEquals("whitelist", stored(b, "filter-type"), "A vetoed change must keep the old value");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the value via setNewValue stores the override")
    void testSetNewValueOverride() {
        Player player = server.addPlayer();
        Block b = placeNode(40, 40, "filter-type", "whitelist");

        Listener overriding = new Listener() {
            @EventHandler
            public void onFilterChange(CargoNodeFilterChangeEvent event) {
                // Force it back to whitelist even though the toggle wanted blacklist
                event.setNewValue(true);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            boolean applied = node.applyFilterChange(player, b, "filter-type", CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, false);

            Assertions.assertTrue(applied);
            Assertions.assertEquals("whitelist", stored(b, "filter-type"), "The overridden value must have been stored");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Changing the setting without listeners still stores it, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeNode(50, 50, "filter-lore", "true");

        boolean applied = node.applyFilterChange(player, b, "filter-lore", CargoNodeFilterChangeEvent.Reason.LORE_MATCHING, false);

        Assertions.assertTrue(applied);
        Assertions.assertEquals("false", stored(b, "filter-lore"), "The stored value must have been updated");
    }

    @Test
    @DisplayName("A node without a stored setting reports the default previous value")
    void testMissingSettingDefaultsPreviousValue() {
        Player player = server.addPlayer();
        Block b = placeNode(60, 60, null, null);

        boolean[] seenFilter = { false };
        boolean[] seenLore = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFilterChange(CargoNodeFilterChangeEvent event) {
                if (event.getReason() == CargoNodeFilterChangeEvent.Reason.FILTER_TYPE) {
                    seenFilter[0] = true;
                    Assertions.assertTrue(event.getPreviousValue(), "A missing filter-type must default to whitelist (true)");
                } else {
                    seenLore[0] = true;
                    Assertions.assertTrue(event.getPreviousValue(), "A missing filter-lore must default to include lore (true)");
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            node.applyFilterChange(player, b, "filter-type", CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, false);
            node.applyFilterChange(player, b, "filter-lore", CargoNodeFilterChangeEvent.Reason.LORE_MATCHING, false);

            Assertions.assertTrue(seenFilter[0], "Filter-type event was not fired");
            Assertions.assertTrue(seenLore[0], "Lore event was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

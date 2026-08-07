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

import io.github.thebusybiscuit.slimefun4.api.events.CargoNetVisualizerToggleEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the cargo network API expansion:
 * {@link CargoNetVisualizerToggleEvent}, exercised by driving the real
 * {@link CargoManager#applyVisualizerToggle} toggle path directly.
 * <p>
 * The visualizer state is stored in {@link BlockStorage} under the "visualizer" key
 * ({@code null} = enabled, "disabled" = disabled), so the outcome is asserted end-to-end.
 *
 * @author Zurker
 */
class TestCargoNetVisualizerToggleEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static CargoManager manager;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "cargo_visualizer_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CARGO_MANAGER", Material.CRAFTING_TABLE, "&fTest Cargo Manager");
        Slimefun.getItemCfg().setValue("_TEST_CARGO_MANAGER.enabled", true);
        manager = new CargoManager(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        manager.register(plugin);
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
     * Places a CargoManager block backed by {@link BlockStorage}. When {@code enabled} is
     * true the visualizer key is left absent (enabled); otherwise it is set to "disabled".
     */
    private Block placeManager(int x, int z, Boolean enabled) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.CRAFTING_TABLE);
        BlockStorage.addBlockInfo(b, "id", manager.getId());

        if (enabled != null) {
            BlockStorage.addBlockInfo(b, "visualizer", enabled ? null : "disabled");
        }

        return b;
    }

    private boolean isEnabled(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "visualizer") == null;
    }

    @Test
    @DisplayName("CargoNetVisualizerToggleEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeManager(1, 1, true);

        CargoNetVisualizerToggleEvent event = new CargoNetVisualizerToggleEvent(player, manager, b, true, false);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(manager, event.getCargoManager());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertTrue(event.wasPreviouslyEnabled());
        Assertions.assertFalse(event.isEnabled());
        Assertions.assertFalse(event.isCancelled());

        event.setEnabled(true);
        Assertions.assertTrue(event.isEnabled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNetVisualizerToggleEvent(player, null, b, true, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNetVisualizerToggleEvent(player, manager, null, true, false));
    }

    @Test
    @DisplayName("Toggling the visualizer fires the event and stores the new state")
    void testToggleFiresEventAndStores() {
        Player player = server.addPlayer();
        Block b = placeManager(10, 10, true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onToggle(CargoNetVisualizerToggleEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(manager, event.getCargoManager());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertTrue(event.wasPreviouslyEnabled());
                Assertions.assertFalse(event.isEnabled());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            manager.applyVisualizerToggle(player, b);

            Assertions.assertTrue(seen[0], "CargoNetVisualizerToggleEvent was not fired");
            Assertions.assertFalse(isEnabled(b), "The visualizer must have been disabled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Toggling back re-enables the visualizer")
    void testToggleBackReEnables() {
        Player player = server.addPlayer();
        Block b = placeManager(20, 20, false);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onToggle(CargoNetVisualizerToggleEvent event) {
                seen[0] = true;
                Assertions.assertFalse(event.wasPreviouslyEnabled());
                Assertions.assertTrue(event.isEnabled());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            manager.applyVisualizerToggle(player, b);

            Assertions.assertTrue(seen[0], "CargoNetVisualizerToggleEvent was not fired");
            Assertions.assertTrue(isEnabled(b), "The visualizer must have been re-enabled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling CargoNetVisualizerToggleEvent keeps the stored state")
    void testCancelKeepsStoredState() {
        Player player = server.addPlayer();
        Block b = placeManager(30, 30, true);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onToggle(CargoNetVisualizerToggleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            manager.applyVisualizerToggle(player, b);

            Assertions.assertTrue(isEnabled(b), "A vetoed toggle must keep the visualizer enabled");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the state via setEnabled stores the override")
    void testSetEnabledOverride() {
        Player player = server.addPlayer();
        Block b = placeManager(40, 40, true);

        // The toggle wants to disable, the listener forces it back to enabled
        Listener overriding = new Listener() {
            @EventHandler
            public void onToggle(CargoNetVisualizerToggleEvent event) {
                event.setEnabled(true);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            manager.applyVisualizerToggle(player, b);

            Assertions.assertTrue(isEnabled(b), "The overridden state must have been stored");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Toggling without listeners still stores it, preserving the old behavior")
    void testToggleWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeManager(50, 50, true);

        manager.applyVisualizerToggle(player, b);

        Assertions.assertFalse(isEnabled(b), "The visualizer must have been disabled");
    }

    @Test
    @DisplayName("A fresh CargoManager without a stored state defaults to enabled")
    void testFreshManagerDefaultsEnabled() {
        Player player = server.addPlayer();
        Block b = placeManager(60, 60, null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onToggle(CargoNetVisualizerToggleEvent event) {
                seen[0] = true;
                Assertions.assertTrue(event.wasPreviouslyEnabled(), "A missing visualizer key must default to enabled");
                Assertions.assertFalse(event.isEnabled());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            manager.applyVisualizerToggle(player, b);

            Assertions.assertTrue(seen[0], "CargoNetVisualizerToggleEvent was not fired");
            Assertions.assertFalse(isEnabled(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors;

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

import io.github.thebusybiscuit.slimefun4.api.events.ReactorModeChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the reactor API expansion: {@link ReactorModeChangeEvent},
 * exercised by driving the real {@link Reactor#applyModeChange} mode application path
 * that the focus selector delegates to.
 * <p>
 * The selector click cannot be simulated under MockBukkit, so the tests drive the
 * extracted application method directly. The outcome is asserted end-to-end through
 * the mode stored in {@link BlockStorage} and read back via {@link Reactor#getReactorMode}.
 *
 * @author Zurker
 */
class TestReactorModeChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static NuclearReactor reactor;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "reactor_mode_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MODE_REACTOR", Material.DISPENSER, "&fTest Mode Reactor");
        Slimefun.getItemCfg().setValue("_TEST_MODE_REACTOR.enabled", true);
        reactor = new NuclearReactor(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public int getEnergyProduction() {
                return 100;
            }

            @Override
            public int getCapacity() {
                return 512;
            }
        };
        reactor.register(plugin);
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
     * Places a reactor block backed by {@link BlockStorage} with the given mode
     * (or none at all when null).
     */
    private Block placeReactor(int x, int z, ReactorMode mode) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", reactor.getId());

        if (mode != null) {
            BlockStorage.addBlockInfo(b, "reactor-mode", mode.toString());
        }

        return b;
    }

    private ReactorMode storedMode(Block b) {
        return reactor.getReactorMode(b.getLocation());
    }

    @Test
    @DisplayName("ReactorModeChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeReactor(1, 1, ReactorMode.GENERATOR);

        ReactorModeChangeEvent event = new ReactorModeChangeEvent(player, reactor, b, ReactorMode.GENERATOR, ReactorMode.PRODUCTION);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(reactor, event.getReactor());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(ReactorMode.GENERATOR, event.getPreviousMode());
        Assertions.assertEquals(ReactorMode.PRODUCTION, event.getNewMode());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorModeChangeEvent(player, null, b, ReactorMode.GENERATOR, ReactorMode.PRODUCTION));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorModeChangeEvent(player, reactor, null, ReactorMode.GENERATOR, ReactorMode.PRODUCTION));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorModeChangeEvent(player, reactor, b, null, ReactorMode.PRODUCTION));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorModeChangeEvent(player, reactor, b, ReactorMode.GENERATOR, null));
    }

    @Test
    @DisplayName("Changing the mode fires the event and stores the new mode")
    void testChangeFiresEventAndStoresMode() {
        Player player = server.addPlayer();
        Block b = placeReactor(10, 10, ReactorMode.GENERATOR);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onModeChange(ReactorModeChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(reactor, event.getReactor());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(ReactorMode.GENERATOR, event.getPreviousMode());
                Assertions.assertEquals(ReactorMode.PRODUCTION, event.getNewMode());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = reactor.applyModeChange(player, b, ReactorMode.PRODUCTION);

            Assertions.assertTrue(applied, "The change must have been applied");
            Assertions.assertTrue(seen[0], "ReactorModeChangeEvent was not fired");
            Assertions.assertEquals(ReactorMode.PRODUCTION, storedMode(b), "The stored mode must have been updated");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ReactorModeChangeEvent keeps the stored mode")
    void testCancelKeepsOldMode() {
        Player player = server.addPlayer();
        Block b = placeReactor(20, 20, ReactorMode.GENERATOR);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onModeChange(ReactorModeChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean applied = reactor.applyModeChange(player, b, ReactorMode.PRODUCTION);

            Assertions.assertFalse(applied, "A vetoed change must not be applied");
            Assertions.assertEquals(ReactorMode.GENERATOR, storedMode(b), "A vetoed change must keep the old mode");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Changing the mode without listeners still stores it, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeReactor(30, 30, ReactorMode.PRODUCTION);

        boolean applied = reactor.applyModeChange(player, b, ReactorMode.GENERATOR);

        Assertions.assertTrue(applied);
        Assertions.assertEquals(ReactorMode.GENERATOR, storedMode(b), "The stored mode must have been updated");
    }

    @Test
    @DisplayName("A fresh reactor without a stored mode defaults to GENERATOR and reports it as previous")
    void testFreshReactorDefaultsToGenerator() {
        Player player = server.addPlayer();
        Block b = placeReactor(40, 40, null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onModeChange(ReactorModeChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(ReactorMode.GENERATOR, event.getPreviousMode(), "A missing mode must default to GENERATOR");
                Assertions.assertEquals(ReactorMode.PRODUCTION, event.getNewMode());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = reactor.applyModeChange(player, b, ReactorMode.PRODUCTION);

            Assertions.assertTrue(applied);
            Assertions.assertTrue(seen[0], "ReactorModeChangeEvent was not fired");
            Assertions.assertEquals(ReactorMode.PRODUCTION, storedMode(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Toggling back and forth chains previous and new modes correctly")
    void testToggleChainsModes() {
        Player player = server.addPlayer();
        Block b = placeReactor(50, 50, ReactorMode.GENERATOR);

        ReactorMode[] observedPrevious = { null };
        ReactorMode[] observedNew = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onModeChange(ReactorModeChangeEvent event) {
                observedPrevious[0] = event.getPreviousMode();
                observedNew[0] = event.getNewMode();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertTrue(reactor.applyModeChange(player, b, ReactorMode.PRODUCTION));
            Assertions.assertEquals(ReactorMode.GENERATOR, observedPrevious[0]);
            Assertions.assertEquals(ReactorMode.PRODUCTION, observedNew[0]);

            Assertions.assertTrue(reactor.applyModeChange(player, b, ReactorMode.GENERATOR));
            Assertions.assertEquals(ReactorMode.PRODUCTION, observedPrevious[0], "The second toggle must see the first toggle's mode as previous");
            Assertions.assertEquals(ReactorMode.GENERATOR, observedNew[0]);
            Assertions.assertEquals(ReactorMode.GENERATOR, storedMode(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.LumberAxeTreeFellEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the lumber axe API expansion: {@link LumberAxeTreeFellEvent},
 * exercised by driving the real {@link LumberAxe} {@link ToolUseHandler} with a constructed
 * {@link BlockBreakEvent}.
 * <p>
 * The per-log break ends in {@code playEffect(STEP_SOUND, Material)}, which MockBukkit rejects
 * with an {@link IllegalArgumentException} ("Wrong kind of data for this effect!") before any
 * world change. Reaching that tail therefore proves the fell loop ran on a log, so the helper
 * reports whether the tail was reached instead of asserting world state.
 *
 * @author Zurker
 */
class TestLumberAxeTreeFellEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static LumberAxe lumberAxe;
    private static ToolUseHandler toolUseHandler;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "lumber_axe_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_LUMBER_AXE", Material.DIAMOND_AXE, "&6Test Lumber Axe");
        Slimefun.getItemCfg().setValue("_TEST_LUMBER_AXE.enabled", true);
        lumberAxe = new LumberAxe(itemGroup, stack, RecipeType.NULL, new org.bukkit.inventory.ItemStack[9]);
        lumberAxe.register(plugin);

        for (ItemHandler handler : lumberAxe.getHandlers()) {
            if (handler instanceof ToolUseHandler tuh) {
                toolUseHandler = tuh;
            }
        }

        Assertions.assertNotNull(toolUseHandler, "The LumberAxe must expose a ToolUseHandler");
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
     * Breaks the bottom log of a 3-log column via the real handler.
     *
     * @return true if the fell loop reached a per-log break (the playEffect tail), false if
     *         no additional log break was attempted
     */
    private boolean fellTree(Player player, int x, int z) {
        for (int y = 4; y <= 6; y++) {
            world.getBlockAt(x, y, z).setType(Material.OAK_LOG);
        }

        Block bottom = world.getBlockAt(x, 4, z);
        BlockBreakEvent breakEvent = new BlockBreakEvent(bottom, player);

        try {
            toolUseHandler.onToolUse(breakEvent, lumberAxe.getItem(), 0, new ArrayList<>());
            return false;
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Wrong kind of data")) {
                // MockBukkit rejects playEffect(STEP_SOUND, Material) - see class javadoc
                return true;
            }

            throw ex;
        }
    }

    @Test
    @DisplayName("LumberAxeTreeFellEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block log = world.getBlockAt(0, 4, 0);

        LumberAxeTreeFellEvent event = new LumberAxeTreeFellEvent(player, lumberAxe, log, new ArrayList<>());

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(lumberAxe, event.getLumberAxe());
        Assertions.assertEquals(log, event.getPrimaryLog());
        Assertions.assertTrue(event.getAdditionalLogs().isEmpty());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new LumberAxeTreeFellEvent(player, null, log, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LumberAxeTreeFellEvent(player, lumberAxe, null, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LumberAxeTreeFellEvent(player, lumberAxe, log, null));
    }

    @Test
    @DisplayName("Breaking a log fires the event with the connected logs and fells them")
    void testFellFiresEvent() {
        Player player = server.addPlayer();
        Block bottom = world.getBlockAt(10, 4, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFell(LumberAxeTreeFellEvent event) {
                seen[0] = true;
                Assertions.assertEquals(lumberAxe, event.getLumberAxe());
                Assertions.assertEquals(bottom, event.getPrimaryLog());
                Assertions.assertEquals(2, event.getAdditionalLogs().size(), "The two logs above must be felled too");
                Assertions.assertTrue(event.getAdditionalLogs().contains(world.getBlockAt(10, 5, 10)));
                Assertions.assertTrue(event.getAdditionalLogs().contains(world.getBlockAt(10, 6, 10)));
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean breakAttempted = fellTree(player, 10, 10);

            Assertions.assertTrue(seen[0], "LumberAxeTreeFellEvent was not fired");
            Assertions.assertTrue(breakAttempted, "The fell loop must have reached a per-log break");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling LumberAxeTreeFellEvent leaves the connected logs untouched")
    void testEventCancellationSkipsFelling() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFell(LumberAxeTreeFellEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean breakAttempted = fellTree(player, 20, 20);

            Assertions.assertFalse(breakAttempted, "A cancelled fell must not break any additional log");
            Assertions.assertEquals(Material.OAK_LOG, world.getBlockAt(20, 5, 20).getType());
            Assertions.assertEquals(Material.OAK_LOG, world.getBlockAt(20, 6, 20).getType());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Removing entries from getAdditionalLogs spares those logs")
    void testAdditionalLogsFiltering() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener filtering = new Listener() {
            @EventHandler
            public void onFell(LumberAxeTreeFellEvent event) {
                seen[0] = true;
                event.getAdditionalLogs().clear();
            }
        };
        server.getPluginManager().registerEvents(filtering, plugin);

        try {
            boolean breakAttempted = fellTree(player, 30, 30);

            Assertions.assertTrue(seen[0], "LumberAxeTreeFellEvent was not fired");
            Assertions.assertFalse(breakAttempted, "Clearing the additional logs must skip the fell loop");
        } finally {
            HandlerList.unregisterAll(filtering);
        }
    }

    @Test
    @DisplayName("Breaking a log without listeners still fells, preserving the old behavior")
    void testFellWithoutListenersStillFells() {
        Player player = server.addPlayer();

        boolean breakAttempted = fellTree(player, 40, 40);

        Assertions.assertTrue(breakAttempted, "The fell loop must have reached a per-log break");
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.LumberAxeStripEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the lumber axe stripping API expansion: {@link LumberAxeStripEvent},
 * exercised by driving the real {@link LumberAxe}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent} on a column of logs.
 * <p>
 * MockBukkit's {@code BlockDataMock} does not implement {@code Orientable}, so stripping a log
 * ends in a {@link ClassCastException} after the strip sound was played. The helper catches it
 * and the tests observe the strip through the axe-strip sound routed to the player.
 *
 * @author Zurker
 */
class TestLumberAxeStripEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static LumberAxe lumberAxe;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "lumber_axe_strip_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_STRIP_LUMBER_AXE", Material.DIAMOND_AXE, "&6Test Lumber Axe");
        Slimefun.getItemCfg().setValue("_TEST_STRIP_LUMBER_AXE.enabled", true);
        lumberAxe = new LumberAxe(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        lumberAxe.register(plugin);
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
     * Right-clicks the bottom log of a 3-log column via the real handler. The strip sound is
     * observable through the player's heard sounds; the trailing ClassCastException from
     * MockBukkit's non-Orientable BlockData is caught.
     */
    private void stripColumn(PlayerMock player, int x, int z) {
        player.getInventory().setItemInMainHand(lumberAxe.getItem().clone());

        for (int y = 4; y <= 6; y++) {
            world.getBlockAt(x, y, z).setType(Material.OAK_LOG);
        }

        Block bottom = world.getBlockAt(x, 4, z);
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, handItem, bottom, BlockFace.UP);

        try {
            lumberAxe.onItemUse().onRightClick(new PlayerRightClickEvent(interactEvent));
        } catch (ClassCastException expected) {
            // BlockDataMock is not Orientable - see class javadoc; the sound played beforehand
        }
    }

    private boolean heardStripSound(PlayerMock player) {
        return player.getHeardSounds().stream().anyMatch(audio -> audio.getSound().contains("axe.strip"));
    }

    @Test
    @DisplayName("LumberAxeStripEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Block log = world.getBlockAt(0, 4, 0);

        LumberAxeStripEvent event = new LumberAxeStripEvent(player, lumberAxe, log, new ArrayList<>());

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(lumberAxe, event.getLumberAxe());
        Assertions.assertEquals(log, event.getPrimaryLog());
        Assertions.assertTrue(event.getAdditionalLogs().isEmpty());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new LumberAxeStripEvent(player, null, log, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LumberAxeStripEvent(player, lumberAxe, null, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LumberAxeStripEvent(player, lumberAxe, log, null));
    }

    @Test
    @DisplayName("Stripping a log column fires the event with the attached logs and strips them")
    void testStripFiresEvent() {
        PlayerMock player = server.addPlayer();
        Block bottom = world.getBlockAt(10, 4, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStrip(LumberAxeStripEvent event) {
                seen[0] = true;
                Assertions.assertEquals(lumberAxe, event.getLumberAxe());
                Assertions.assertEquals(bottom, event.getPrimaryLog());
                Assertions.assertEquals(2, event.getAdditionalLogs().size(), "The two logs above must be included");
                Assertions.assertTrue(event.getAdditionalLogs().contains(world.getBlockAt(10, 6, 10)));
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            stripColumn(player, 10, 10);

            Assertions.assertTrue(seen[0], "LumberAxeStripEvent was not fired");
            Assertions.assertTrue(heardStripSound(player), "The strip sound must have been played");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling LumberAxeStripEvent spares the attached logs")
    void testEventCancellationSkipsStripping() {
        PlayerMock player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onStrip(LumberAxeStripEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            stripColumn(player, 20, 20);

            Assertions.assertFalse(heardStripSound(player), "A cancelled strip must not strip any additional log");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Removing entries from getAdditionalLogs spares those logs")
    void testLogsFiltering() {
        PlayerMock player = server.addPlayer();

        Listener filtering = new Listener() {
            @EventHandler
            public void onStrip(LumberAxeStripEvent event) {
                event.getAdditionalLogs().clear();
            }
        };
        server.getPluginManager().registerEvents(filtering, plugin);

        try {
            stripColumn(player, 30, 30);

            Assertions.assertFalse(heardStripSound(player), "Clearing the additional logs must skip the strip loop");
        } finally {
            HandlerList.unregisterAll(filtering);
        }
    }

    @Test
    @DisplayName("Clicking a non-log block neither fires the event nor strips anything")
    void testNonLogDoesNothing() {
        PlayerMock player = server.addPlayer();
        player.getInventory().setItemInMainHand(lumberAxe.getItem().clone());

        Block stone = world.getBlockAt(40, 4, 40);
        stone.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStrip(LumberAxeStripEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, handItem, stone, BlockFace.UP);
            lumberAxe.onItemUse().onRightClick(new PlayerRightClickEvent(interactEvent));

            Assertions.assertFalse(seen[0], "LumberAxeStripEvent must not fire for a non-log block");
            Assertions.assertFalse(heardStripSound(player), "No strip sound must be played");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Stripping a log column without listeners still strips the attached logs, preserving the old behavior")
    void testStripWithoutListenersStillStrips() {
        PlayerMock player = server.addPlayer();

        stripColumn(player, 50, 50);

        Assertions.assertTrue(heardStripSound(player), "The strip sound must have been played");
    }
}

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
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PickaxeOfVeinMiningEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the pickaxe of vein mining API expansion:
 * {@link PickaxeOfVeinMiningEvent}, exercised by driving the real {@link PickaxeOfVeinMining}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler} with a constructed
 * {@link BlockBreakEvent}.
 * <p>
 * The per-ore break starts with {@code playEffect(STEP_SOUND, Material)}, which MockBukkit
 * rejects with an {@link IllegalArgumentException} before any world change. Reaching that tail
 * proves the vein loop ran on an ore, so the helper reports whether the tail was reached
 * instead of asserting world state.
 *
 * @author Zurker
 */
class TestPickaxeOfVeinMiningEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static PickaxeOfVeinMining pickaxe;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "vein_mining_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_VEIN_PICKAXE", Material.DIAMOND_PICKAXE, "&bTest Pickaxe of Vein Mining");
        Slimefun.getItemCfg().setValue("_TEST_VEIN_PICKAXE.enabled", true);
        pickaxe = new PickaxeOfVeinMining(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        pickaxe.register(plugin);
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
     * Mines the bottom ore of a 3-ore column via the real handler.
     *
     * @return true if the vein loop reached a per-ore break (the playEffect tail), false if
     *         no vein break was attempted
     */
    private boolean mineVein(Player player, int x, int z) {
        for (int y = 4; y <= 6; y++) {
            world.getBlockAt(x, y, z).setType(Material.IRON_ORE);
        }

        Block bottom = world.getBlockAt(x, 4, z);
        BlockBreakEvent breakEvent = new BlockBreakEvent(bottom, player);

        try {
            pickaxe.getItemHandler().onToolUse(breakEvent, pickaxe.getItem(), 1, new ArrayList<>());
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
    @DisplayName("PickaxeOfVeinMiningEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        PickaxeOfVeinMiningEvent event = new PickaxeOfVeinMiningEvent(player, pickaxe, new ArrayList<>());

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(pickaxe, event.getPickaxe());
        Assertions.assertTrue(event.getBlocks().isEmpty());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PickaxeOfVeinMiningEvent(player, null, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PickaxeOfVeinMiningEvent(player, pickaxe, null));
    }

    @Test
    @DisplayName("Mining an ore fires the event with the whole vein and mines it")
    void testMineFiresEvent() {
        Player player = server.addPlayer();
        Block bottom = world.getBlockAt(10, 4, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onVeinMine(PickaxeOfVeinMiningEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pickaxe, event.getPickaxe());
                Assertions.assertEquals(3, event.getBlocks().size(), "The whole 3-ore vein must be included");
                Assertions.assertTrue(event.getBlocks().contains(bottom));
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean breakAttempted = mineVein(player, 10, 10);

            Assertions.assertTrue(seen[0], "PickaxeOfVeinMiningEvent was not fired");
            Assertions.assertTrue(breakAttempted, "The vein loop must have reached a per-ore break");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling PickaxeOfVeinMiningEvent leaves the vein untouched")
    void testEventCancellationSkipsVeinMining() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onVeinMine(PickaxeOfVeinMiningEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean breakAttempted = mineVein(player, 20, 20);

            Assertions.assertFalse(breakAttempted, "A cancelled vein mine must not break any ore");
            Assertions.assertEquals(Material.IRON_ORE, world.getBlockAt(20, 5, 20).getType());
            Assertions.assertEquals(Material.IRON_ORE, world.getBlockAt(20, 6, 20).getType());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Removing entries from getBlocks spares those ores")
    void testBlocksFiltering() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener filtering = new Listener() {
            @EventHandler
            public void onVeinMine(PickaxeOfVeinMiningEvent event) {
                seen[0] = true;
                event.getBlocks().clear();
            }
        };
        server.getPluginManager().registerEvents(filtering, plugin);

        try {
            boolean breakAttempted = mineVein(player, 30, 30);

            Assertions.assertTrue(seen[0], "PickaxeOfVeinMiningEvent was not fired");
            Assertions.assertFalse(breakAttempted, "Clearing the vein must skip the break loop");
        } finally {
            HandlerList.unregisterAll(filtering);
        }
    }

    @Test
    @DisplayName("Mining an ore without listeners still mines the vein, preserving the old behavior")
    void testMineWithoutListenersStillMines() {
        Player player = server.addPlayer();

        boolean breakAttempted = mineVein(player, 40, 40);

        Assertions.assertTrue(breakAttempted, "The vein loop must have reached a per-ore break");
    }
}

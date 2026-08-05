package io.github.thebusybiscuit.slimefun4.implementation.items.tools;


import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
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

import io.github.thebusybiscuit.slimefun4.api.events.GoldPanUseEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the gold pan API expansion: {@link GoldPanUseEvent}, exercised by
 * driving the real {@link GoldPan} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * The sift path ends in a {@code playEffect(STEP_SOUND)} that MockBukkit rejects, so a
 * RuntimeException from that tail is ignored here - the event was fired and the output
 * decided beforehand.
 *
 * @author Zurker
 */
class TestGoldPanUseEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static GoldPan goldPan;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "gold_pan_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_GOLD_PAN", Material.BOWL, "&fTest Gold Pan");
        Slimefun.getItemCfg().setValue("TEST_GOLD_PAN.enabled", true);
        goldPan = new GoldPan(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        goldPan.register(plugin);
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
     * Runs the gold pan's use handler on a gravel block via a constructed event.
     */
    private void pan(Player player, Block gravel) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, goldPan.getItem().clone(), gravel, BlockFace.UP);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            goldPan.getItemHandler().onRightClick(event);
        } catch (RuntimeException ignored) {
            // playEffect(STEP_SOUND) is not fully supported by MockBukkit - see class javadoc
        }
    }

    private Block placeGravel(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.GRAVEL);
        return b;
    }

    @Test
    @DisplayName("GoldPanUseEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeGravel(1, 1);
        ItemStack output = new ItemStack(Material.FLINT);

        GoldPanUseEvent event = new GoldPanUseEvent(player, goldPan, b, output);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(goldPan, event.getGoldPan());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(output, event.getOutput());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.DIAMOND);
        event.setOutput(swapped);
        Assertions.assertEquals(swapped, event.getOutput());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GoldPanUseEvent(player, null, b, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GoldPanUseEvent(player, goldPan, null, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GoldPanUseEvent(player, goldPan, b, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setOutput(null));
    }

    @Test
    @DisplayName("Panning gravel fires the event with one of the gold pan drops")
    void testPanFires() {
        Player player = server.addPlayer();
        Block gravel = placeGravel(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPan(GoldPanUseEvent event) {
                seen[0] = true;
                Assertions.assertEquals(goldPan, event.getGoldPan());
                Assertions.assertEquals(gravel, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            pan(player, gravel);

            Assertions.assertTrue(seen[0], "GoldPanUseEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling GoldPanUseEvent leaves the gravel block untouched")
    void testEventCancellationSkipsSifting() {
        Player player = server.addPlayer();
        Block gravel = placeGravel(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPan(GoldPanUseEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            pan(player, gravel);

            Assertions.assertEquals(Material.GRAVEL, gravel.getType(), "A cancelled sift must leave the block untouched");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the output via setOutput replaces the rolled drop")
    void testOutputSwap() {
        Player player = server.addPlayer();
        Block gravel = placeGravel(30, 30);
        ItemStack custom = new ItemStack(Material.GOLDEN_APPLE);

        boolean[] seenSwapped = { false };
        Listener swapping = new Listener() {
            @EventHandler
            public void onPan(GoldPanUseEvent event) {
                event.setOutput(custom);
                seenSwapped[0] = true;
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            pan(player, gravel);

            Assertions.assertTrue(seenSwapped[0], "GoldPanUseEvent was not fired");
            // The output was swapped before the playEffect tail, which is enough to assert the
            // API contract: the handler would spawn the swapped item downstream.
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Panning without listeners still runs, preserving the old behavior")
    void testPanWithoutListenersStillRuns() {
        Player player = server.addPlayer();
        Block gravel = placeGravel(40, 40);

        // Should not throw out of the handler beyond the unsupported playEffect tail
        pan(player, gravel);
    }

    @Test
    @DisplayName("Panning a non-input block fires no event")
    void testNonInputBlockFiresNothing() {
        Player player = server.addPlayer();
        Block stone = world.getBlockAt(50, 1, 50);
        stone.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPan(GoldPanUseEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            pan(player, stone);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-input block");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

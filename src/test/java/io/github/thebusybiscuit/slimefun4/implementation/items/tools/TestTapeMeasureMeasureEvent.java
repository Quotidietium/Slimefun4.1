package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.TapeMeasureMeasureEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the tape measure API expansion: {@link TapeMeasureMeasureEvent},
 * exercised by driving the real {@link TapeMeasure}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with constructed
 * {@link PlayerRightClickEvent PlayerRightClickEvents}: a sneaking click sets the anchor, a
 * regular click measures against it.
 * <p>
 * The distance message is a no-op under unit tests (see
 * {@code SlimefunLocalization.sendMessage(CommandSender, String, UnaryOperator)}), so the
 * completion of a measurement is observed through the measure sound heard by the player.
 *
 * @author Zurker
 */
class TestTapeMeasureMeasureEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static TapeMeasure tapeMeasure;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "tape_measure_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_TAPE_MEASURE", Material.STRING, "&eTest Tape Measure");
        Slimefun.getItemCfg().setValue("_TEST_TAPE_MEASURE.enabled", true);
        tapeMeasure = new TapeMeasure(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        tapeMeasure.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private void click(PlayerMock player, Block block, boolean sneaking) {
        player.setSneaking(sneaking);
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, handItem, block, BlockFace.UP);
        tapeMeasure.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));
    }

    /**
     * Gives the player a tape measure, anchors it at (x, 4, 0) and measures the block three
     * blocks away.
     */
    private void anchorAndMeasure(PlayerMock player, int x) {
        player.getInventory().setItemInMainHand(tapeMeasure.getItem().clone());

        Block anchorBlock = world.getBlockAt(x, 4, 0);
        anchorBlock.setType(Material.STONE);
        click(player, anchorBlock, true);

        Block target = world.getBlockAt(x + 3, 4, 0);
        target.setType(Material.STONE);
        click(player, target, false);
    }

    /**
     * Whether the player heard the tape measure's measure sound (a book put-down sound).
     */
    private boolean heardMeasureSound(PlayerMock player) {
        return player.getHeardSounds().stream().anyMatch(audio -> audio.getSound().contains("book.put"));
    }

    @Test
    @DisplayName("TapeMeasureMeasureEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Location anchor = new Location(world, 0, 4, 0);
        Block measured = world.getBlockAt(3, 4, 0);

        TapeMeasureMeasureEvent event = new TapeMeasureMeasureEvent(player, tapeMeasure, anchor, measured, 3.0);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(tapeMeasure, event.getTapeMeasure());
        Assertions.assertEquals(anchor, event.getAnchor());
        Assertions.assertEquals(measured, event.getMeasuredBlock());
        Assertions.assertEquals(3.0, event.getDistance());
        Assertions.assertFalse(event.isCancelled());

        event.setDistance(42.5);
        Assertions.assertEquals(42.5, event.getDistance());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new TapeMeasureMeasureEvent(player, null, anchor, measured, 3.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TapeMeasureMeasureEvent(player, tapeMeasure, null, measured, 3.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TapeMeasureMeasureEvent(player, tapeMeasure, anchor, null, 3.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TapeMeasureMeasureEvent(player, tapeMeasure, anchor, measured, Double.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDistance(Double.POSITIVE_INFINITY));
    }

    @Test
    @DisplayName("Measuring fires the event with the computed distance and completes the measurement")
    void testMeasureFiresEvent() {
        PlayerMock player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMeasure(TapeMeasureMeasureEvent event) {
                seen[0] = true;
                Assertions.assertEquals(tapeMeasure, event.getTapeMeasure());
                Assertions.assertEquals(3.0, event.getDistance(), 0.0001, "The anchor is 3 blocks away");
                Assertions.assertEquals(new Location(world, 10, 4, 0), event.getAnchor());
                Assertions.assertEquals(world.getBlockAt(13, 4, 0), event.getMeasuredBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            anchorAndMeasure(player, 10);

            Assertions.assertTrue(seen[0], "TapeMeasureMeasureEvent was not fired");
            Assertions.assertTrue(heardMeasureSound(player), "The measure sound must have been played");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling TapeMeasureMeasureEvent skips the measurement feedback")
    void testEventCancellationSkipsFeedback() {
        PlayerMock player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onMeasure(TapeMeasureMeasureEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            anchorAndMeasure(player, 20);

            Assertions.assertFalse(heardMeasureSound(player), "A cancelled measurement must not play the measure sound");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Adjusting the distance via setDistance is visible to later listeners")
    void testDistanceAdjustment() {
        PlayerMock player = server.addPlayer();

        Listener adjusting = new Listener() {
            @EventHandler
            public void onMeasure(TapeMeasureMeasureEvent event) {
                event.setDistance(42.5);
            }
        };

        double[] reported = { -1 };
        Listener observing = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onMeasure(TapeMeasureMeasureEvent event) {
                reported[0] = event.getDistance();
            }
        };
        server.getPluginManager().registerEvents(adjusting, plugin);
        server.getPluginManager().registerEvents(observing, plugin);

        try {
            anchorAndMeasure(player, 30);

            Assertions.assertEquals(42.5, reported[0], 0.0001, "The adjusted distance must be the one the measurement continues with");
            Assertions.assertTrue(heardMeasureSound(player), "The measure sound must have been played");
        } finally {
            HandlerList.unregisterAll(adjusting);
            HandlerList.unregisterAll(observing);
        }
    }

    @Test
    @DisplayName("Measuring without an anchor neither fires the event nor plays the measure sound")
    void testNoAnchorSkipsMeasurement() {
        PlayerMock player = server.addPlayer();
        player.getInventory().setItemInMainHand(tapeMeasure.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMeasure(TapeMeasureMeasureEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Block target = world.getBlockAt(43, 4, 0);
            target.setType(Material.STONE);
            click(player, target, false);

            Assertions.assertFalse(seen[0], "TapeMeasureMeasureEvent must not fire without an anchor");
            Assertions.assertFalse(heardMeasureSound(player), "No measure sound must be played without an anchor");
            Assertions.assertNotNull(player.nextMessage(), "The no-anchor message must have been sent");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Measuring without listeners still completes the measurement, preserving the old behavior")
    void testMeasureWithoutListenersStillCompletes() {
        PlayerMock player = server.addPlayer();

        anchorAndMeasure(player, 50);

        Assertions.assertTrue(heardMeasureSound(player), "The measure sound must have been played");
    }
}

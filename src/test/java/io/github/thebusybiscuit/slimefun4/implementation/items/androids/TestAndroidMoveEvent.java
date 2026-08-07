package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

import io.github.thebusybiscuit.slimefun4.api.events.AndroidMoveEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the android API expansion: {@link AndroidMoveEvent}, exercised
 * by driving the real {@link ProgrammableAndroid#move(Block, BlockFace, Block)} against a
 * {@link BlockStorage}-backed (ownerless) android block with an empty destination ahead.
 * <p>
 * A move places the android head at the destination and migrates the block data, so tests
 * assert the outcome end-to-end: a cancelled event leaves both blocks untouched.
 *
 * @author Zurker
 */
class TestAndroidMoveEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ProgrammableAndroid android;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_move_test");
        // A moving android applies its head texture to the destination block, so the stack needs one
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MOVING_ANDROID", HeadTexture.PROGRAMMABLE_ANDROID, "&7Test Moving Android");
        Slimefun.getItemCfg().setValue("_TEST_MOVING_ANDROID.enabled", true);
        android = new ProgrammableAndroid(itemGroup, 1, stack, RecipeType.NULL, new ItemStack[9]);
        android.register(plugin);
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
     * Places an ownerless android block (legacy androids move without a permission check).
     */
    private Block placeAndroid(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_MOVING_ANDROID");
        return b;
    }

    @Test
    @DisplayName("AndroidMoveEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        Block to = world.getBlockAt(2, 1, 1);
        AndroidInstance instance = new AndroidInstance(android, b);

        AndroidMoveEvent event = new AndroidMoveEvent(instance, to, BlockFace.EAST);

        Assertions.assertEquals(instance, event.getAndroid());
        Assertions.assertEquals(android, event.getAndroid().getAndroid());
        Assertions.assertEquals(b, event.getAndroid().getBlock());
        Assertions.assertEquals(to, event.getTo());
        Assertions.assertEquals(BlockFace.EAST, event.getFace());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidMoveEvent(null, to, BlockFace.EAST));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidMoveEvent(instance, null, BlockFace.EAST));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidMoveEvent(instance, to, null));
    }

    @Test
    @DisplayName("Moving into an empty block fires the event and relocates the android")
    void testMoveFiresEventAndRelocates() {
        Block b = placeAndroid(10, 10);
        Block to = world.getBlockAt(11, 1, 10);
        to.setType(Material.AIR);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMove(AndroidMoveEvent event) {
                seen[0] = true;
                Assertions.assertEquals(android, event.getAndroid().getAndroid());
                Assertions.assertEquals(b, event.getAndroid().getBlock());
                Assertions.assertEquals(to, event.getTo());
                Assertions.assertEquals(BlockFace.EAST, event.getFace());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            android.move(b, BlockFace.EAST, to);

            Assertions.assertTrue(seen[0], "AndroidMoveEvent was not fired");
            Assertions.assertEquals(Material.PLAYER_HEAD, to.getType(), "The android head must have been placed at the destination");
            Assertions.assertTrue(b.getType().isAir(), "The origin block must have been cleared");
            // The block data migration is queued on the TickerTask, which never runs under MockBukkit
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AndroidMoveEvent keeps the android in place")
    void testCancelKeepsAndroidInPlace() {
        Block b = placeAndroid(20, 20);
        Block to = world.getBlockAt(21, 1, 20);
        to.setType(Material.AIR);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onMove(AndroidMoveEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            android.move(b, BlockFace.EAST, to);

            Assertions.assertTrue(to.getType().isAir(), "A cancelled move must leave the destination empty");
            Assertions.assertEquals(Material.PLAYER_HEAD, b.getType(), "A cancelled move must keep the android block in place");
            Assertions.assertEquals("_TEST_MOVING_ANDROID", BlockStorage.checkID(b), "A cancelled move must keep the block data in place");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Moving without listeners still relocates, preserving the old behavior")
    void testMoveWithoutListenersStillRelocates() {
        Block b = placeAndroid(30, 30);
        Block to = world.getBlockAt(31, 1, 30);
        to.setType(Material.AIR);

        android.move(b, BlockFace.EAST, to);

        Assertions.assertEquals(Material.PLAYER_HEAD, to.getType(), "The android head must have been placed at the destination");
        Assertions.assertTrue(b.getType().isAir(), "The origin block must have been cleared");
        // The block data migration is queued on the TickerTask, which never runs under MockBukkit
    }

    @Test
    @DisplayName("Moving into an occupied block fires no event")
    void testOccupiedDestinationFiresNothing() {
        Block b = placeAndroid(40, 40);
        Block to = world.getBlockAt(41, 1, 40);
        to.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMove(AndroidMoveEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            android.move(b, BlockFace.EAST, to);

            Assertions.assertFalse(seen[0], "No event must be fired for an occupied destination");
            Assertions.assertEquals(Material.STONE, to.getType(), "The occupied destination must be untouched");
            Assertions.assertEquals(Material.PLAYER_HEAD, b.getType(), "The android must have stayed in place");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("The android moves towards the direction it faces")
    void testMoveFollowsFacingDirection() {
        Block b = placeAndroid(50, 50);
        Block to = world.getBlockAt(50, 1, 51);
        to.setType(Material.AIR);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMove(AndroidMoveEvent event) {
                seen[0] = true;
                Assertions.assertEquals(BlockFace.SOUTH, event.getFace());
                Assertions.assertEquals(to, event.getTo());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            android.move(b, BlockFace.SOUTH, to);

            Assertions.assertTrue(seen[0], "AndroidMoveEvent was not fired");
            Assertions.assertEquals(Material.PLAYER_HEAD, to.getType(), "The android head must have been placed at the faced destination");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

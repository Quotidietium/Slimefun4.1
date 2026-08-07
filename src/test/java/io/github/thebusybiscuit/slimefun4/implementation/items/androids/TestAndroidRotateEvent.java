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

import io.github.thebusybiscuit.slimefun4.api.events.AndroidRotateEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the android API expansion: {@link AndroidRotateEvent}, exercised
 * by driving the real {@link ProgrammableAndroid#rotate(Block, BlockFace, int)} against a
 * {@link BlockStorage}-backed android block, following the rotation cycle
 * {@code NORTH -> EAST -> SOUTH -> WEST -> NORTH} with wraps at both ends.
 * <p>
 * A rotation writes the new facing into the block's data, so tests assert it end-to-end:
 * a cancelled event leaves the stored rotation untouched.
 *
 * @author Zurker
 */
class TestAndroidRotateEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ProgrammableAndroid android;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_rotate_test");
        // A rotating android re-applies its head texture, so the stack needs one
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ROTATING_ANDROID", HeadTexture.PROGRAMMABLE_ANDROID, "&7Test Rotating Android");
        Slimefun.getItemCfg().setValue("_TEST_ROTATING_ANDROID.enabled", true);
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
     * Places an android block facing the given direction.
     */
    private Block placeAndroid(int x, int z, BlockFace facing) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_ROTATING_ANDROID");
        BlockStorage.addBlockInfo(b, "rotation", facing.name());
        return b;
    }

    private String rotation(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "rotation");
    }

    @Test
    @DisplayName("AndroidRotateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        AndroidInstance instance = new AndroidInstance(android, b);

        AndroidRotateEvent event = new AndroidRotateEvent(instance, BlockFace.NORTH, BlockFace.EAST);

        Assertions.assertEquals(instance, event.getAndroid());
        Assertions.assertEquals(android, event.getAndroid().getAndroid());
        Assertions.assertEquals(BlockFace.NORTH, event.getPreviousRotation());
        Assertions.assertEquals(BlockFace.EAST, event.getNewRotation());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidRotateEvent(null, BlockFace.NORTH, BlockFace.EAST));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidRotateEvent(instance, null, BlockFace.EAST));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidRotateEvent(instance, BlockFace.NORTH, null));
    }

    @Test
    @DisplayName("Turning right fires the event and stores the new rotation")
    void testTurnRightFiresEventAndRotates() {
        Block b = placeAndroid(10, 10, BlockFace.NORTH);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRotate(AndroidRotateEvent event) {
                seen[0] = true;
                Assertions.assertEquals(android, event.getAndroid().getAndroid());
                Assertions.assertEquals(b, event.getAndroid().getBlock());
                Assertions.assertEquals(BlockFace.NORTH, event.getPreviousRotation());
                Assertions.assertEquals(BlockFace.EAST, event.getNewRotation());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            android.rotate(b, BlockFace.NORTH, 1);

            Assertions.assertTrue(seen[0], "AndroidRotateEvent was not fired");
            Assertions.assertEquals(BlockFace.EAST.name(), rotation(b), "The new rotation must have been stored");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AndroidRotateEvent keeps the current rotation")
    void testCancelKeepsRotation() {
        Block b = placeAndroid(20, 20, BlockFace.NORTH);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRotate(AndroidRotateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            android.rotate(b, BlockFace.NORTH, 1);

            Assertions.assertEquals(BlockFace.NORTH.name(), rotation(b), "A cancelled rotation must keep the stored rotation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Rotating without listeners still rotates, preserving the old behavior")
    void testRotateWithoutListenersStillRotates() {
        Block b = placeAndroid(30, 30, BlockFace.EAST);

        android.rotate(b, BlockFace.EAST, 1);

        Assertions.assertEquals(BlockFace.SOUTH.name(), rotation(b), "The new rotation must have been stored");
    }

    @Test
    @DisplayName("Turning right from WEST wraps around to NORTH")
    void testTurnRightWrapsAround() {
        Block b = placeAndroid(40, 40, BlockFace.WEST);

        BlockFace[] seen = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRotate(AndroidRotateEvent event) {
                seen[0] = event.getNewRotation();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            android.rotate(b, BlockFace.WEST, 1);

            Assertions.assertEquals(BlockFace.NORTH, seen[0], "The wrap-around rotation must be exposed on the event");
            Assertions.assertEquals(BlockFace.NORTH.name(), rotation(b), "The wrapped rotation must have been stored");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Turning left from NORTH wraps around to WEST")
    void testTurnLeftWrapsAround() {
        Block b = placeAndroid(50, 50, BlockFace.NORTH);

        android.rotate(b, BlockFace.NORTH, -1);

        Assertions.assertEquals(BlockFace.WEST.name(), rotation(b), "The left wrap-around rotation must have been stored");
    }
}

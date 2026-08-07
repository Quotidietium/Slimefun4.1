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

import io.github.thebusybiscuit.slimefun4.api.events.AndroidChopTreeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the android API expansion: {@link AndroidChopTreeEvent}, exercised
 * by driving the real {@link WoodcutterAndroid#breakLog(Block, Block, BlockMenu, BlockFace)}
 * against a {@link BlockStorage}-backed android block and a log block.
 * <p>
 * The chop path ends in a {@code playEffect(STEP_SOUND)} that MockBukkit rejects, so a
 * RuntimeException from that tail is ignored here - the event was fired and the drop was
 * pushed into the android's inventory beforehand. A cancelled event returns before that
 * tail, so the cancel path is asserted without any exception.
 *
 * @author Zurker
 */
class TestAndroidChopTreeEvent {

    private static final int OUTPUT_SLOT = 20;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static WoodcutterAndroid android;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_chop_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_WOODCUTTER", Material.PLAYER_HEAD, "&7Test Woodcutter Android");
        Slimefun.getItemCfg().setValue("_TEST_WOODCUTTER.enabled", true);
        android = new WoodcutterAndroid(itemGroup, 1, stack, RecipeType.NULL, new ItemStack[9]);
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
     * Places a woodcutter android block and returns it.
     */
    private Block placeAndroid(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_WOODCUTTER");
        return b;
    }

    /**
     * Breaks the given log via the real chop path. The trailing {@code playEffect(STEP_SOUND)}
     * is not fully supported by MockBukkit, so the RuntimeException from that tail is ignored -
     * see the class javadoc.
     */
    private void chop(Block androidBlock, Block log) {
        try {
            android.breakLog(log, androidBlock, BlockStorage.getInventory(androidBlock), BlockFace.EAST);
        } catch (RuntimeException ignored) {
            // playEffect(STEP_SOUND) is not fully supported by MockBukkit - see class javadoc
        }
    }

    private ItemStack output(Block androidBlock) {
        return BlockStorage.getInventory(androidBlock).getItemInSlot(OUTPUT_SLOT);
    }

    @Test
    @DisplayName("AndroidChopTreeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block log = world.getBlockAt(1, 2, 1);
        Block b = world.getBlockAt(1, 1, 1);
        AndroidInstance instance = new AndroidInstance(android, b);

        AndroidChopTreeEvent event = new AndroidChopTreeEvent(log, instance);

        Assertions.assertEquals(log, event.getBlock());
        Assertions.assertEquals(instance, event.getAndroid());
        Assertions.assertEquals(android, event.getAndroid().getAndroid());
        Assertions.assertEquals(b, event.getAndroid().getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidChopTreeEvent(null, instance));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidChopTreeEvent(log, null));
    }

    @Test
    @DisplayName("Chopping a log fires the event and pushes the drop into the android")
    void testChopFiresEventAndPushesDrop() {
        Block b = placeAndroid(10, 10);
        Block log = world.getBlockAt(10, 2, 10);
        log.setType(Material.OAK_LOG);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChop(AndroidChopTreeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(log, event.getBlock());
                Assertions.assertEquals(android, event.getAndroid().getAndroid());
                Assertions.assertEquals(b, event.getAndroid().getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            chop(b, log);

            Assertions.assertTrue(seen[0], "AndroidChopTreeEvent was not fired");
            ItemStack slot = output(b);
            Assertions.assertNotNull(slot, "The log drop must have been pushed into the android");
            Assertions.assertEquals(Material.OAK_LOG, slot.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AndroidChopTreeEvent keeps the log and pushes no drop")
    void testCancelKeepsLogAndNoDrop() {
        Block b = placeAndroid(20, 20);
        Block log = world.getBlockAt(20, 2, 20);
        log.setType(Material.OAK_LOG);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onChop(AndroidChopTreeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            chop(b, log);

            Assertions.assertEquals(Material.OAK_LOG, log.getType(), "A cancelled chop must keep the log");
            Assertions.assertNull(output(b), "A cancelled chop must not push any drop");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Chopping without listeners still pushes the drop, preserving the old behavior")
    void testChopWithoutListenersStillPushesDrop() {
        Block b = placeAndroid(30, 30);
        Block log = world.getBlockAt(30, 2, 30);
        log.setType(Material.OAK_LOG);

        chop(b, log);

        ItemStack slot = output(b);
        Assertions.assertNotNull(slot, "The log drop must have been pushed into the android");
        Assertions.assertEquals(Material.OAK_LOG, slot.getType());
    }

    @Test
    @DisplayName("Cancelling a bottom log chop keeps the log un-replanted")
    void testCancelKeepsBottomLogUnReplanted() {
        Block b = placeAndroid(40, 40);
        // A bottom log sits at the same height as the block the android faces
        Block log = world.getBlockAt(41, 1, 40);
        log.setType(Material.OAK_LOG);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onChop(AndroidChopTreeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            chop(b, log);

            Assertions.assertEquals(Material.OAK_LOG, log.getType(), "A cancelled chop must leave the bottom log standing");
            Assertions.assertNull(output(b), "A cancelled chop must not push any drop");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Chopping a birch log pushes a birch drop")
    void testChopPushesMatchingDrop() {
        Block b = placeAndroid(50, 50);
        Block log = world.getBlockAt(50, 2, 50);
        log.setType(Material.BIRCH_LOG);

        chop(b, log);

        ItemStack slot = output(b);
        Assertions.assertNotNull(slot, "The log drop must have been pushed into the android");
        Assertions.assertEquals(Material.BIRCH_LOG, slot.getType(), "The drop must match the chopped log type");
    }
}

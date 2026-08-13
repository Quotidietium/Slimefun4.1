package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.CowMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the android script-execution hardening:
 * <ul>
 * <li>An unknown or type-mismatched instruction token is skipped (index advanced)
 * instead of stalling the android on it forever while burning fuel.</li>
 * <li>Corrupted scalar data fails closed: "NaN"/"Infinity" fuel is treated as empty
 * and an overflowing "index" entry restarts the script instead of throwing
 * {@link ArrayIndexOutOfBoundsException}.</li>
 * <li>A tick is skipped while another android's data move into this location is
 * still queued, and {@code move()} refuses a destination that is part of a queued
 * move - together these close the chain-move race that destroyed androids.</li>
 * <li>The woodcutter gives up on a tree it may not chop instead of stalling.</li>
 * <li>A butcher android's kill tag does not survive on entities it failed to kill.</li>
 * </ul>
 *
 * @author Zurker
 */
class TestAndroidScriptExecution {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ProgrammableAndroid android;
    private static WoodcutterAndroid woodcutter;
    private static ButcherAndroid butcher;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_script_exec_test");

        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SCRIPT_ANDROID", HeadTexture.PROGRAMMABLE_ANDROID, "&7Test Script Android");
        Slimefun.getItemCfg().setValue("_TEST_SCRIPT_ANDROID.enabled", true);
        android = new ProgrammableAndroid(itemGroup, 1, stack, RecipeType.NULL, new ItemStack[9]);
        android.register(plugin);

        SlimefunItemStack woodStack = new SlimefunItemStack("_TEST_SCRIPT_WOODCUTTER", Material.PLAYER_HEAD, "&7Test Script Woodcutter");
        Slimefun.getItemCfg().setValue("_TEST_SCRIPT_WOODCUTTER.enabled", true);
        woodcutter = new WoodcutterAndroid(itemGroup, 1, woodStack, RecipeType.NULL, new ItemStack[9]);
        woodcutter.register(plugin);

        SlimefunItemStack butcherStack = new SlimefunItemStack("_TEST_SCRIPT_BUTCHER", Material.PLAYER_HEAD, "&7Test Script Butcher");
        Slimefun.getItemCfg().setValue("_TEST_SCRIPT_BUTCHER.enabled", true);
        butcher = new ButcherAndroid(itemGroup, 1, butcherStack, RecipeType.NULL, new ItemStack[9]);
        butcher.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Block placeAndroid(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_SCRIPT_ANDROID");
        return b;
    }

    private void setRunning(Block b, String fuel, String script) {
        BlockStorage.addBlockInfo(b, "paused", "false");
        BlockStorage.addBlockInfo(b, "fuel", fuel);

        if (script != null) {
            BlockStorage.addBlockInfo(b, "script", script);
        }
    }

    private Config data(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation());
    }

    @Test
    @DisplayName("An unknown instruction token is skipped instead of stalling the script")
    void testUnknownInstructionIsSkipped() {
        Block b = placeAndroid(10, 10);
        setRunning(b, "10", "START-FOO-REPEAT");

        android.tick(b, data(b));
        Assertions.assertEquals("0", data(b).getString("index"), "START must store its index");

        android.tick(b, data(b));
        Assertions.assertEquals("1", data(b).getString("index"), "The unknown token must have been skipped (index advanced)");
        Assertions.assertEquals("8.0", data(b).getString("fuel"), "Fuel is consumed per tick but the android must not stall");
    }

    @Test
    @DisplayName("An instruction for a different android type is skipped instead of stalling")
    void testTypeMismatchedInstructionIsSkipped() {
        Block b = placeAndroid(20, 20);
        setRunning(b, "10", "START-DIG_FORWARD-REPEAT");

        android.tick(b, data(b));
        Assertions.assertEquals("0", data(b).getString("index"));

        // A NONE android cannot execute DIG_FORWARD (requires MINER)
        android.tick(b, data(b));
        Assertions.assertEquals("1", data(b).getString("index"), "The type-mismatched token must have been skipped (index advanced)");
    }

    @Test
    @DisplayName("NaN or infinite fuel from corrupted data is treated as empty fuel")
    void testNonFiniteFuelFailsClosed() {
        Block nanBlock = placeAndroid(30, 30);
        setRunning(nanBlock, "NaN", null);

        android.tick(nanBlock, data(nanBlock));
        Assertions.assertNull(data(nanBlock).getString("index"), "NaN fuel must not run the script (it previously ran forever)");

        Block infBlock = placeAndroid(31, 30);
        setRunning(infBlock, "Infinity", null);

        android.tick(infBlock, data(infBlock));
        Assertions.assertNull(data(infBlock).getString("index"), "Infinite fuel must not run the script");
    }

    @Test
    @DisplayName("An overflowing index entry from corrupted data restarts the script")
    void testIndexOverflowFailsClosed() {
        Block b = placeAndroid(40, 40);
        setRunning(b, "10", null);
        BlockStorage.addBlockInfo(b, "index", String.valueOf(Integer.MAX_VALUE));

        // Integer.MAX_VALUE + 1 overflows to a negative index, previously an AIOOBE
        Assertions.assertDoesNotThrow(() -> android.tick(b, data(b)));
        Assertions.assertEquals("0", data(b).getString("index"), "The overflowed index must have restarted the script");
    }

    @Test
    @DisplayName("A tick is skipped while an incoming data move is queued for this location")
    void testTickSkippedWhileOccupiedSoon() {
        Block b = placeAndroid(50, 50);
        setRunning(b, "10", null);

        // Another android's data is on its way into this location: the head is not ours
        Slimefun.getTickerTask().queueMove(new Location(world, 51, 1, 50), b.getLocation());
        Assertions.assertTrue(Slimefun.getTickerTask().isOccupiedSoon(b.getLocation()));

        android.tick(b, data(b));
        Assertions.assertEquals("10", data(b).getString("fuel"), "The tick must have been skipped (no fuel consumed)");
        Assertions.assertNull(data(b).getString("index"), "The tick must have been skipped (no instruction executed)");
    }

    @Test
    @DisplayName("move() refuses a destination that is part of a queued data move")
    void testMoveRefusesDestinationWithQueuedMove() {
        Block b = placeAndroid(60, 60);
        Block to = world.getBlockAt(61, 1, 60);
        to.setType(Material.AIR);

        // Destination is about to RECEIVE another android's data
        Slimefun.getTickerTask().queueMove(new Location(world, 62, 1, 60), to.getLocation());

        android.move(b, BlockFace.EAST, to);

        Assertions.assertTrue(to.getType().isAir(), "The move must have been refused (incoming move pending)");
        Assertions.assertEquals(Material.PLAYER_HEAD, b.getType(), "The android must have stayed in place");
    }

    @Test
    @DisplayName("move() refuses a destination whose data has not left yet (chain move)")
    void testMoveRefusesDestinationWithOutgoingMove() {
        Block b = placeAndroid(70, 70);
        Block to = world.getBlockAt(71, 1, 70);
        to.setType(Material.AIR);

        // Destination data is about to LEAVE (the resident android moved away this tick)
        Slimefun.getTickerTask().queueMove(to.getLocation(), new Location(world, 72, 1, 70));
        Assertions.assertTrue(Slimefun.getTickerTask().isMovingFrom(to.getLocation()));

        android.move(b, BlockFace.EAST, to);

        Assertions.assertTrue(to.getType().isAir(), "The move must have been refused (chain move would overwrite data)");
        Assertions.assertEquals(Material.PLAYER_HEAD, b.getType(), "The android must have stayed in place");
    }

    @Test
    @DisplayName("The woodcutter gives up on a tree its owner may not chop instead of stalling")
    void testChopTreeSkipsUnchopabbleTree() {
        Block b = world.getBlockAt(80, 1, 80);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_SCRIPT_WOODCUTTER");

        Block log = world.getBlockAt(81, 1, 80);
        log.setType(Material.OAK_LOG);

        // No owner data: the android may not chop here. It must report "done" so the
        // script advances instead of replaying the effect and burning fuel forever.
        Assertions.assertTrue(woodcutter.chopTree(b, null, BlockFace.EAST), "An unchoppable tree must be given up on, not stalled on");
        Assertions.assertEquals(Material.OAK_LOG, log.getType(), "The protected log must be untouched");
    }

    @Test
    @DisplayName("A surviving entity does not keep the butcher android's kill tag")
    void testSurvivingEntityKeepsNoKillTag() {
        Block b = world.getBlockAt(90, 1, 90);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_SCRIPT_BUTCHER");
        BlockStorage.addBlockInfo(b, "owner", UUID.randomUUID().toString());

        CowMock cow = new CowMock(server, UUID.randomUUID());
        // Facing NORTH attacks entities at a lower Z
        cow.setLocation(new Location(world, 90, 1, 88));
        server.registerEntity(cow);

        double before = cow.getHealth();
        butcher.attack(b, BlockFace.NORTH, entity -> true);

        Assertions.assertTrue(cow.getHealth() < before, "The android must have attacked the cow");
        Assertions.assertFalse(cow.hasMetadata("android_killer"), "A surviving entity must not keep the kill tag - a later death would route drops into the android");
    }
}

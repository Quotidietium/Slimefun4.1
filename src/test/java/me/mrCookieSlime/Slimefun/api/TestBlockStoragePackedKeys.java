package me.mrCookieSlime.Slimefun.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;

/**
 * Discriminating coverage for the packed-long block keys inside
 * {@link BlockStorage}: the in-memory maps are keyed by block-aligned
 * coordinates instead of full {@link Location} equality, which aligns the
 * in-memory keying with what the on-disk format has always done
 * (serializeLocation writes block coordinates).
 *
 * <p>These tests pin: the block-alignment of the key, the coordinate range
 * that must keep working (vanilla world bounds), and the loud failure for
 * writes outside the packable range while reads keep returning "no data".
 *
 * @author Zurker
 */
class TestBlockStoragePackedKeys {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("A yaw/pitch-carrying Location resolves to the same block data as its block-aligned twin")
    void testBlockAlignedKeyIgnoresYawAndPitch() {
        Block block = world.getBlockAt(50, 64, 50);
        block.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(block, "id", "TEST_PACKED_KEY_ITEM", true);

        // The block-aligned twin
        Location aligned = new Location(world, 50, 64, 50);
        // Same block coordinates, but carrying rotation like an entity position
        Location rotated = new Location(world, 50.7, 64.2, 50.9, 135.0F, -90.0F);

        Config alignedData = BlockStorage.getLocationInfo(aligned);
        Config rotatedData = BlockStorage.getLocationInfo(rotated);

        Assertions.assertEquals("TEST_PACKED_KEY_ITEM", alignedData.getString("id"), "The block-aligned lookup must find the data");
        Assertions.assertSame(alignedData, rotatedData, "The in-memory key must be block-aligned, matching the on-disk key");
        Assertions.assertTrue(BlockStorage.hasBlockInfo(rotated), "hasBlockInfo must be block-aligned too");
    }

    @Test
    @DisplayName("Data survives the extreme legal coordinates of a vanilla world")
    void testExtremeLegalCoordinatesRoundTrip() {
        Location ground = new Location(world, -16_777_216, -64, 16_777_215);
        Location ceiling = new Location(world, 16_777_215, 320, -16_777_216);

        BlockStorage.addBlockInfo(ground, "id", "EXTREME_GROUND", true);
        BlockStorage.addBlockInfo(ceiling, "id", "EXTREME_CEILING", true);

        Assertions.assertEquals("EXTREME_GROUND", BlockStorage.getLocationInfo(ground).getString("id"));
        Assertions.assertEquals("EXTREME_CEILING", BlockStorage.getLocationInfo(ceiling).getString("id"));

        // Reading through a distinct-but-equal Location object must also find them
        Assertions.assertEquals("EXTREME_GROUND", BlockStorage.getLocationInfo(new Location(world, -16_777_216, -64, 16_777_215)).getString("id"));
    }

    @Test
    @DisplayName("Reads for non-storable Locations report no data instead of throwing")
    void testOutOfRangeReadReturnsNoData() {
        Location aboveBuildLimit = new Location(world, 0, 5000, 0);
        Location beyondBorder = new Location(world, 40_000_000, 64, 0);

        Assertions.assertFalse(BlockStorage.hasBlockInfo(aboveBuildLimit));
        Assertions.assertFalse(BlockStorage.hasBlockInfo(beyondBorder));
        Assertions.assertNull(BlockStorage.getLocationInfo(aboveBuildLimit).getString("id"));
        Assertions.assertNull(BlockStorage.getLocationInfo(beyondBorder).getString("id"));
    }

    @Test
    @DisplayName("Writes outside the packable range fail loudly instead of corrupting a neighbouring key")
    void testOutOfRangeWriteThrows() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> BlockStorage.addBlockInfo(new Location(world, 0, 5000, 0), "id", "OUT_OF_RANGE", true));
    }

    @Test
    @DisplayName("getRawStorage still exposes a Location-keyed view of every entry")
    void testRawStorageView() {
        Block block = world.getBlockAt(70, 64, 70);
        BlockStorage.addBlockInfo(block, "id", "TEST_RAW_STORAGE_ITEM", true);

        Location key = new Location(world, 70, 64, 70);
        Config data = BlockStorage.getRawStorage(world).get(key);

        Assertions.assertNotNull(data, "The raw storage view must contain the block under its Location");
        Assertions.assertEquals("TEST_RAW_STORAGE_ITEM", data.getString("id"));
    }
}

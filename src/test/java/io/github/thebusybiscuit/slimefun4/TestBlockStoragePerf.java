package io.github.thebusybiscuit.slimefun4;

import org.bukkit.Location;
import org.bukkit.World;
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
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression tests for the charge-write hot path optimisations:
 * <ul>
 * <li>{@link BlockStorage#updateBlockInfo} — the lightweight write path that reuses the
 * caller-held {@link Config} (falling back to {@link BlockStorage#addBlockInfo} for a
 * block with no data yet).</li>
 * <li>{@link BlockStorage#setBlockInfo} skipping the redundant {@code put} when the
 * stored reference is already the one being written (the energy-charge update path).</li>
 * </ul>
 */
class TestBlockStoragePerf {

    private static ServerMock server;
    private World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        world = TestUtilities.createWorld(server);
    }

    @Test
    @DisplayName("Test updateBlockInfo updates an existing block's value")
    void testUpdateBlockInfoUpdatesValue() {
        Location l = new Location(world, 0, 0, 0);
        BlockStorage.addBlockInfo(l, "id", "MACHINE_X", false);

        Config data = BlockStorage.getLocationInfo(l);
        BlockStorage.updateBlockInfo(l, data, "energy-charge", "42");

        Assertions.assertEquals("42", BlockStorage.getLocationInfo(l, "energy-charge"));
        // The id must be untouched by a charge-style value update.
        Assertions.assertEquals("MACHINE_X", BlockStorage.getLocationInfo(l, "id"));
    }

    @Test
    @DisplayName("Test updateBlockInfo on a data-less block falls back to creating a record")
    void testUpdateBlockInfoEmptyBlockFallsBack() {
        Location l = new Location(world, 1, 0, 0);

        // No prior addBlockInfo -> getLocationInfo returns the shared EmptyBlockData singleton.
        Config data = BlockStorage.getLocationInfo(l);

        // updateBlockInfo must detect the empty singleton and fall back to addBlockInfo
        // (otherwise it would silently mutate the shared singleton and write nothing).
        BlockStorage.updateBlockInfo(l, data, "energy-charge", "7");

        Assertions.assertEquals("7", BlockStorage.getLocationInfo(l, "energy-charge"));
    }

    @Test
    @DisplayName("Test repeated addBlockInfo on the same block preserves the id (same-reference setBlockInfo path)")
    void testRepeatedAddBlockInfoPreservesId() {
        Location l = new Location(world, 2, 0, 0);

        // First write stores the block with an id.
        BlockStorage.addBlockInfo(l, "id", "MACHINE_X", false);

        // Second write reuses the same live Config (same-reference setBlockInfo path,
        // which now skips the redundant put). The id must survive, the new value must land.
        BlockStorage.addBlockInfo(l, "energy-charge", "99", false);

        Assertions.assertEquals("MACHINE_X", BlockStorage.getLocationInfo(l, "id"));
        Assertions.assertEquals("99", BlockStorage.getLocationInfo(l, "energy-charge"));
    }

    @Test
    @DisplayName("Test getLocationInfo(Location, BlockStorage) overload matches the single-arg version")
    void testGetLocationInfoOverloadMatches() {
        Location l = new Location(world, 3, 0, 0);
        BlockStorage.addBlockInfo(l, "id", "MACHINE_Y", false);
        BlockStorage.addBlockInfo(l, "foo", "bar", false);

        BlockStorage storage = BlockStorage.getStorage(world);
        Assertions.assertNotNull(storage);

        Config viaOverload = BlockStorage.getLocationInfo(l, storage);
        Config viaSingle = BlockStorage.getLocationInfo(l);

        Assertions.assertEquals(viaSingle.getString("id"), viaOverload.getString("id"));
        Assertions.assertEquals("bar", viaOverload.getString("foo"));
    }
}

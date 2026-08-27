package io.github.thebusybiscuit.slimefun4.api.gps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for {@link TeleportationManager#getTeleportationTime(int, Location, Location)}:
 * the complexity-squared term used to be computed in int arithmetic and wrapped around at
 * already-reachable complexities (5x tier-4 transmitters at world height exceed 90,000),
 * snapping the teleportation time back to the 40-interval maximum instead of the fastest one.
 *
 * @author Zurker
 */
class TestTeleportationTime {

    private static ServerMock server;
    private static World world;

    private static TeleportationManager manager;
    private static Location source;
    private static Location farDestination;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(io.github.thebusybiscuit.slimefun4.implementation.Slimefun.class);
        world = TestUtilities.createWorld(server);

        manager = new TeleportationManager();
        source = new Location(world, 0, 64, 0);

        // ~5,000 blocks away - the distance term stays at its cap-free far end
        farDestination = new Location(world, 3000, 64, 4000);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Complexity below 100 always yields the 100-interval baseline")
    void testBaselineBelowHundred() {
        assertEquals(100, manager.getTeleportationTime(0, source, farDestination));
        assertEquals(100, manager.getTeleportationTime(99, source, farDestination));
    }

    @Test
    @DisplayName("Complexity 92,682 must not wrap to the 40-interval maximum")
    void testIntOverflowSweetSpot() {
        // 92,682^2 mod 2^32 is ~18,500: int arithmetic used to produce a tiny "speed",
        // making this the slowest possible far teleport despite the huge complexity
        assertEquals(1, manager.getTeleportationTime(92682, source, farDestination));
    }

    @Test
    @DisplayName("Teleportation time is non-increasing as complexity grows")
    void testMonotonicityAcrossReachableComplexities() {
        int previous = Integer.MAX_VALUE;

        // Covers the int-overflow region (46,341+) up to values no legitimate network reaches
        for (int complexity = 100; complexity <= 300_000; complexity += 250) {
            int time = manager.getTeleportationTime(complexity, source, farDestination);
            assertTrue(time <= previous, "Teleportation time increased at complexity " + complexity + ": " + previous + " -> " + time);
            previous = time;
        }

        assertEquals(1, previous);
    }

    @Test
    @DisplayName("Short distances and capped distances stay within bounds")
    void testDistanceBounds() {
        Location near = new Location(world, 10, 64, 10);

        // Both ends of the distance scale remain in the [1, 40] window
        assertTrue(manager.getTeleportationTime(100, source, near) >= 1);
        assertTrue(manager.getTeleportationTime(100, source, near) <= 40);
        assertTrue(manager.getTeleportationTime(100, source, farDestination) >= 1);
        assertTrue(manager.getTeleportationTime(100, source, farDestination) <= 40);
    }

    @Test
    @DisplayName("Same-world distance squared beyond int range must stay at the 40-interval cap")
    void testUltraFarDistanceSaturation() {
        /*
         * 50,000 blocks apart: distanceSquared = 2.5e9, past Integer.MAX_VALUE. The
         * double-to-int cast in TeleportationManager#distanceSquared saturates
         * (JLS 5.1.3) to Integer.MAX_VALUE rather than wrapping, so the subsequent
         * Math.min clamp still applies and the farthest reachable teleport stays at
         * the 20-second cap. This pins that behavior: a refactor switching the
         * distance term to int arithmetic (e.g. computing delta coordinates as ints
         * first) would wrap negative and collapse the time to the fastest interval.
         */
        Location ultraFar = new Location(world, 50_000, 64, 0);
        assertEquals(40, manager.getTeleportationTime(100, source, ultraFar));
    }
}

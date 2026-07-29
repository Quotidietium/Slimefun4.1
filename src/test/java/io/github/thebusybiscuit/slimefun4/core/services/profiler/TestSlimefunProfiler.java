package io.github.thebusybiscuit.slimefun4.core.services.profiler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression tests for the on-demand profiler collection optimisation.
 *
 * <p>The profiler used to collect per-block timings on every single tick. After the
 * optimisation it only does so when a summary was requested (/sf timings); on every
 * other tick {@link SlimefunProfiler#newEntry()} returns {@code 0} immediately and the
 * tick's total elapsed time is recorded via {@link SlimefunProfiler#endTick(long)} so
 * that {@link SlimefunProfiler#getTime()} (the PlaceholderAPI placeholder) stays current.
 */
class TestSlimefunProfiler {

    private static ServerMock server;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test profiler getTime starts at 0ms")
    void testGetTimeInitiallyZero() {
        SlimefunProfiler profiler = new SlimefunProfiler();
        Assertions.assertEquals("0ms", profiler.getTime());
    }

    @Test
    @DisplayName("Test idle tick (no /sf timings request) skips per-block collection")
    void testIdleDoesNotCollect() {
        SlimefunProfiler profiler = new SlimefunProfiler();

        // No pending request -> start() leaves isProfiling false.
        profiler.start();

        // On the no-op path newEntry() returns 0 immediately (no nanoTime, no queued bump).
        Assertions.assertEquals(0, profiler.newEntry());

        // closeEntry() with a zero timestamp is likewise a no-op.
        Assertions.assertEquals(0, profiler.newEntry());
    }

    @Test
    @DisplayName("Test endTick records the tick total elapsed time for getTime")
    void testEndTickUpdatesGetTime() {
        SlimefunProfiler profiler = new SlimefunProfiler();
        profiler.start();
        profiler.endTick(2_000_000L);

        // getTime() now reflects this tick's total (the PlaceholderAPI placeholder stays 実时).
        Assertions.assertNotEquals("0ms", profiler.getTime());
    }

    @Test
    @DisplayName("Test /sf timings request enables per-block collection on the next start")
    void testRequestEnablesCollection() {
        SlimefunProfiler profiler = new SlimefunProfiler();

        PerformanceInspector inspector = org.mockito.Mockito.mock(PerformanceInspector.class);
        profiler.requestSummary(inspector);

        // A pending request makes start() flip isProfiling to true ...
        profiler.start();

        // ... so newEntry() now returns a real (non-zero) timestamp instead of the no-op 0.
        Assertions.assertNotEquals(0, profiler.newEntry());
    }

    @Test
    @DisplayName("Test endTick without a request does not touch profiling state")
    void testEndTickIdlePreservesState() {
        SlimefunProfiler profiler = new SlimefunProfiler();
        profiler.start();
        profiler.endTick(1_000_000L);

        // After an idle endTick, a subsequent idle tick still skips collection.
        profiler.start();
        Assertions.assertEquals(0, profiler.newEntry());
    }
}

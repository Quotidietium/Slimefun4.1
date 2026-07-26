package benchmark.scenarios;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;

import benchmark.BenchContext;
import benchmark.BenchItems;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Measures a full {@link TickerTask#run()} cycle over many ticking blocks
 * with a trivial asynchronous ticker. This isolates the tick dispatch
 * overhead (location info lookup, item resolution, ticker resolution) that
 * the optimization reduced.
 *
 * <p>In the MockBukkit unit-test environment the plugin does not start its
 * scheduled ticker ({@code onUnitTestStart} skips {@code ticker.start}), so
 * no background runs interfere. The {@code running}-guard checks below are
 * kept defensively in case that ever changes; an early return from the guard
 * costs single-digit microseconds while a real run over thousands of blocks
 * costs far more, so samples below {@link #EARLY_RETURN_THRESHOLD_NS} are
 * discarded.
 */
public final class TickerRunBench {

    private static final int BLOCKS = 5000;
    private static final int WARMUP = 3;
    private static final int SAMPLES_WANTED = 15;
    private static final int MAX_ATTEMPTS = 80;

    /**
     * Anything faster than 50 us cannot be a real run over {@link #BLOCKS}
     * blocks; it must be an early return from the running-guard.
     */
    private static final long EARLY_RETURN_THRESHOLD_NS = 50_000;

    public void run(BenchContext ctx, Results results) {
        // y=120: MockBukkit's WorldMock rejects y >= 128 (its max height).
        List<Location> locations = ctx.grid(BLOCKS, 120);

        for (Location l : locations) {
            // Force chunk creation so tickChunk() sees loaded chunks.
            ctx.world().getChunkAt(l);
            BlockStorage.addBlockInfo(l, "id", BenchItems.TICKER_ID, true);
        }

        TickerTask task = Slimefun.getTickerTask();
        Field running = runningField(task, results);

        if (running == null) {
            return;
        }

        var locationMap = task.getLocations();
        int tickingBlocks = locationMap.values().stream().mapToInt(java.util.Set::size).sum();
        results.note("ticker-run: ticking chunks=" + locationMap.size() + ", ticking blocks=" + tickingBlocks);

        if (!locationMap.isEmpty()) {
            var firstChunk = locationMap.keySet().iterator().next();
            results.note("ticker-run: first chunk isLoaded=" + firstChunk.isLoaded());
        }

        // Integrity check: verify that blocks actually get ticked.
        BenchItems.TICK_COUNTER.set(0);
        task.run();
        long ticked = BenchItems.TICK_COUNTER.get();

        if (ticked == 0) {
            results.note("ticker-run: TickerTask.run() ticked 0 blocks, running manual diagnostic tick");

            try {
                Location probe = locations.get(0);
                var data = BlockStorage.getLocationInfo(probe);
                var item = io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getById(data.getString("id"));
                results.note("ticker-run: probe item=" + item + ", ticker=" + (item == null ? null : item.getBlockTicker()));
                item.getBlockTicker().tick(probe.getBlock(), item, data);
                results.note("ticker-run: manual tick succeeded, counter=" + BenchItems.TICK_COUNTER.get());
            } catch (Throwable t) {
                java.io.StringWriter sw = new java.io.StringWriter();
                t.printStackTrace(new java.io.PrintWriter(sw));
                results.note("ticker-run: manual tick threw: " + sw);
            }

            task.halt();
            return;
        }

        results.note("ticker-run: " + ticked + " blocks ticked per run");

        for (int i = 0; i < WARMUP; i++) {
            awaitGuardClear(running, task);
            task.run();
        }

        long[] samples = new long[SAMPLES_WANTED];
        int collected = 0;

        for (int attempt = 0; attempt < MAX_ATTEMPTS && collected < SAMPLES_WANTED; attempt++) {
            awaitGuardClear(running, task);

            long start = System.nanoTime();
            task.run();
            long elapsed = System.nanoTime() - start;

            if (elapsed >= EARLY_RETURN_THRESHOLD_NS) {
                samples[collected++] = elapsed;
            }
        }

        task.halt();

        if (collected == 0) {
            results.note("ticker-run: no clean samples collected, scenario skipped");
            return;
        }

        long[] sorted = Arrays.copyOf(samples, collected);
        Arrays.sort(sorted);

        if (collected < SAMPLES_WANTED) {
            results.note("ticker-run: only " + collected + " clean samples collected");
        }

        double medianMs = sorted[sorted.length / 2] / 1_000_000.0;
        double minMs = sorted[0] / 1_000_000.0;

        results.emit("ticker-run", "5000-trivial-tickers", "median_ms_per_run", "ms", medianMs);
        results.emit("ticker-run", "5000-trivial-tickers", "min_ms_per_run", "ms", minMs);
        results.emit("ticker-run", "5000-trivial-tickers", "median_ns_per_block", "ns",
            sorted[sorted.length / 2] / (double) BLOCKS);
    }

    private Field runningField(TickerTask task, Results results) {
        try {
            Field field = TickerTask.class.getDeclaredField("running");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException x) {
            results.note("ticker-run: cannot access TickerTask.running (" + x + "), scenario skipped");
            return null;
        }
    }

    private void awaitGuardClear(Field running, TickerTask task) {
        try {
            while (running.getBoolean(task)) {
                Thread.sleep(1);
            }
        } catch (IllegalAccessException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

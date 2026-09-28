package benchmark.scenarios;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchItems;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Times the per-block resolution chain that {@code TickerTask#tickLocation}
 * performs before every ticker invocation: the {@code BlockStorage} map read
 * ({@code getLocationInfo(l, storage)}), the {@code data.getString("id")} key
 * extraction and the {@code SlimefunItem.getById(id)} registry lookup.
 *
 * <p>This is the calibration anchor for the ticker-structure optimization:
 * whichever parts of the chain get eliminated predict a per-block saving of
 * (removed chain cost) on {@code TickerRunBench}, the same micro-plus-macro
 * methodology the protection-query round used.
 *
 * <p>Variants:
 * <ul>
 * <li><b>info-get</b>: only the live {@code Config} map read (the part that
 * must stay per-tick for data freshness).</li>
 * <li><b>full-chain</b>: map read + id extraction + item lookup (everything
 * the old per-tick path paid).</li>
 * </ul>
 */
public final class TickerResolutionBench {

    private static final int BLOCKS = 2000;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int CALLS_PER_ROUND = 50;

    public void run(BenchContext ctx, Results results) {
        // y=110: below the y=120 band TickerRunBench uses; no ticker entries
        // are enabled here, this scenario only reads block data.
        List<Location> locations = ctx.grid(BLOCKS, 110);

        for (Location l : locations) {
            BlockStorage.addBlockInfo(l, "id", BenchItems.TICKER_ID, false);
        }

        BlockStorage storage = BlockStorage.getStorage(ctx.world());

        chain(ctx, results, locations, storage, true);
        Bench.gcSettle();
        chain(ctx, results, locations, storage, false);
    }

    private void chain(BenchContext ctx, Results results, List<Location> locations, BlockStorage storage, boolean infoGetOnly) {
        for (int w = 0; w < WARMUP; w++) {
            spin(locations, storage, infoGetOnly);
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < CALLS_PER_ROUND; i++) {
                spin(locations, storage, infoGetOnly);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        String variant = infoGetOnly ? "info-get" : "full-chain";
        double divisor = (double) BLOCKS * CALLS_PER_ROUND;
        results.emit("ticker-resolution", variant, "min_ns_per_block", "ns", Bench.min(samples) / divisor);
        results.emit("ticker-resolution", variant, "median_ns_per_block", "ns", Bench.median(samples) / divisor);
    }

    /**
     * One pass over every location, performing exactly the lookups
     * {@code tickLocation} performs per tick (blackholed into an accumulator
     * the JIT cannot eliminate).
     */
    private long spin(List<Location> locations, BlockStorage storage, boolean infoGetOnly) {
        long accumulator = 0;

        for (Location l : locations) {
            Config data = BlockStorage.getLocationInfo(l, storage);

            if (!infoGetOnly) {
                String id = data.getString("id");
                SlimefunItem item = id == null ? null : SlimefunItem.getById(id);
                accumulator += item == null ? 0 : 1;
            } else {
                accumulator += data == null ? 0 : 1;
            }
        }

        return accumulator;
    }
}

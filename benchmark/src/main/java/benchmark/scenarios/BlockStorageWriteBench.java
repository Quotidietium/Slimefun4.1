package benchmark.scenarios;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchItems;
import benchmark.Results;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Measures the BlockStorage write hot path and the save path.
 *
 * <p>This is the workload every energy network performs: one
 * {@code energy-charge} write per component per tick. The optimized version
 * turns the write into a pure in-memory operation and defers JSON
 * serialization to {@link BlockStorage#save()}.
 */
public final class BlockStorageWriteBench {

    private static final int BLOCKS = 5000;
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;
    private static final int SAVE_ROUNDS = 5;

    public void run(BenchContext ctx, Results results) {
        List<Location> locations = ctx.grid(BLOCKS, 64);

        for (Location l : locations) {
            BlockStorage.addBlockInfo(l, "id", BenchItems.DUMMY_ID, false);
        }

        BlockStorage storage = BlockStorage.getStorage(ctx.world());

        // --- Hot write path: one charge update per block per "tick" ---
        long[] samples = Bench.timeRounds(WARMUP, ROUNDS, round -> {
            for (int i = 0; i < BLOCKS; i++) {
                // The value changes on every write, exactly like a live network.
                BlockStorage.addBlockInfo(locations.get(i), "energy-charge", String.valueOf(round * BLOCKS + i), false);
            }
        });

        results.emit("blockstorage", "charge-write", "median_ns_per_write", "ns",
            (double) Bench.median(samples) / BLOCKS);
        results.emit("blockstorage", "charge-write", "min_ns_per_write", "ns",
            (double) Bench.min(samples) / BLOCKS);

        // --- Save path: dirty all blocks, then persist (inner timing, the
        // dirtying loop above is setup and not part of the measurement) ---
        long[] saveSamples = new long[SAVE_ROUNDS];

        for (int round = 0; round < SAVE_ROUNDS; round++) {
            for (int i = 0; i < BLOCKS; i++) {
                BlockStorage.addBlockInfo(locations.get(i), "energy-charge", String.valueOf(-(round * BLOCKS + i)), false);
            }

            long start = System.nanoTime();
            storage.save();
            saveSamples[round] = System.nanoTime() - start;
        }

        Arrays.sort(saveSamples);

        results.emit("blockstorage", "save-5000-dirty-blocks", "median_ms_per_save", "ms",
            (double) Bench.median(saveSamples) / 1_000_000.0);
        results.emit("blockstorage", "save-5000-dirty-blocks", "min_ms_per_save", "ms",
            (double) Bench.min(saveSamples) / 1_000_000.0);
    }
}

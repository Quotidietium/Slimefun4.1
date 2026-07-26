package benchmark.scenarios;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * Measures the capacitor texture update path.
 *
 * <p>Every charge change of a capacitor triggers
 * {@link SlimefunUtils#updateCapacitorTexture}. The baseline dispatches a
 * texture update task every single time; the optimized version only
 * dispatches when the 4-stage fill level actually changes.
 *
 * <p>The scenario keeps the charge within a single stage (the overwhelmingly
 * common case on a live server). If MockBukkit supports skull block states,
 * real PLAYER_HEAD blocks are used so the baseline also pays the actual
 * texture application; otherwise the scenario falls back to measuring the
 * dispatch overhead only (noted in the results).
 */
public final class CapacitorTextureBench {

    private static final int CAPACITORS = 2000;
    private static final int CAPACITY = 128;
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;

    public void run(BenchContext ctx, Results results) {
        List<Location> locations = ctx.grid(CAPACITORS, 96);

        boolean realHeads = probeSkullSupport(ctx, locations.get(0), results);

        if (realHeads) {
            for (Location l : locations) {
                ctx.world().getBlockAt(l).setType(Material.PLAYER_HEAD);
            }

            results.note("capacitor-texture: using real PLAYER_HEAD blocks (baseline pays full texture application)");
        } else {
            results.note("capacitor-texture: MockBukkit lacks skull support, measuring dispatch overhead only");
        }

        // Charge oscillates between ~5% and ~9%: always stage 0, so the
        // optimized version dispatches once and then skips every call.
        long[] samples = Bench.timeRounds(WARMUP, ROUNDS, round -> {
            int charge = 6 + (round % 5);

            for (Location l : locations) {
                SlimefunUtils.updateCapacitorTexture(l, charge, CAPACITY);
            }
        });

        String variant = realHeads ? "same-stage-real-heads" : "same-stage-dispatch-only";
        results.emit("capacitor-texture", variant, "median_ns_per_call", "ns",
            (double) Bench.median(samples) / CAPACITORS);
        results.emit("capacitor-texture", variant, "min_ns_per_call", "ns",
            (double) Bench.min(samples) / CAPACITORS);
    }

    /**
     * Tries one real texture update against a PLAYER_HEAD block. Returns
     * whether MockBukkit supports the skull code path.
     */
    private boolean probeSkullSupport(BenchContext ctx, Location probe, Results results) {
        try {
            ctx.world().getBlockAt(probe).setType(Material.PLAYER_HEAD);
            SlimefunUtils.updateCapacitorTexture(probe, 6, CAPACITY);
            return true;
        } catch (Exception | LinkageError x) {
            ctx.world().getBlockAt(probe).setType(Material.AIR);
            results.note("capacitor-texture: skull probe failed with " + x.getClass().getSimpleName() + ": " + x.getMessage());
            return false;
        }
    }
}

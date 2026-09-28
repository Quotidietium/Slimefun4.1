package benchmark.scenarios;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.BenchCargoRoute;

/**
 * Measures the per-tick routing-map work of {@code CargoNet#tick}:
 * {@code mapInputNodes()} + {@code mapOutputNodes()}, decoupled from
 * holograms, profiling and task scheduling.
 *
 * <p>With the default {@code networks.cargo-ticker-delay: 0} this mapping is
 * rebuilt from scratch on <strong>every game tick</strong> for every cargo
 * network - each node paying a BlockStorage read plus a frequency parse. The
 * mapping only actually changes when a node is added/removed or a node's
 * configuration changes, which is what the routing-cache optimization
 * exploits.
 *
 * <p>Variants: {@code mapping-100} and {@code mapping-400} (nodes per side).
 * Each round performs {@value #CALLS_PER_ROUND} mapping ticks; the metric is
 * per-tick cost. Reflection overhead is identical on both A/B sides.
 */
public final class CargoMappingBench {

    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int CALLS_PER_ROUND = 50;

    public void run(BenchContext ctx, Results results) {
        mapping(ctx, results, 100, "mapping-100");
        Bench.gcSettle();
        mapping(ctx, results, 400, "mapping-400");
    }

    private void mapping(BenchContext ctx, Results results, int nodesPerSide, String variant) {
        // Disjoint z-bands far away from every cargo-route band (which reach z=311)
        int zBase = nodesPerSide == 100 ? 500 : 600;

        Runnable mapping = BenchCargoRoute.mappingDriver(ctx.world(), nodesPerSide, zBase);

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < CALLS_PER_ROUND; i++) {
                mapping.run();
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < CALLS_PER_ROUND; i++) {
                mapping.run();
            }

            samples[r] = System.nanoTime() - start;
        }

        java.util.Arrays.sort(samples);
        results.emit("cargo-mapping", variant, "median_ns_per_tick", "ns", (double) Bench.median(samples) / CALLS_PER_ROUND);
        results.emit("cargo-mapping", variant, "min_ns_per_tick", "ns", (double) Bench.min(samples) / CALLS_PER_ROUND);
        results.note("cargo-mapping " + variant + ": z-base " + zBase + ", nodes/side " + nodesPerSide);
    }
}

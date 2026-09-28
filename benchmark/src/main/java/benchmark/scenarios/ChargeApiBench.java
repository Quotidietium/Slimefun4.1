package benchmark.scenarios;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchMachine;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Isolates the charge read-modify-write API that every electric machine calls
 * on every tick.
 *
 * <ul>
 * <li><b>take-charge-hit</b>: {@code AContainer#takeCharge} with sufficient
 * charge (parse + compare + write) - the pre-consumption gate of all processing
 * machines.</li>
 * <li><b>take-charge-miss</b>: {@code takeCharge} on a machine whose charge is
 * below the consumption (parse + compare + early return).</li>
 * <li><b>single-arg-remove</b>: the single-argument
 * {@code EnergyNetComponent#removeCharge(Location, int)} - the delegated form
 * used by the tick loops of roughly a dozen machines (accelerators, GEOMiner,
 * FluidPump, AutoCrafter, ...).</li>
 * </ul>
 *
 * <p>Every round is preceded by an untimed top-up back to the intended charge
 * level, so all timed iterations stay on the variant's intended code path even
 * though each call mutates the stored charge.
 */
public final class ChargeApiBench {

    private static final int MACHINES = 1000;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;

    public void run(BenchContext ctx, Results results) {
        SlimefunItem item = SlimefunItem.getById(BenchMachine.ID);

        if (!(item instanceof BenchMachine machine) || !(item instanceof EnergyNetComponent component)) {
            results.note("charge-api: " + BenchMachine.ID + " not available as BenchMachine/EnergyNetComponent, skipped");
            return;
        }

        int capacity = component.getCapacity();
        List<Location> locations = ctx.grid(MACHINES, 40);

        for (Location l : locations) {
            BlockStorage.addBlockInfo(l, "id", BenchMachine.ID, false);
        }

        Location[] locs = locations.toArray(new Location[0]);
        Config[] data = new Config[MACHINES];

        for (int i = 0; i < MACHINES; i++) {
            data[i] = BlockStorage.getLocationInfo(locs[i]);
        }

        // Variant A: take-charge-hit (write path, charge topped up before every round)
        long[] hit = timedRounds(round -> {
            for (int i = 0; i < MACHINES; i++) {
                machine.takeCharge(locs[i]);
            }
        }, () -> {
            for (int i = 0; i < MACHINES; i++) {
                component.setCharge(locs[i], data[i], capacity);
            }
        });

        results.emit("charge-api", "take-charge-hit", "median_ns_per_call", "ns", (double) Bench.median(hit) / MACHINES);
        results.emit("charge-api", "take-charge-hit", "min_ns_per_call", "ns", (double) Bench.min(hit) / MACHINES);

        // Variant B: take-charge-miss (early return, charge held just below consumption)
        int consumption = machine.getEnergyConsumption();
        int starved = Math.max(0, consumption - 1);
        long[] miss = timedRounds(round -> {
            for (int i = 0; i < MACHINES; i++) {
                machine.takeCharge(locs[i]);
            }
        }, () -> {
            for (int i = 0; i < MACHINES; i++) {
                component.setCharge(locs[i], data[i], starved);
            }
        });

        results.emit("charge-api", "take-charge-miss", "median_ns_per_call", "ns", (double) Bench.median(miss) / MACHINES);
        results.emit("charge-api", "take-charge-miss", "min_ns_per_call", "ns", (double) Bench.min(miss) / MACHINES);

        // Variant C: single-arg removeCharge (delegated form, write path)
        long[] removed = timedRounds(round -> {
            for (int i = 0; i < MACHINES; i++) {
                component.removeCharge(locs[i], 2);
            }
        }, () -> {
            for (int i = 0; i < MACHINES; i++) {
                component.setCharge(locs[i], data[i], capacity);
            }
        });

        results.emit("charge-api", "single-arg-remove", "median_ns_per_call", "ns", (double) Bench.median(removed) / MACHINES);
        results.emit("charge-api", "single-arg-remove", "min_ns_per_call", "ns", (double) Bench.min(removed) / MACHINES);
    }

    /**
     * Like {@link benchmark.Bench#timeRounds(int, int, java.util.function.IntConsumer)}
     * but with an untimed {@code prepare} action run before every round (warmup
     * and timed alike), used to restore the charge state the variant needs.
     */
    private static long[] timedRounds(java.util.function.IntConsumer round, Runnable prepare) {
        for (int i = 0; i < WARMUP; i++) {
            prepare.run();
            round.accept(i);
        }

        long[] samples = new long[ROUNDS];

        for (int i = 0; i < ROUNDS; i++) {
            prepare.run();

            long start = System.nanoTime();
            round.accept(WARMUP + i);
            samples[i] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        return samples;
    }
}

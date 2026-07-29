package benchmark.scenarios;

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
 * Measures the per-component cost of the energy-network settlement write path.
 *
 * <p>The {@link io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet} settlement
 * loop calls {@code setCharge(location, data, charge)} on every component every tick, reusing
 * the {@link Config} it already read. This is the exact path the P2 optimisation turned into
 * a lightweight {@link BlockStorage#updateBlockInfo} write.
 *
 * <p>Two variants are measured:
 * <ul>
 * <li><b>charging</b>: the charge changes every tick, so the write path runs (value + dirty).</li>
 * <li><b>saturated</b>: the charge is already at capacity, so {@code setCharge} short-circuits
 * without writing — the common case for a full capacitor or a satisfied consumer.</li>
 * </ul>
 */
public final class EnergySettlementBench {

    private static final String MACHINE_ID = BenchMachine.ID;
    private static final int COMPONENTS = 1000;
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;

    public void run(BenchContext ctx, Results results) {
        SlimefunItem machineItem = SlimefunItem.getById(MACHINE_ID);

        if (machineItem == null || !(machineItem instanceof EnergyNetComponent component)) {
            results.note("energy-settlement: " + MACHINE_ID + " not an EnergyNetComponent, skipped");
            return;
        }

        int capacity = component.getCapacity();
        List<Location> locations = ctx.grid(COMPONENTS, 80);

        for (Location l : locations) {
            BlockStorage.addBlockInfo(l, "id", MACHINE_ID, false);
        }

        Location[] locs = locations.toArray(new Location[0]);
        Config[] data = new Config[COMPONENTS];

        for (int i = 0; i < COMPONENTS; i++) {
            data[i] = BlockStorage.getLocationInfo(locs[i]);
        }

        // Variant A: charging — the charge changes every round, exercising the write path.
        long[] charging = Bench.timeRounds(WARMUP, ROUNDS, round -> {
            int charge = (round * 7) % capacity;

            for (int i = 0; i < COMPONENTS; i++) {
                component.setCharge(locs[i], data[i], charge);
            }
        });

        results.emit("energy-settlement", "charging-write", "median_ns_per_component", "ns",
            (double) Bench.median(charging) / COMPONENTS);
        results.emit("energy-settlement", "charging-write", "min_ns_per_component", "ns",
            (double) Bench.min(charging) / COMPONENTS);

        // Variant B: saturated — charge is already at capacity, setCharge short-circuits.
        for (int i = 0; i < COMPONENTS; i++) {
            component.setCharge(locs[i], data[i], capacity);
        }

        long[] saturated = Bench.timeRounds(WARMUP, ROUNDS, round -> {
            for (int i = 0; i < COMPONENTS; i++) {
                component.setCharge(locs[i], data[i], capacity);
            }
        });

        results.emit("energy-settlement", "saturated-skip", "median_ns_per_component", "ns",
            (double) Bench.median(saturated) / COMPONENTS);
        results.emit("energy-settlement", "saturated-skip", "min_ns_per_component", "ns",
            (double) Bench.min(saturated) / COMPONENTS);
    }
}

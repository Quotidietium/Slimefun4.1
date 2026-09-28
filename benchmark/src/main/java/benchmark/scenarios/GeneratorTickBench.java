package benchmark.scenarios;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchGenerator;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Measures the per-tick cost of a burning generator: the hot branch of
 * {@code AGenerator#getGeneratedOutput(Location, Config)} that the
 * {@link io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet}
 * calls on every producer on every tick (BlockMenu lookup, processor
 * getOperation, progress bar update, charge-space check).
 *
 * <p>Generators in production are driven by the energy network, not by their
 * own BlockTicker, so the scenario invokes {@code getGeneratedOutput}
 * directly - exactly the call the settlement loop makes, reusing the
 * {@link Config} it already read. The generator's own stored charge stays at
 * zero (produced energy is collected by the net, the generator itself only
 * checks for room), so every call takes the "space available, progress +1"
 * branch, which is the steady state of a net-connected burning generator.
 */
public final class GeneratorTickBench {

    private static final int GENERATORS = 500;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;

    public void run(BenchContext ctx, Results results) {
        SlimefunItem item = SlimefunItem.getById(BenchGenerator.ID);

        if (item == null || item.isDisabled() || !(item instanceof EnergyNetProvider provider)) {
            results.note("generator-tick: " + BenchGenerator.ID + " not available, skipped");
            return;
        }

        List<Location> locations = ctx.grid(GENERATORS, 90);

        for (Location l : locations) {
            BlockStorage.addBlockInfo(l, "id", BenchGenerator.ID, false);

            // Fuel in the first input slot so the first call starts an operation.
            BlockStorage.getInventory(l).replaceExistingItem(19, new ItemStack(Material.COAL, 64));
        }

        Location[] locs = locations.toArray(new Location[0]);
        Config[] data = new Config[GENERATORS];

        for (int i = 0; i < GENERATORS; i++) {
            data[i] = BlockStorage.getLocationInfo(locs[i]);
        }

        // Warm up: the first calls consume fuel and start the FuelOperations;
        // the measured calls stay in the steady "operation in progress" branch.
        for (int w = 0; w < WARMUP; w++) {
            tickAll(provider, locs, data);
        }

        long[] samples = Bench.timeRounds(0, ROUNDS, round -> tickAll(provider, locs, data));

        results.emit("generator-tick", "burning", "median_ns_per_call", "ns",
            (double) Bench.median(samples) / GENERATORS);
        results.emit("generator-tick", "burning", "min_ns_per_call", "ns",
            (double) Bench.min(samples) / GENERATORS);
    }

    private void tickAll(EnergyNetProvider provider, Location[] locs, Config[] data) {
        for (int i = 0; i < locs.length; i++) {
            provider.getGeneratedOutput(locs[i], data[i]);
        }
    }
}

package benchmark.scenarios;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchMachine;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Measures the per-tick cost of idle electric machines (AContainer).
 *
 * <p>An idle machine whose inputs match no recipe re-runs the full recipe
 * scan on every tick in the baseline. The optimized version caches that
 * negative result together with an exact snapshot of the input slots and
 * skips the scan while the snapshot holds.
 *
 * <p>Two variants are measured: empty input slots (the common idle case) and
 * a non-matching item in the input (exercises the snapshot comparison in the
 * optimized version).
 *
 * <p>The machine under test is the benchmark's own {@code BenchMachine}
 * (default Slimefun items are not registered in the MockBukkit unit-test
 * environment). It runs the exact same AContainer tick code as production
 * machines and has a realistic recipe count.
 */
public final class MachineIdleScanBench {

    private static final String MACHINE_ID = BenchMachine.ID;
    private static final int MACHINES = 1000;
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;

    public void run(BenchContext ctx, Results results) {
        SlimefunItem machine = SlimefunItem.getById(MACHINE_ID);

        if (machine == null || machine.isDisabled() || machine.getBlockTicker() == null) {
            results.note("machine-idle-scan: " + MACHINE_ID + " not available (found=" + (machine != null)
                + (machine != null ? ", disabled=" + machine.isDisabled() + ", ticker=" + (machine.getBlockTicker() != null) : "")
                + "), scenario skipped");
            return;
        }

        BlockTicker ticker = machine.getBlockTicker();
        List<Location> locations = ctx.grid(MACHINES, 80);

        for (Location l : locations) {
            BlockStorage.addBlockInfo(l, "id", MACHINE_ID, false);
        }

        Block[] blocks = new Block[MACHINES];
        Config[] data = new Config[MACHINES];

        for (int i = 0; i < MACHINES; i++) {
            blocks[i] = locations.get(i).getBlock();
            data[i] = BlockStorage.getLocationInfo(locations.get(i));
        }

        // --- Variant A: empty inputs (the common idle machine) ---
        long[] empty = Bench.timeRounds(WARMUP, ROUNDS, round -> tickAll(ticker, machine, blocks, data));
        results.emit("machine-idle-scan", "empty-input", "median_ns_per_tick", "ns",
            (double) Bench.median(empty) / MACHINES);
        results.emit("machine-idle-scan", "empty-input", "min_ns_per_tick", "ns",
            (double) Bench.min(empty) / MACHINES);

        // --- Variant B: non-matching junk item in an input slot ---
        for (Location l : locations) {
            BlockStorage.getInventory(l).replaceExistingItem(19, new ItemStack(Material.BEDROCK));
        }

        long[] junk = Bench.timeRounds(WARMUP, ROUNDS, round -> tickAll(ticker, machine, blocks, data));
        results.emit("machine-idle-scan", "junk-input", "median_ns_per_tick", "ns",
            (double) Bench.median(junk) / MACHINES);
        results.emit("machine-idle-scan", "junk-input", "min_ns_per_tick", "ns",
            (double) Bench.min(junk) / MACHINES);
    }

    private void tickAll(BlockTicker ticker, SlimefunItem machine, Block[] blocks, Config[] data) {
        for (int i = 0; i < MACHINES; i++) {
            ticker.tick(blocks[i], machine, data[i]);
        }
    }
}

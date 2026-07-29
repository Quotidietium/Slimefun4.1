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
 * Measures the per-tick cost of machines that are actively processing (as opposed to the
 * idle scan in {@link MachineIdleScanBench}).
 *
 * <p>Each machine is charged and given a valid input, then ticked repeatedly. While a
 * {@code CraftingOperation} is running the tick takes the {@code takeCharge} + progress
 * path; when the operation finishes it re-scans for the next recipe. This is the workload
 * a busy machine room performs, and it exercises the charge write hot path on every tick.
 */
public final class MachineProcessingBench {

    private static final String MACHINE_ID = BenchMachine.ID;
    private static final int MACHINES = 1000;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 7;

    public void run(BenchContext ctx, Results results) {
        SlimefunItem machine = SlimefunItem.getById(MACHINE_ID);

        if (machine == null || machine.isDisabled() || machine.getBlockTicker() == null) {
            results.note("machine-processing: " + MACHINE_ID + " not available, skipped");
            return;
        }

        BlockTicker ticker = machine.getBlockTicker();
        List<Location> locations = ctx.grid(MACHINES, 80);

        for (Location l : locations) {
            BlockStorage.addBlockInfo(l, "id", MACHINE_ID, false);
        }

        Block[] blocks = new Block[MACHINES];
        Config[] data = new Config[MACHINES];

        io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent energyComponent =
            (io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent) machine;

        for (int i = 0; i < MACHINES; i++) {
            Location l = locations.get(i);
            blocks[i] = l.getBlock();
            data[i] = BlockStorage.getLocationInfo(l);

            // A valid input that matches one of BenchMachine's recipes (cobblestone -> stone).
            BlockStorage.getInventory(l).replaceExistingItem(19, new ItemStack(Material.COBBLESTONE, 64));

            // Charge to capacity so takeCharge() succeeds for the whole run.
            energyComponent.setCharge(l, data[i], energyComponent.getCapacity());
        }

        // Warm up: kick off the CraftingOperation so the measured ticks take the active path.
        for (int w = 0; w < WARMUP; w++) {
            tickAll(ticker, machine, blocks, data);
        }

        long[] samples = Bench.timeRounds(0, ROUNDS, round -> tickAll(ticker, machine, blocks, data));

        results.emit("machine-processing", "active", "median_ns_per_tick", "ns",
            (double) Bench.median(samples) / MACHINES);
        results.emit("machine-processing", "active", "min_ns_per_tick", "ns",
            (double) Bench.min(samples) / MACHINES);
    }

    private void tickAll(BlockTicker ticker, SlimefunItem machine, Block[] blocks, Config[] data) {
        for (int i = 0; i < MACHINES; i++) {
            ticker.tick(blocks[i], machine, data[i]);
        }
    }
}

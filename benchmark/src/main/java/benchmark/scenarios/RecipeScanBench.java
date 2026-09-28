package benchmark.scenarios;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchHeftyMachine;
import benchmark.BenchItems;
import benchmark.BenchMachine;
import benchmark.Results;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Measures the full recipe-scan cost of a processing machine.
 *
 * <p>{@code AContainer#tick} caches negative recipe scans, but that cache is
 * invalidated by every input change - which is exactly what hopper-fed or
 * cargo-fed machines experience on every item arrival. Each arrival forces a
 * complete walk of the recipe list. This scenario drives
 * {@code findNextRecipe(BlockMenu)} directly (the protected entry point that
 * bypasses the negative cache) with an input that matches nothing, so every
 * call performs the worst-case full scan.
 *
 * <p>Two variants:
 * <ul>
 * <li><b>junk-10</b>: a 10-recipe machine (Electric Furnace class, {@link BenchMachine}).</li>
 * <li><b>junk-150</b>: a 150-recipe machine (Electric Smeltery class, {@link BenchHeftyMachine}).</li>
 * </ul>
 */
public final class RecipeScanBench {

    private static final int MACHINES = 200;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;

    public void run(BenchContext ctx, Results results) {
        scan(ctx, results, BenchItems.machine, Material.BEDROCK, "junk-10");
        Bench.gcSettle();
        scan(ctx, results, BenchItems.heftyMachine, BenchHeftyMachine.JUNK_MATERIAL, "junk-150");
    }

    private void scan(BenchContext ctx, Results results, BenchMachine machine, Material junk, String variant) {
        if (machine == null) {
            results.note("recipe-scan: machine not registered, skipped " + variant);
            return;
        }

        String id = machine.getId();
        List<Location> locations = ctx.grid(MACHINES, 100);

        BlockMenu[] menus = new BlockMenu[MACHINES];

        for (int i = 0; i < MACHINES; i++) {
            Location l = locations.get(i);
            BlockStorage.addBlockInfo(l, "id", id, false);

            BlockMenu menu = BlockStorage.getInventory(l);
            // A junk stack that matches no recipe: the scan must walk the whole list.
            menu.replaceExistingItem(19, new ItemStack(junk, 64));
            menus[i] = menu;
        }

        for (int w = 0; w < WARMUP; w++) {
            scanAll(machine, menus);
        }

        long[] samples = Bench.timeRounds(0, ROUNDS, round -> scanAll(machine, menus));

        results.emit("recipe-scan", variant, "median_ns_per_scan", "ns",
            (double) Bench.median(samples) / MACHINES);
        results.emit("recipe-scan", variant, "min_ns_per_scan", "ns",
            (double) Bench.min(samples) / MACHINES);
    }

    private void scanAll(BenchMachine machine, BlockMenu[] menus) {
        for (BlockMenu menu : menus) {
            machine.findNextRecipe(menu);
        }
    }
}

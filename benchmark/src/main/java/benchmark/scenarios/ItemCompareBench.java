package benchmark.scenarios;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * Measures the item identity resolution and comparison path:
 * {@code SlimefunItem.getByItem} (template-side PDC resolution, the vanilla
 * fast-negative lookup and fresh-instance misses) and
 * {@code SlimefunUtils.isItemSimilar} (vanilla-vs-template misses and
 * PDC-to-PDC hits).
 *
 * <p>
 * This is the workhorse comparator behind cargo routing, recipe matching and
 * pickup validation. {@value #ITEM_COUNT} items all share one Material
 * ({@link Material#PAPER}), so the vanilla variants exercise the
 * "same Material, no slimefun id" path that every cargo filter pays for
 * vanilla items. Template arguments are stable long-lived instances
 * ({@code SlimefunItemStack#item()} clones), mirroring how cargo filters and
 * recipe caches hold their filter items.
 * </p>
 */
public final class ItemCompareBench {

    private static final int ITEM_COUNT = 40;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int SIM_OPS = 100_000;
    private static final int RESOLVE_OPS = 200_000;

    public void run(BenchContext ctx, Results results) throws Exception {
        ItemGroup group = new ItemGroup(new NamespacedKey(ctx.plugin(), "compare_bench_group"), new ItemStack(Material.CHEST));

        List<ItemStack> templateItems = new ArrayList<>(ITEM_COUNT);

        for (int i = 0; i < ITEM_COUNT; i++) {
            SlimefunItemStack stack = new SlimefunItemStack("BENCH_CITEM_" + i, Material.PAPER, "&bBench Widget " + i, "&8\u21E8 &7Speed: &b" + i);
            new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]).register(Slimefun.instance());
            templateItems.add(stack.item());
        }

        // The tested items, mirroring what an inventory actually holds
        ItemStack vanillaPaper = new ItemStack(Material.PAPER);
        vanillaPaper.editMeta(meta -> meta.setDisplayName("Plain Paper"));

        ItemStack sfClone = templateItems.get(0).clone();

        simVanillaMiss(results, templateItems, vanillaPaper);
        Bench.gcSettle();
        simSfHit(results, templateItems, sfClone);
        Bench.gcSettle();
        resolveTemplate(results, templateItems);
        Bench.gcSettle();
        resolveVanillaShared(results, vanillaPaper);
        Bench.gcSettle();
        resolveVanillaForeign(results);
        Bench.gcSettle();
        resolveFresh(results, templateItems);
    }

    /**
     * The common cargo case: a vanilla item sharing the template's Material
     * (PDC absent) compared against a registered template. Misses on the
     * display name after the full resolution chain.
     */
    private void simVanillaMiss(Results results, List<ItemStack> templateItems, ItemStack vanillaPaper) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < SIM_OPS; i++) {
                SlimefunUtils.isItemSimilar(vanillaPaper, templateItems.get(i % ITEM_COUNT), true);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < SIM_OPS; i++) {
                SlimefunUtils.isItemSimilar(vanillaPaper, templateItems.get(i % ITEM_COUNT), true);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("item-compare", "sim-vanilla-miss", "min_ns_per_compare", "ns", Bench.min(samples) / (double) SIM_OPS);
        results.emit("item-compare", "sim-vanilla-miss", "median_ns_per_compare", "ns", Bench.median(samples) / (double) SIM_OPS);
    }

    /**
     * A real slimefun item (PDC id present) compared against its template:
     * both sides resolve through getByItem, then the ids match.
     */
    private void simSfHit(Results results, List<ItemStack> templateItems, ItemStack sfClone) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < SIM_OPS; i++) {
                SlimefunUtils.isItemSimilar(sfClone, templateItems.get(i % ITEM_COUNT), true);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < SIM_OPS; i++) {
                SlimefunUtils.isItemSimilar(sfClone, templateItems.get(i % ITEM_COUNT), true);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("item-compare", "sim-sf-hit", "min_ns_per_compare", "ns", Bench.min(samples) / (double) SIM_OPS);
        results.emit("item-compare", "sim-sf-hit", "median_ns_per_compare", "ns", Bench.median(samples) / (double) SIM_OPS);
    }

    /**
     * Resolving a stable long-lived template instance - the shape cargo
     * filters and recipe caches hold.
     */
    private void resolveTemplate(Results results, List<ItemStack> templateItems) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < RESOLVE_OPS; i++) {
                SlimefunItem.getByItem(templateItems.get(i % ITEM_COUNT));
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < RESOLVE_OPS; i++) {
                SlimefunItem.getByItem(templateItems.get(i % ITEM_COUNT));
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("item-compare", "resolve-template", "min_ns_per_resolve", "ns", Bench.min(samples) / (double) RESOLVE_OPS);
        results.emit("item-compare", "resolve-template", "median_ns_per_resolve", "ns", Bench.median(samples) / (double) RESOLVE_OPS);
    }

    /**
     * A stable vanilla item whose Material is also used by registered
     * templates: passes the Material fast check, then misses the PDC read.
     */
    private void resolveVanillaShared(Results results, ItemStack vanillaPaper) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < RESOLVE_OPS; i++) {
                SlimefunItem.getByItem(vanillaPaper);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < RESOLVE_OPS; i++) {
                SlimefunItem.getByItem(vanillaPaper);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("item-compare", "resolve-vanilla-shared", "min_ns_per_resolve", "ns", Bench.min(samples) / (double) RESOLVE_OPS);
        results.emit("item-compare", "resolve-vanilla-shared", "median_ns_per_resolve", "ns", Bench.median(samples) / (double) RESOLVE_OPS);
    }

    /**
     * A vanilla item of a Material no template uses: the fast negative path
     * both variants share (guard variant).
     */
    private void resolveVanillaForeign(Results results) {
        ItemStack stone = new ItemStack(Material.STONE);

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < RESOLVE_OPS; i++) {
                SlimefunItem.getByItem(stone);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < RESOLVE_OPS; i++) {
                SlimefunItem.getByItem(stone);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("item-compare", "resolve-vanilla-foreign", "min_ns_per_resolve", "ns", Bench.min(samples) / (double) RESOLVE_OPS);
        results.emit("item-compare", "resolve-vanilla-foreign", "median_ns_per_resolve", "ns", Bench.median(samples) / (double) RESOLVE_OPS);
    }

    /**
     * A fresh stack instance every iteration: the memoization miss path
     * (resolve + map insert), guarding against the cache adding overhead
     * to churn-heavy workloads.
     */
    private void resolveFresh(Results results, List<ItemStack> templateItems) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < RESOLVE_OPS / 10; i++) {
                SlimefunItem.getByItem(templateItems.get(i % ITEM_COUNT).clone());
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < RESOLVE_OPS / 10; i++) {
                SlimefunItem.getByItem(templateItems.get(i % ITEM_COUNT).clone());
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("item-compare", "resolve-fresh", "min_ns_per_resolve", "ns", Bench.min(samples) / (double) (RESOLVE_OPS / 10));
        results.emit("item-compare", "resolve-fresh", "median_ns_per_resolve", "ns", Bench.median(samples) / (double) (RESOLVE_OPS / 10));
    }
}

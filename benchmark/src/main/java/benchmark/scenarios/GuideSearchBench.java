package benchmark.scenarios;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

/**
 * Measures the survival guide search path: {@code openSearch} walking every
 * enabled item ({@code ChatColor.stripColor} + {@code toLowerCase} on the
 * display name per item, plus an {@link SlimefunItem#getItemName()} meta
 * round-trip per item) and the per-item name lookup alone.
 *
 * <p>
 * {@value #ITEM_COUNT} items model a moderate addon server; every
 * {@value #NEEDLE_INTERVAL}-th item is a sparse "needle" match so the hit
 * variant performs the full walk plus result-item builds.
 * </p>
 */
public final class GuideSearchBench {

    private static final int ITEM_COUNT = 500;
    private static final int NEEDLE_INTERVAL = 15;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int MISS_SEARCHES = 200;
    private static final int HIT_SEARCHES = 100;
    private static final int NAME_CALLS = 100_000;

    public void run(BenchContext ctx, Results results) throws Exception {
        Player player = ctx.server().addPlayer("bench_search_user");

        Slimefun.getWorldSettingsService().setEnabled(player.getWorld(), true);

        ItemGroup group = new ItemGroup(new NamespacedKey(ctx.plugin(), "search_bench_group"), new ItemStack(Material.CHEST));
        List<SlimefunItem> items = new ArrayList<>(ITEM_COUNT);

        for (int i = 0; i < ITEM_COUNT; i++) {
            String name = (i % NEEDLE_INTERVAL == 0 ? "&bNeedle Widget " : "&7Bench Widget ") + i;
            SlimefunItemStack stack = new SlimefunItemStack("BENCH_SITEM_" + i, Material.PAPER, name, "&8\u21E8 &7Speed: &b1");
            SlimefunItem item = new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]);
            item.register(Slimefun.instance());
            item.load();
            items.add(item);
        }

        PlayerData data = new PlayerData(new HashSet<>(), new HashMap<>(), new HashSet<>());
        PlayerProfile profile = newProfile(player, data);

        SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(SlimefunGuideMode.SURVIVAL_MODE);

        searchMiss(results, guide, profile);
        Bench.gcSettle();
        searchHit(results, guide, profile);
        Bench.gcSettle();
        itemName(results, items);
    }

    /**
     * Constructs a {@link PlayerProfile} through the protected constructor -
     * no async loading, exactly the state we set up.
     */
    private PlayerProfile newProfile(OfflinePlayer player, PlayerData data) throws Exception {
        Constructor<PlayerProfile> constructor = PlayerProfile.class.getDeclaredConstructor(OfflinePlayer.class, PlayerData.class);
        constructor.setAccessible(true);
        return constructor.newInstance(player, data);
    }

    /**
     * The full-registry walk with zero results: every enabled item pays the
     * name normalization, no result items are built. The faithful
     * production-shaped signal (no ItemMeta ops beyond the name read).
     */
    private void searchMiss(Results results, SlimefunGuideImplementation guide, PlayerProfile profile) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < MISS_SEARCHES; i++) {
                guide.openSearch(profile, "zzz_no_match", false);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < MISS_SEARCHES; i++) {
                guide.openSearch(profile, "zzz_no_match", false);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-search", "search-miss", "min_ns_per_search", "ns", Bench.min(samples) / (double) MISS_SEARCHES);
        results.emit("guide-search", "search-miss", "median_ns_per_search", "ns", Bench.median(samples) / (double) MISS_SEARCHES);
    }

    /**
     * The full walk plus result-item builds: the sparse "needle" matches are
     * spread across the whole registry, so the walk never terminates early.
     */
    private void searchHit(Results results, SlimefunGuideImplementation guide, PlayerProfile profile) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < HIT_SEARCHES; i++) {
                guide.openSearch(profile, "needle", false);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < HIT_SEARCHES; i++) {
                guide.openSearch(profile, "needle", false);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-search", "search-hit", "min_ns_per_search", "ns", Bench.min(samples) / (double) HIT_SEARCHES);
        results.emit("guide-search", "search-hit", "median_ns_per_search", "ns", Bench.median(samples) / (double) HIT_SEARCHES);
    }

    /**
     * The per-item display name lookup alone (a meta round-trip per call on
     * the unoptimized path).
     */
    private void itemName(Results results, List<SlimefunItem> items) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < NAME_CALLS; i++) {
                items.get(i % ITEM_COUNT).getItemName();
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < NAME_CALLS; i++) {
                items.get(i % ITEM_COUNT).getItemName();
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-search", "item-name", "min_ns_per_call", "ns", Bench.min(samples) / (double) NAME_CALLS);
        results.emit("guide-search", "item-name", "median_ns_per_call", "ns", Bench.median(samples) / (double) NAME_CALLS);
    }
}

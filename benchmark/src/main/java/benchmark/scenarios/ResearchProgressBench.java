package benchmark.scenarios;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

/**
 * Measures the research progression bookkeeping of {@link PlayerProfile}:
 * {@code setResearched} (the per-unlock full path - ownership flip
 * detection, progress counting twice, rank-title comparison before and
 * after), {@code getTitle} (the rank computation that walks every registry
 * research and its items) and the per-interaction {@code hasUnlocked} gate.
 *
 * <p>The unit-test environment does not load the real research setup, so the
 * scenario registers its own: {@value #RESEARCH_COUNT} researches with
 * {@value #ITEMS_PER_RESEARCH} enabled items each (roughly the shape of a
 * real server), four rank titles, and a profile that has unlocked 80% of
 * them. The profile is constructed directly (reflection on the protected
 * constructor) so no async loading is involved.
 */
public final class ResearchProgressBench {

    private static final int RESEARCH_COUNT = 100;
    private static final int ITEMS_PER_RESEARCH = 5;
    private static final int UNLOCKED_FRACTION = 4;

    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int UNLOCK_CALLS = 2000;
    private static final int TITLE_CALLS = 5000;
    private static final int GATE_CALLS = 200_000;

    public void run(BenchContext ctx, Results results) throws Exception {
        ItemGroup itemGroup = new ItemGroup(new NamespacedKey(ctx.plugin(), "research_bench"), new ItemStack(Material.NETHER_STAR));

        List<Research> researches = new ArrayList<>(RESEARCH_COUNT);

        for (int r = 0; r < RESEARCH_COUNT; r++) {
            Research research = new Research(new NamespacedKey(ctx.plugin(), "bench_r_" + r), 900_000 + r, "Bench Research " + r, r % 20);
            SlimefunItem[] items = new SlimefunItem[ITEMS_PER_RESEARCH];

            for (int i = 0; i < ITEMS_PER_RESEARCH; i++) {
                SlimefunItem item = new SlimefunItem(itemGroup, new SlimefunItemStack("BENCH_RITEM_" + r + '_' + i, Material.PAPER, "Bench Item"), RecipeType.NULL, new ItemStack[9]);
                item.register(Slimefun.instance());
                items[i] = item;
            }

            research.addItems(items);
            researches.add(research);
        }

        // The registry list is what getTitle()/countNonEmptyResearches walk
        Slimefun.getRegistry().getResearches().addAll(researches);

        if (Slimefun.getRegistry().getResearchRanks().isEmpty()) {
            Slimefun.getRegistry().getResearchRanks().addAll(List.of("见习", "学徒", "工匠", "工程师", "大师"));
        }

        // 80% of the researches already unlocked - the end-game player shape
        Set<Research> unlocked = new HashSet<>();

        for (int r = 0; r < RESEARCH_COUNT * UNLOCKED_FRACTION / (UNLOCKED_FRACTION + 1); r++) {
            unlocked.add(researches.get(r));
        }

        OfflinePlayer player = ctx.server().addPlayer("bench_researcher");
        PlayerData data = new PlayerData(unlocked, new HashMap<>(), new HashSet<>());
        PlayerProfile profile = newProfile(player, data);

        unlockCycle(results, researches, profile);
        Bench.gcSettle();
        titleLoop(results, profile);
        Bench.gcSettle();
        gate(results, researches, profile);
    }

    /**
     * Constructs a {@link PlayerProfile} through the protected constructor -
     * no async loading, no storage round-trip, exactly the state we set up.
     */
    private PlayerProfile newProfile(OfflinePlayer player, PlayerData data) throws Exception {
        Constructor<PlayerProfile> constructor = PlayerProfile.class.getDeclaredConstructor(OfflinePlayer.class, PlayerData.class);
        constructor.setAccessible(true);
        return constructor.newInstance(player, data);
    }

    /**
     * The full per-unlock bookkeeping path. Toggling ownership on and off
     * keeps every call a genuine flip, so both progress counts and both
     * rank-title comparisons run on every iteration.
     */
    private void unlockCycle(Results results, List<Research> researches, PlayerProfile profile) {
        Research cursor = researches.get(0);
        boolean unlock = false;

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < UNLOCK_CALLS; i++) {
                profile.setResearched(cursor, unlock = !unlock);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < UNLOCK_CALLS; i++) {
                profile.setResearched(cursor, unlock = !unlock);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("research-progress", "unlock-cycle", "min_ns_per_unlock", "ns", Bench.min(samples) / (double) UNLOCK_CALLS);
        results.emit("research-progress", "unlock-cycle", "median_ns_per_unlock", "ns", Bench.median(samples) / (double) UNLOCK_CALLS);
    }

    /**
     * The rank-title computation alone (walks every registry research and
     * the player's unlocked set - the part the per-unlock path pays twice).
     */
    private void titleLoop(Results results, PlayerProfile profile) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < TITLE_CALLS; i++) {
                profile.getTitle();
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < TITLE_CALLS; i++) {
                profile.getTitle();
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("research-progress", "title", "min_ns_per_call", "ns", Bench.min(samples) / (double) TITLE_CALLS);
        results.emit("research-progress", "title", "median_ns_per_call", "ns", Bench.median(samples) / (double) TITLE_CALLS);
    }

    /**
     * The per-interaction research gate (canUse-adjacent): flat guard,
     * expected to stay unchanged.
     */
    private void gate(Results results, List<Research> researches, PlayerProfile profile) {
        Research unlocked = researches.get(0);
        Research locked = researches.get(RESEARCH_COUNT - 1);

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < GATE_CALLS; i++) {
                profile.hasUnlocked(i % 2 == 0 ? unlocked : locked);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < GATE_CALLS; i++) {
                profile.hasUnlocked(i % 2 == 0 ? unlocked : locked);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("research-progress", "has-unlocked", "min_ns_per_call", "ns", Bench.min(samples) / (double) GATE_CALLS);
        results.emit("research-progress", "has-unlocked", "median_ns_per_call", "ns", Bench.median(samples) / (double) GATE_CALLS);
    }
}

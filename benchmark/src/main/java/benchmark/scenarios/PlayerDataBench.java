package benchmark.scenarios;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.backend.legacy.LegacyStorage;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

/**
 * Measures the legacy player-data persistence path: {@link LegacyStorage}'s
 * save (per-save config re-parse, research rewrite loop, backpack slot
 * serialization, atomic file write) and load (config parse, research section
 * walk, backpack content deserialization).
 *
 * <p>
 * Three data shapes cover the hot combinations: a research-heavy profile
 * ({@value #HEAVY_UNLOCKED} of {@value #REGISTRY_RESEARCHES} researches
 * unlocked, the realistic end-game shape), a research-sparse profile
 * ({@value #SPARSE_UNLOCKED} unlocked - early-game, where the registry walk
 * dwarfs the writes), and a backpack profile ({@value #BACKPACK_COUNT} x
 * {@value #BACKPACK_SIZE} slots, {@value #BACKPACK_FILL} filled each). Save
 * variants write through the snapshot path the async auto-save uses; load
 * variants read the files the matching save variant primed during setup, so
 * the parsed content is identical on every iteration. A scale extension
 * grows the registry to {@value #SCALE_REGISTRY_RESEARCHES} researches
 * (addon-heavy shape) afterwards: the per-research config walks scale
 * linearly with the registry, which lifts them above the disk-noise floor.
 * </p>
 */
public final class PlayerDataBench {

    private static final int REGISTRY_RESEARCHES = 250;
    private static final int SCALE_REGISTRY_RESEARCHES = 2500;
    private static final int HEAVY_UNLOCKED = 230;
    private static final int SPARSE_UNLOCKED = 10;
    private static final int BACKPACK_COUNT = 3;
    private static final int BACKPACK_SIZE = 54;
    private static final int BACKPACK_FILL = 36;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int SAVE_OPS = 40;
    private static final int LOAD_OPS = 40;

    public void run(BenchContext ctx, Results results) throws Exception {
        List<Research> researches = new ArrayList<>(REGISTRY_RESEARCHES);

        for (int r = 0; r < REGISTRY_RESEARCHES; r++) {
            researches.add(new Research(new NamespacedKey(ctx.plugin(), "bench_pdata_" + r), 900_000 + r, "Bench Research " + r, r % 20));
        }

        // The registry list is what both the save and load research loops walk.
        Slimefun.getRegistry().getResearches().addAll(researches);

        LegacyStorage storage = new LegacyStorage();

        UUID uuidHeavy = UUID.nameUUIDFromBytes("r13-heavy".getBytes(StandardCharsets.UTF_8));
        UUID uuidSparse = UUID.nameUUIDFromBytes("r13-sparse".getBytes(StandardCharsets.UTF_8));
        UUID uuidBackpacks = UUID.nameUUIDFromBytes("r13-backpacks".getBytes(StandardCharsets.UTF_8));

        PlayerData dataHeavy = new PlayerData(unlocked(researches, HEAVY_UNLOCKED), new HashMap<>(), new HashSet<>());
        PlayerData dataSparse = new PlayerData(unlocked(researches, SPARSE_UNLOCKED), new HashMap<>(), new HashSet<>());

        Map<Integer, ItemStack[]> snapshots = new HashMap<>();
        Map<Integer, PlayerBackpack> backpacks = new HashMap<>();

        for (int b = 1; b <= BACKPACK_COUNT; b++) {
            HashMap<Integer, ItemStack> contents = new HashMap<>();
            ItemStack[] snapshot = new ItemStack[BACKPACK_SIZE];

            for (int i = 0; i < BACKPACK_FILL; i++) {
                ItemStack item = new ItemStack(Material.STONE, (i % 64) + 1);
                snapshot[i] = item;
                contents.put(i, item);
            }

            backpacks.put(b, PlayerBackpack.load(uuidBackpacks, b, BACKPACK_SIZE, contents));
            snapshots.put(b, snapshot);
        }

        PlayerData dataBackpacks = new PlayerData(unlocked(researches, SPARSE_UNLOCKED), backpacks, new HashSet<>());

        // Prime each file once (untimed) so the load variants always parse the
        // exact content the matching save variant produces.
        storage.savePlayerData(uuidHeavy, dataHeavy, null);
        storage.savePlayerData(uuidSparse, dataSparse, null);
        storage.savePlayerData(uuidBackpacks, dataBackpacks, snapshots);

        saveVariant(results, storage, "save-research-heavy", uuidHeavy, dataHeavy, null);
        Bench.gcSettle();
        saveVariant(results, storage, "save-research-sparse", uuidSparse, dataSparse, null);
        Bench.gcSettle();
        saveVariant(results, storage, "save-backpacks", uuidBackpacks, dataBackpacks, snapshots);
        Bench.gcSettle();
        loadVariant(results, storage, "load-research", uuidHeavy);
        Bench.gcSettle();
        loadVariant(results, storage, "load-backpacks", uuidBackpacks);
        Bench.gcSettle();

        /*
         * Scale extension: model an addon-heavy server with a very large research
         * registry. The per-research config walks being measured scale linearly
         * with the registry size, so this shape lifts the walk cost above the
         * disk-I/O noise floor that dominates the realistic shapes above.
         */
        List<Research> scaleResearches = new ArrayList<>(SCALE_REGISTRY_RESEARCHES - REGISTRY_RESEARCHES);

        for (int r = REGISTRY_RESEARCHES; r < SCALE_REGISTRY_RESEARCHES; r++) {
            scaleResearches.add(new Research(new NamespacedKey(ctx.plugin(), "bench_pdata_" + r), 900_000 + r, "Bench Research " + r, r % 20));
        }

        Slimefun.getRegistry().getResearches().addAll(scaleResearches);

        saveVariant(results, storage, "save-scale-sparse", uuidSparse, dataSparse, null);
        Bench.gcSettle();
        loadVariant(results, storage, "load-scale-sparse", uuidSparse);
    }

    private static Set<Research> unlocked(List<Research> researches, int count) {
        Set<Research> unlocked = new HashSet<>();

        for (int i = 0; i < count; i++) {
            unlocked.add(researches.get(i));
        }

        return unlocked;
    }

    private void saveVariant(Results results, LegacyStorage storage, String variant, UUID uuid, PlayerData data, Map<Integer, ItemStack[]> snapshots) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < SAVE_OPS; i++) {
                storage.savePlayerData(uuid, data, snapshots);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < SAVE_OPS; i++) {
                storage.savePlayerData(uuid, data, snapshots);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("player-data", variant, "min_us_per_save", "us", Bench.min(samples) / 1000.0 / SAVE_OPS);
        results.emit("player-data", variant, "median_us_per_save", "us", Bench.median(samples) / 1000.0 / SAVE_OPS);
    }

    private void loadVariant(Results results, LegacyStorage storage, String variant, UUID uuid) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LOAD_OPS; i++) {
                storage.loadPlayerData(uuid);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < LOAD_OPS; i++) {
                storage.loadPlayerData(uuid);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("player-data", variant, "min_us_per_load", "us", Bench.min(samples) / 1000.0 / LOAD_OPS);
        results.emit("player-data", variant, "median_us_per_load", "us", Bench.median(samples) / 1000.0 / LOAD_OPS);
    }
}

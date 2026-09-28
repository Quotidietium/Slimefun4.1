package benchmark.scenarios;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchProtectionModule;
import benchmark.Results;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.BenchCargoRoute;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Measures the protection-query cost of a cargo tick when a region-protection
 * module is installed: {@code CargoNetworkTask} asks
 * {@code ProtectionManager#hasPermission} for every (input, output) pair it
 * touches - {@value #INPUTS} inputs routed through {@value #OUTPUTS} shared
 * outputs pay {@value #INPUTS}×(1+{@value #OUTPUTS}) module queries per tick
 * without caching.
 *
 * <p>Variants:
 * <ul>
 * <li><b>prot-bounce-1owner</b>: every node belongs to one player (the common
 * single-base cargo setup) - all queries collapse to 1+{@value #OUTPUTS}
 * distinct (owner, target) pairs.</li>
 * <li><b>prot-bounce-4owners</b>: four players share the network - 4× the
 * distinct pairs, still far below the uncached query count.</li>
 * <li><b>prot-bounce-*-heavy</b>: same layouts, but a second module with
 * {@value #HEAVY_REGIONS} regions is registered first - every query then
 * walks a plot-server-scale region list, the cost shape under which the
 * per-pair cache pays off most.</li>
 * <li><b>prot-bounce-*-xheavy</b>: a third module raises the walk to
 * {@value #XHEAVY_REGIONS} regions (~µs per query, L2-spilling footprint):
 * a stress point chosen so the eliminated-query saving resolves well above
 * the run-to-run noise floor, validating the linear scaling law.</li>
 * </ul>
 *
 * <p>The item flow is the mixed-bounce shape (withdrawn item finds no
 * accepting output and returns), so both query sites - the input container
 * check and every output check - run on every tick.
 *
 * <p>A <b>protection-query</b> micro measurement times the registered module
 * stack directly (ns per {@code hasPermission} call) before and after the
 * heavy module joins, which converts the eliminated query count into an
 * expected per-tick saving for whatever module a real server runs.
 *
 * <p>This scenario registers {@link BenchProtectionModule}s on the global
 * {@code ProtectionManager}, which cannot be unregistered - therefore it runs
 * <strong>last</strong> in the bench, after every other scenario.
 */
public final class CargoProtectionBench {

    private static final int INPUTS = 16;
    private static final int OUTPUTS = 8;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int HEAVY_REGIONS = 400;
    private static final int XHEAVY_REGIONS = 3000;
    private static final int MICRO_CALLS = 100_000;

    private static final Material MOVED = Material.DIAMOND;
    private static final Material FILLER = Material.STONE;

    public void run(BenchContext ctx, Results results) {
        UUID[] owners = new UUID[4];

        for (int i = 0; i < owners.length; i++) {
            owners[i] = UUID.nameUUIDFromBytes(("bench-owner-" + i).getBytes());
        }

        /*
         * The ProtectionManager is normally created by a scheduled task on the
         * first server tick, which this bench never pumps - inject it the same
         * way IntegrationsManager#onServerStart would (environment setup, not
         * code under test).
         */
        try {
            java.lang.reflect.Field field = Slimefun.getIntegrations().getClass().getDeclaredField("protectionManager");
            field.setAccessible(true);
            field.set(Slimefun.getIntegrations(), new io.github.bakedlibs.dough.protection.ProtectionManager(ctx.plugin()));
        } catch (ReflectiveOperationException x) {
            throw new IllegalStateException("Cannot inject the ProtectionManager", x);
        }

        Slimefun.getProtectionManager().registerModule(ctx.server().getPluginManager(), "Slimefun",
            plugin -> new BenchProtectionModule(plugin, owners));

        bounce(ctx, results, 1, owners, "prot-bounce-1owner", 700);
        Bench.gcSettle();
        bounce(ctx, results, 4, owners, "prot-bounce-4owners", 800);

        // Cost of one module query with only the cheap module registered
        queryCost(ctx, results, owners, "cheap-module");

        /*
         * The module stack now also carries the heavy module; every following
         * query walks its full region list first (it cannot be unregistered,
         * exactly like a real protection plugin being present).
         */
        Slimefun.getProtectionManager().registerModule(ctx.server().getPluginManager(), "Slimefun",
            plugin -> new BenchProtectionModule(plugin, HEAVY_REGIONS, owners));

        queryCost(ctx, results, owners, "heavy-module-stack");

        Bench.gcSettle();
        bounce(ctx, results, 1, owners, "prot-bounce-1owner-heavy", 900);
        Bench.gcSettle();
        bounce(ctx, results, 4, owners, "prot-bounce-4owners-heavy", 1000);

        Slimefun.getProtectionManager().registerModule(ctx.server().getPluginManager(), "Slimefun",
            plugin -> new BenchProtectionModule(plugin, XHEAVY_REGIONS, owners));

        queryCost(ctx, results, owners, "xheavy-module-stack");

        Bench.gcSettle();
        bounce(ctx, results, 1, owners, "prot-bounce-1owner-xheavy", 1100);
        Bench.gcSettle();
        bounce(ctx, results, 4, owners, "prot-bounce-4owners-xheavy", 1200);
    }

    /**
     * Times the registered module stack directly: nanoseconds per
     * {@code ProtectionManager#hasPermission} call. Together with the number
     * of eliminated queries this yields the expected per-tick saving for any
     * module cost a real server might run.
     */
    private void queryCost(BenchContext ctx, Results results, UUID[] owners, String variant) {
        OfflinePlayer owner = ctx.server().getOfflinePlayer(owners[0]);
        Block target = ctx.world().getBlockAt(7, 100, 705);
        Interaction action = Interaction.INTERACT_BLOCK;

        for (int i = 0; i < 20_000; i++) {
            Slimefun.getProtectionManager().hasPermission(owner, target, action);
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < MICRO_CALLS; i++) {
                Slimefun.getProtectionManager().hasPermission(owner, target, action);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("protection-query", variant, "min_ns_per_query", "ns", Bench.min(samples) / (double) MICRO_CALLS);
        results.emit("protection-query", variant, "median_ns_per_query", "ns", Bench.median(samples) / (double) MICRO_CALLS);
    }

    private void bounce(BenchContext ctx, Results results, int ownerCount, UUID[] owners, String variant, int zBase) {
        World world = ctx.world();
        CargoNet network = BenchCargoRoute.mockNetwork(world);

        Map<Location, Integer> inputs = new HashMap<>();
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>(OUTPUTS);
        outputs.put(0, outputNodes);

        Inventory[] inputChests = new Inventory[INPUTS];
        Inventory[] outputChests = new Inventory[OUTPUTS];

        for (int i = 0; i < INPUTS; i++) {
            Location node = new Location(world, i, 100, zBase);
            world.getBlockAt(node).setType(Material.STONE);
            BlockStorage.addBlockInfo(node, "owner", owners[i % ownerCount].toString(), false);
            Block chest = world.getBlockAt(i, 100, zBase + 1);
            chest.setType(Material.CHEST);
            inputChests[i] = ((InventoryHolder) chest.getState()).getInventory();
            BenchCargoRoute.attach(network, node, chest);
            inputs.put(node, 0);
        }

        for (int o = 0; o < OUTPUTS; o++) {
            Location node = new Location(world, o, 100, zBase + 10);
            world.getBlockAt(node).setType(Material.STONE);
            BlockStorage.addBlockInfo(node, "owner", owners[o % ownerCount].toString(), false);
            Block chest = world.getBlockAt(o, 100, zBase + 11);
            chest.setType(Material.CHEST);
            outputChests[o] = ((InventoryHolder) chest.getState()).getInventory();
            BenchCargoRoute.attach(network, node, chest);
            outputNodes.add(node);
        }

        Runnable reset = () -> {
            for (int i = 0; i < INPUTS; i++) {
                inputChests[i].setItem(0, new ItemStack(MOVED, 16));
            }

            for (int o = 0; o < OUTPUTS; o++) {
                for (int slot = 0; slot < 27; slot++) {
                    outputChests[o].setItem(slot, new ItemStack(FILLER, 1));
                }
            }
        };

        for (int w = 0; w < WARMUP; w++) {
            reset.run();
            BenchCargoRoute.runNewTask(network, inputs, outputs);
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            reset.run();
            long start = System.nanoTime();
            BenchCargoRoute.runNewTask(network, inputs, outputs);
            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("cargo-protection", variant, "median_ns_per_tick", "ns", (double) Bench.median(samples));
        results.emit("cargo-protection", variant, "min_ns_per_tick", "ns", (double) Bench.min(samples));
        results.note("cargo-protection " + variant + ": module queries per tick without caching = " + (INPUTS * (1 + OUTPUTS)));
    }
}

package benchmark.scenarios;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchItems;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.BenchCargoRoute;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Measures a full cargo tick ({@code CargoNetworkTask}) on vanilla-chest and
 * machine-backed networks.
 *
 * <p>Each variant builds one network of {@value #INPUTS} input nodes and routes
 * them through the output map exactly like {@code CargoNet#tick} would, then
 * drives the real task (construction + {@code run()}) per round. State that the
 * tick mutates is reset outside the timed region between rounds.
 *
 * <p>Variants:
 * <ul>
 * <li><b>idle</b>: input chests empty - withdraw misses immediately. The
 * overhead floor of one tick (task shell, attached-block + owner resolution,
 * withdraw miss).</li>
 * <li><b>happy-merge</b>: one private output per input, containing a partial
 * matching stack - one withdraw, one merge insert. The common "everything
 * works" path.</li>
 * <li><b>mixed-bounce</b>: 8 shared outputs whose 27 slots all hold partial
 * stacks of other materials. Every insert attempt scans every occupied,
 * non-full slot and pays a full meta comparison per slot before bouncing -
 * the dominant cost for cargo networks feeding mixed storage.</li>
 * <li><b>mixed-bounce-smartfill</b>: like mixed-bounce but with full filler
 * stacks and smart-fill enabled, which disables the full-stack skip and makes
 * every occupied slot pay the meta comparison.</li>
 * <li><b>machine-bounce</b>: like mixed-bounce but the outputs are
 * Slimefun machines ({@code DirtyChestMenu} path) with filled input slots.</li>
 * </ul>
 */
public final class CargoTransportBench {

    private static final int INPUTS = 16;
    private static final int OUTPUTS = 8;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;

    /** The material moved from input to output chests. */
    private static final Material MOVED = Material.DIAMOND;

    /** 27 distinct, stackable filler materials (a chest's worth), none equal to MOVED. */
    private static final Material[] FILLERS = {
        Material.STONE, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.COBBLESTONE,
        Material.DIRT, Material.GRASS_BLOCK, Material.SAND, Material.GRAVEL, Material.OAK_LOG,
        Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.IRON_ORE, Material.COAL_ORE, Material.COPPER_ORE,
        Material.GLASS, Material.BRICK, Material.SANDSTONE, Material.SNOW_BLOCK, Material.CLAY,
        Material.TERRACOTTA, Material.WHITE_WOOL, Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,
        Material.YELLOW_WOOL, Material.BLACK_WOOL
    };

    public void run(BenchContext ctx, Results results) {
        idle(ctx, results);
        Bench.gcSettle();
        happyMerge(ctx, results);
        Bench.gcSettle();
        bounce(ctx, results, false, "mixed-bounce");
        Bench.gcSettle();
        bounce(ctx, results, true, "mixed-bounce-smartfill");
        Bench.gcSettle();
        machineBounce(ctx, results);
    }

    /**
     * One variant's fixed layout: the mocked network with attached containers
     * plus the routing maps and the inventories to reset between rounds.
     */
    private record Layout(CargoNet network, Map<Location, Integer> inputs, Map<Integer, List<Location>> outputs,
            Inventory[] inputChests, Inventory[] outputChests) {
    }

    private void idle(BenchContext ctx, Results results) {
        Layout layout = buildChestLayout(ctx, 100, 0, 1, OUTPUTS);

        // Nothing to reset: the withdraw misses leave the chests untouched.
        timed(results, "idle", () -> {}, layout);
    }

    private void happyMerge(BenchContext ctx, Results results) {
        Layout layout = buildChestLayout(ctx, 100, 100, INPUTS, INPUTS);

        Runnable reset = () -> {
            for (int i = 0; i < INPUTS; i++) {
                layout.inputChests()[i].setItem(0, new ItemStack(MOVED, 16));

                // Each channel is private, so output i belongs to input i.
                layout.outputChests()[i].setItem(0, new ItemStack(MOVED, 8));
            }
        };

        timed(results, "happy-merge", reset, layout);
    }

    private void bounce(BenchContext ctx, Results results, boolean smartFill, String variant) {
        int fillerAmount = smartFill ? 64 : 1;
        Layout layout = buildChestLayout(ctx, 100, 200, 1, OUTPUTS);

        if (smartFill) {
            // distributeItem reads smart-fill from the input node's block data.
            for (Location input : layout.inputs().keySet()) {
                BlockStorage.addBlockInfo(input, "smart-fill", "true", false);
            }
        }

        Runnable reset = () -> {
            for (int i = 0; i < INPUTS; i++) {
                layout.inputChests()[i].setItem(0, new ItemStack(MOVED, 16));
            }

            for (int o = 0; o < OUTPUTS; o++) {
                for (int slot = 0; slot < FILLERS.length; slot++) {
                    layout.outputChests()[o].setItem(slot, new ItemStack(FILLERS[slot], fillerAmount));
                }
            }
        };

        timed(results, variant, reset, layout);
    }

    private void machineBounce(BenchContext ctx, Results results) {
        if (BenchItems.machine == null) {
            results.note("cargo-route: machine not registered, skipped machine-bounce");
            return;
        }

        World world = ctx.world();
        CargoNet network = BenchCargoRoute.mockNetwork(world);
        Map<Location, Integer> inputs = new HashMap<>();
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>(OUTPUTS);
        outputs.put(0, outputNodes);

        Inventory[] inputChests = new Inventory[INPUTS];

        for (int i = 0; i < INPUTS; i++) {
            Location node = new Location(world, i, 100, 300);
            world.getBlockAt(node).setType(Material.STONE);
            Block chest = world.getBlockAt(i, 100, 301);
            chest.setType(Material.CHEST);
            inputChests[i] = ((InventoryHolder) chest.getState()).getInventory();
            BenchCargoRoute.attach(network, node, chest);
            inputs.put(node, 0);
        }

        BlockMenu[] menus = new BlockMenu[OUTPUTS];

        for (int o = 0; o < OUTPUTS; o++) {
            Location node = new Location(world, o, 100, 310);
            world.getBlockAt(node).setType(Material.STONE);
            Location machineLoc = new Location(world, o, 100, 311);
            Block machineBlock = world.getBlockAt(machineLoc);
            machineBlock.setType(Material.DISPENSER);
            BlockStorage.addBlockInfo(machineLoc, "id", BenchItems.machine.getId(), false);
            menus[o] = BlockStorage.getInventory(machineLoc);
            BenchCargoRoute.attach(network, node, machineBlock);
            outputNodes.add(node);
        }

        Runnable reset = () -> {
            for (int i = 0; i < INPUTS; i++) {
                inputChests[i].setItem(0, new ItemStack(MOVED, 16));
            }

            for (BlockMenu menu : menus) {
                menu.replaceExistingItem(19, new ItemStack(FILLERS[0], 1));
                menu.replaceExistingItem(20, new ItemStack(FILLERS[1], 1));
            }
        };

        timed(results, "machine-bounce", reset, new Layout(network, inputs, outputs, inputChests, new Inventory[0]));
    }

    /**
     * Builds the shared chest layout used by idle/happy-merge/bounce: input
     * nodes along one row with attached chests behind them, output nodes along
     * a parallel row.
     *
     * @param zBase
     *            Z offset of the input row (variants use disjoint bands)
     * @param channels
     *            How many channels to spread the outputs over (1 = all outputs
     *            shared by every input, INPUTS = one private output per input)
     * @param outputCount
     *            How many output nodes (and chests) to build
     */
    private Layout buildChestLayout(BenchContext ctx, int y, int zBase, int channels, int outputCount) {
        World world = ctx.world();
        CargoNet network = BenchCargoRoute.mockNetwork(world);

        Map<Location, Integer> inputs = new HashMap<>();
        Map<Integer, List<Location>> outputs = new HashMap<>();

        Inventory[] inputChests = new Inventory[INPUTS];
        Inventory[] outputChests = new Inventory[outputCount];

        for (int i = 0; i < INPUTS; i++) {
            Location node = new Location(world, i, y, zBase);
            world.getBlockAt(node).setType(Material.STONE);
            Block chest = world.getBlockAt(i, y, zBase + 1);
            chest.setType(Material.CHEST);
            inputChests[i] = ((InventoryHolder) chest.getState()).getInventory();
            BenchCargoRoute.attach(network, node, chest);
            inputs.put(node, i % channels);
        }

        for (int o = 0; o < outputCount; o++) {
            Location node = new Location(world, o, y, zBase + 10);
            world.getBlockAt(node).setType(Material.STONE);
            Block chest = world.getBlockAt(o, y, zBase + 11);
            chest.setType(Material.CHEST);
            outputChests[o] = ((InventoryHolder) chest.getState()).getInventory();
            BenchCargoRoute.attach(network, node, chest);
            outputs.computeIfAbsent(o % channels, key -> new ArrayList<>()).add(node);
        }

        return new Layout(network, inputs, outputs, inputChests, outputChests);
    }

    private void timed(Results results, String variant, Runnable reset, Layout layout) {
        for (int w = 0; w < WARMUP; w++) {
            reset.run();
            BenchCargoRoute.runNewTask(layout.network(), layout.inputs(), layout.outputs());
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            reset.run();
            long start = System.nanoTime();
            BenchCargoRoute.runNewTask(layout.network(), layout.inputs(), layout.outputs());
            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("cargo-route", variant, "median_ns_per_tick", "ns", (double) Bench.median(samples));
        results.emit("cargo-route", variant, "min_ns_per_tick", "ns", (double) Bench.min(samples));
    }
}

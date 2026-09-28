package benchmark;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;

/**
 * Registers the two synthetic Slimefun items used by the benchmark:
 *
 * <ul>
 * <li>{@link #DUMMY_ID} - a plain item without ticker or inventory, used to
 * isolate the raw BlockStorage write path.</li>
 * <li>{@link #TICKER_ID} - an item with a trivial asynchronous BlockTicker,
 * used to measure ticker dispatch overhead.</li>
 * </ul>
 */
public final class BenchItems {

    public static final String DUMMY_ID = "BENCH_DUMMY";
    public static final String TICKER_ID = "BENCH_TICKER";

    /**
     * Incremented by the bench ticker's body. Used by the ticker scenario to
     * verify that blocks are actually being ticked (an empty measurement
     * would otherwise look like a very fast one).
     */
    public static final java.util.concurrent.atomic.AtomicLong TICK_COUNTER = new java.util.concurrent.atomic.AtomicLong();

    /**
     * The registered machine instances. The {@code recipe-scan} scenario
     * needs the concrete types to call their widened
     * {@code findNextRecipe(BlockMenu)} bridges.
     */
    public static BenchMachine machine;
    public static BenchGenerator generator;
    public static BenchHeftyMachine heftyMachine;

    private BenchItems() {}

    public static void register(Plugin plugin) {
        ItemGroup itemGroup = new ItemGroup(new NamespacedKey(plugin, "bench"), new ItemStack(Material.NETHER_STAR));

        SlimefunItem dummy = new SlimefunItem(itemGroup,
            new SlimefunItemStack(DUMMY_ID, Material.STONE, "Bench Dummy"),
            RecipeType.NULL, new ItemStack[9]);
        dummy.register(Slimefun.instance());

        SlimefunItem ticking = new SlimefunItem(itemGroup,
            new SlimefunItemStack(TICKER_ID, Material.IRON_BLOCK, "Bench Ticker"),
            RecipeType.NULL, new ItemStack[9]);

        ticking.addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                // Intentionally trivial: this measures the ticker's dispatch
                // overhead (location info lookup, item resolution, ticker
                // resolution), not actual machine logic.
                data.getString("id");
                TICK_COUNTER.incrementAndGet();
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });

        ticking.register(Slimefun.instance());

        machine = new BenchMachine(itemGroup,
            new SlimefunItemStack(BenchMachine.ID, Material.FURNACE, "Bench Machine"));
        machine.register(Slimefun.instance());

        generator = new BenchGenerator(itemGroup,
            new SlimefunItemStack(BenchGenerator.ID, Material.BLAST_FURNACE, "Bench Generator"));
        generator.register(Slimefun.instance());

        heftyMachine = new BenchHeftyMachine(itemGroup,
            new SlimefunItemStack(BenchHeftyMachine.ID, Material.SMOKER, "Bench Hefty Machine"));
        heftyMachine.register(Slimefun.instance());

        /*
         * Cargo stand-ins under the production item ids: CargoNetworkTask#run()
         * feeds the profiler with the input-node and manager items, and the
         * profiler rejects null items. Registering plain items under those ids
         * (environment setup, not code under test) lets the cargo scenario
         * drive the task's real run() method end to end. Note the ids follow
         * the SlimefunItems constants (the input node's id is CARGO_NODE_INPUT).
         */
        SlimefunItem cargoInputNode = new SlimefunItem(itemGroup,
            new SlimefunItemStack("CARGO_NODE_INPUT", Material.PLAYER_HEAD, "Bench Cargo Input Node"),
            RecipeType.NULL, new ItemStack[9]);
        cargoInputNode.register(Slimefun.instance());

        SlimefunItem cargoManager = new SlimefunItem(itemGroup,
            new SlimefunItemStack("CARGO_MANAGER", Material.PLAYER_HEAD, "Bench Cargo Manager"),
            RecipeType.NULL, new ItemStack[9]);
        cargoManager.register(Slimefun.instance());
    }
}

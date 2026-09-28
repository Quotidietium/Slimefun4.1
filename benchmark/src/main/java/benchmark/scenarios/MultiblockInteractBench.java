package benchmark.scenarios;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import be.seeseemelk.mockbukkit.WorldMock;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.MultiBlockListener;

/**
 * Measures the multiblock interaction scan: every right-click on ANY block
 * walks the registered multiblock list and compares the surrounding
 * structure (up to 15 block reads per multiblock).
 *
 * <p>
 * {@value #MACHINE_COUNT} machines model the vanilla registry scale; the
 * three variants cover the production shapes: a click on an unrelated block
 * (the overwhelmingly common case), a click on a matching trigger material
 * with a non-matching environment, and a fully assembled structure.
 * </p>
 */
public final class MultiblockInteractBench {

    private static final int MACHINE_COUNT = 40;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int MISS_CLICKS = 200_000;
    private static final int NEAR_CLICKS = 50_000;
    private static final int MATCH_CLICKS = 10_000;

    private static final Material[] TRIGGER_MATERIALS = { Material.CAULDRON, Material.CRAFTING_TABLE, Material.FURNACE, Material.BOOKSHELF, Material.DISPENSER, Material.OAK_LOG, Material.SMITHING_TABLE, Material.BLAST_FURNACE, Material.SMOKER, Material.CAULDRON };

    public void run(BenchContext ctx, Results results) throws Exception {
        /*
         * The full-match variant calls canUse(), which consults the
         * ProtectionManager - normally created on the first server tick this
         * bench never pumps. Inject it the same way IntegrationsManager
         * would (environment setup, not code under test).
         */
        try {
            java.lang.reflect.Field field = Slimefun.getIntegrations().getClass().getDeclaredField("protectionManager");
            field.setAccessible(true);
            field.set(Slimefun.getIntegrations(), new io.github.bakedlibs.dough.protection.ProtectionManager(ctx.plugin()));
        } catch (ReflectiveOperationException x) {
            throw new IllegalStateException("Cannot inject the ProtectionManager", x);
        }

        ItemGroup group = new ItemGroup(new NamespacedKey(ctx.plugin(), "mb_bench_group"), new ItemStack(Material.CHEST));

        // Register the machine fleet (postRegister puts them in the registry)
        for (int i = 0; i < MACHINE_COUNT; i++) {
            Material trigger = TRIGGER_MATERIALS[i % TRIGGER_MATERIALS.length];

            // Structure: mostly the trigger material column + a decorated side column,
            // index layout [top,mid,bottom] x [sideA, middle, sideB]
            ItemStack[] structure = new ItemStack[9];

            for (int s = 0; s < 9; s++) {
                structure[s] = s % 3 == 1 ? new ItemStack(trigger) : null;
            }

            SlimefunItemStack stack = new SlimefunItemStack("BENCH_MBLOCK_" + i, Material.PAPER, "&bBench Machine " + i, "&7multiblock");
            new MultiBlockMachine(group, stack, structure, BlockFace.SELF) {

                @Override
                public void onInteract(org.bukkit.entity.Player p, org.bukkit.block.Block b) {
                    // Bench machines have no interaction behaviour
                }
            }.register(Slimefun.instance());
        }

        Player player = ctx.server().addPlayer("mb_bench_user");
        WorldMock world = (WorldMock) player.getWorld();

        MultiBlockListener listener = new MultiBlockListener(Slimefun.instance());

        // The common production shape: a click on a block no multiblock uses
        Block missBlock = world.getBlockAt(0, 100, 0);
        world.getBlockAt(0, 100, 0).setType(Material.STONE);

        // Near miss: the right material, the wrong environment
        Block nearBlock = world.getBlockAt(100, 100, 0);
        world.getBlockAt(100, 100, 0).setType(Material.CAULDRON);

        // Full match: an assembled machine (trigger column: mid + top + bottom)
        Block matchBlock = world.getBlockAt(200, 100, 0);
        world.getBlockAt(200, 101, 0).setType(Material.CAULDRON);
        world.getBlockAt(200, 100, 0).setType(Material.CAULDRON);
        world.getBlockAt(200, 99, 0).setType(Material.CAULDRON);

        clickVariant(results, listener, player, "click-no-match", missBlock, MISS_CLICKS);
        Bench.gcSettle();
        clickVariant(results, listener, player, "click-near-miss", nearBlock, NEAR_CLICKS);
        Bench.gcSettle();
        clickVariant(results, listener, player, "click-full-match", matchBlock, MATCH_CLICKS);
    }

    private void clickVariant(Results results, MultiBlockListener listener, Player player, String variant, Block block, int clicks) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < clicks; i++) {
                PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST, EquipmentSlot.HAND);
                listener.onRightClick(event);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < clicks; i++) {
                PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST, EquipmentSlot.HAND);
                listener.onRightClick(event);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("multiblock-interact", variant, "min_ns_per_click", "ns", Bench.min(samples) / (double) clicks);
        results.emit("multiblock-interact", variant, "median_ns_per_click", "ns", Bench.median(samples) / (double) clicks);
    }
}

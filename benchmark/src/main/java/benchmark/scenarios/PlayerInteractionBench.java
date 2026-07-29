package benchmark.scenarios;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.BenchItems;
import benchmark.Results;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * Measures the cost of player interaction with Slimefun blocks.
 *
 * <p>MockBukkit's inventory-click simulation is unreliable for Slimefun's custom menus, so
 * this scenario simulates the two highest-frequency world interactions instead: placing and
 * breaking a Slimefun block. Placing fires {@link BlockPlaceEvent} (which Slimefun turns into
 * a block-data write + ticker registration); breaking fires {@link BlockBreakEvent} (block-data
 * removal + ticker deregistration). Both are the {@link io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockPlaceEvent}
 * / break paths that many concurrent players hit at once.
 */
public final class PlayerInteractionBench {

    private static final int OPS = 200;
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;

    public void run(BenchContext ctx, Results results) {
        SlimefunItem dummy = SlimefunItem.getById(BenchItems.DUMMY_ID);

        if (dummy == null) {
            results.note("player-interaction: " + BenchItems.DUMMY_ID + " not available, skipped");
            return;
        }

        ServerMock server = ctx.server();
        Player player = server.addPlayer();
        ItemStack item = dummy.getItem();
        int batches = WARMUP + ROUNDS;

        // Variant A: place — each round places OPS fresh blocks at new locations.
        int[] placeIdx = { 0 };
        List<Location> placeLocs = ctx.grid(OPS * batches, 64);
        long[] placeSamples = Bench.timeRounds(WARMUP, ROUNDS, round -> {
            for (int i = 0; i < OPS; i++) {
                placeSlimefunBlock(server, placeLocs.get(placeIdx[0]++), item, player);
            }
        });

        results.emit("player-interaction", "place-block", "median_ns_per_op", "ns",
            (double) Bench.median(placeSamples) / OPS);
        results.emit("player-interaction", "place-block", "min_ns_per_op", "ns",
            (double) Bench.min(placeSamples) / OPS);

        // Variant B: break — pre-place all blocks, then measure the break events.
        int[] breakIdx = { 0 };
        List<Location> breakLocs = ctx.grid(OPS * batches, 66);

        for (int i = 0; i < OPS * batches; i++) {
            placeSlimefunBlock(server, breakLocs.get(i), item, player);
        }

        long[] breakSamples = Bench.timeRounds(WARMUP, ROUNDS, round -> {
            for (int i = 0; i < OPS; i++) {
                breakBlock(server, breakLocs.get(breakIdx[0]++), player);
            }
        });

        results.emit("player-interaction", "break-block", "median_ns_per_op", "ns",
            (double) Bench.median(breakSamples) / OPS);
        results.emit("player-interaction", "break-block", "min_ns_per_op", "ns",
            (double) Bench.min(breakSamples) / OPS);
    }

    private static void placeSlimefunBlock(ServerMock server, Location loc, ItemStack item, Player player) {
        Block block = new BlockMock(item.getType(), loc);
        Block against = new BlockMock(Material.GRASS_BLOCK, loc.clone().add(0, 1, 0));
        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), against, item, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
    }

    private static void breakBlock(ServerMock server, Location loc, Player player) {
        BlockBreakEvent event = new BlockBreakEvent(loc.getBlock(), player);
        server.getPluginManager().callEvent(event);
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent;
import io.github.thebusybiscuit.slimefun4.core.SlimefunRegistry;
import io.github.thebusybiscuit.slimefun4.core.handlers.MultiBlockInteractionHandler;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This {@link Listener} is responsible for listening to a {@link PlayerInteractEvent} and
 * triggering any {@link MultiBlockInteractionHandler}.
 * 
 * @author TheBusyBiscuit
 * 
 * @see MultiBlock
 * @see MultiBlockInteractionHandler
 * @see MultiBlockInteractEvent
 *
 */
public class MultiBlockListener implements Listener {

    /**
     * Pre-allocated direction arrays for {@link #compareMaterials(Block, Material[], boolean)}.
     * These are read-only constants, allocating them on every call would just create garbage
     * on each right click.
     */
    private static final BlockFace[] TWO_WAY_DIRECTIONS = { BlockFace.NORTH, BlockFace.EAST };
    private static final BlockFace[] FOUR_WAY_DIRECTIONS = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    public MultiBlockListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player p = e.getPlayer();
        Block b = e.getClickedBlock();

        SlimefunRegistry registry = Slimefun.getRegistry();

        /*
         * Direct additions to the registry list (tests, exotic addons) bypass the
         * rebuild hook in MultiBlockMachine#postRegister - detect the size drift
         * and rebuild so no multiblock can silently stop matching.
         */
        if (registry.isMultiblockBucketStale()) {
            registry.rebuildMultiblockBuckets();
        }

        /*
         * Trigger-material pre-filter: only multiblocks whose clicked structure
         * cell (see MultiBlock#getClickMaterial) is this material - directly or
         * via a supported tag - can ever match, everything else is skipped
         * without a single block read. Null-wildcard multiblocks cannot be
         * pre-filtered and are always compared.
         */
        List<MultiBlock> candidates = registry.getMultiblockCandidates(b.getType());
        List<MultiBlock> unbinned = registry.getUnbinnedMultiblocks();

        /*
         * The old implementation collected every match and acted on the last
         * one in registry order - preserve that selection exactly while
         * iterating the (order-fragmented) buckets.
         */
        MultiBlock best = null;
        int bestIndex = -1;

        for (MultiBlock mb : candidates) {
            best = findBetterMatch(b, mb, best, registry);
        }

        for (MultiBlock mb : unbinned) {
            best = findBetterMatch(b, mb, best, registry);
        }

        if (best != null) {
            MultiBlock mb = best;
            e.setCancelled(true);

            MultiBlockInteractEvent event = new MultiBlockInteractEvent(p, mb, b, e.getBlockFace());
            Bukkit.getPluginManager().callEvent(event);

            // Fixes #2809
            if (!event.isCancelled()) {
                mb.getSlimefunItem().callItemHandler(MultiBlockInteractionHandler.class, handler -> handler.onInteract(p, mb, b));
            }
        }
    }

    @ParametersAreNonnullByDefault
    private MultiBlock findBetterMatch(Block b, MultiBlock mb, MultiBlock best, SlimefunRegistry registry) {
        Block center = b.getRelative(mb.getTriggerBlock());

        if (compareMaterials(center, mb.getStructure(), mb.isSymmetric())) {
            int index = registry.getMultiblockOrder(mb);

            if (best == null || index > registry.getMultiblockOrder(best)) {
                return mb;
            }
        }

        return best;
    }

    @ParametersAreNonnullByDefault
    private boolean compareMaterials(Block b, Material[] blocks, boolean onlyTwoWay) {
        if (!compareMaterialsVertical(b, blocks[1], blocks[4], blocks[7])) {
            return false;
        }

        BlockFace[] directions = onlyTwoWay ? TWO_WAY_DIRECTIONS : FOUR_WAY_DIRECTIONS;

        for (BlockFace direction : directions) {
            if (compareMaterialsVertical(b.getRelative(direction), blocks[0], blocks[3], blocks[6]) && compareMaterialsVertical(b.getRelative(direction.getOppositeFace()), blocks[2], blocks[5], blocks[8])) {
                return true;
            }
        }

        return false;
    }

    private boolean compareMaterialsVertical(@Nonnull Block b, @Nullable Material top, @Nullable Material center, @Nullable Material bottom) {
        return (center == null || equals(b.getType(), center)) && (top == null || equals(b.getRelative(BlockFace.UP).getType(), top)) && (bottom == null || equals(b.getRelative(BlockFace.DOWN).getType(), bottom));
    }

    @ParametersAreNonnullByDefault
    private boolean equals(Material a, Material b) {
        if (a == b) {
            return true;
        }

        for (Tag<Material> tag : MultiBlock.getSupportedTags()) {
            if (tag.isTagged(a) && tag.isTagged(b)) {
                return true;
            }
        }

        return false;
    }
}

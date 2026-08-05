package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Piston;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockBurnEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockPistonEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * This {@link Listener} is responsible for listening to any physics-based events, such
 * as {@link EntityChangeBlockEvent} or a {@link BlockPistonEvent}.
 * 
 * This ensures that a {@link Piston} cannot be abused to break Slimefun blocks.
 * 
 * @author VoidAngel
 * @author Poslovitch
 * @author TheBusyBiscuit
 * @author AccelShark
 *
 */
public class BlockPhysicsListener implements Listener {

    public BlockPhysicsListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFall(EntityChangeBlockEvent e) {
        if (e.getEntity().getType() == EntityType.FALLING_BLOCK && (BlockStorage.hasBlockInfo(e.getBlock()) || Slimefun.getTickerTask().isOccupiedSoon(e.getBlock().getLocation()))) {
            e.setCancelled(true);
            FallingBlock block = (FallingBlock) e.getEntity();

            if (block.getDropItem()) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getBlockData().getMaterial(), 1));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (BlockStorage.hasBlockInfo(e.getBlock())) {
            if (!isPistonProtectionVetoed(e, e.getBlock(), false)) {
                e.setCancelled(true);
            }
        } else {
            for (Block b : e.getBlocks()) {
                if (isProtected(b) || isProtected(b.getRelative(e.getDirection()))) {
                    if (!isPistonProtectionVetoed(e, b, false)) {
                        e.setCancelled(true);
                    }
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (BlockStorage.hasBlockInfo(e.getBlock())) {
            if (!isPistonProtectionVetoed(e, e.getBlock(), true)) {
                e.setCancelled(true);
            }
        } else if (e.isSticky()) {
            for (Block b : e.getBlocks()) {
                if (isProtected(b) || isProtected(b.getRelative(e.getDirection()))) {
                    if (!isPistonProtectionVetoed(e, b, true)) {
                        e.setCancelled(true);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Fires a {@link SlimefunBlockPistonEvent} if any listener is registered and returns
     * whether the piston protection was vetoed. Without listeners this costs nothing and
     * the old behavior is preserved.
     */
    private boolean isPistonProtectionVetoed(@Nonnull org.bukkit.event.block.BlockPistonEvent e, @Nonnull Block protectedBlock, boolean retract) {
        if (SlimefunBlockPistonEvent.getHandlerList().getRegisteredListeners().length > 0) {
            SlimefunBlockPistonEvent event = new SlimefunBlockPistonEvent(e.getBlock(), e.getDirection(), protectedBlock, retract);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }

    /**
     * A {@link Block} is protected from physics if it holds Slimefun data, or if it
     * is an air block with leftover data, or if its position has been reserved by a
     * pending block-data move (e.g. a moving android) - in all three cases, letting
     * a vanilla block occupy the position would attach that data to the wrong block.
     */
    private boolean isProtected(@Nonnull Block b) {
        if (BlockStorage.hasBlockInfo(b)) {
            return true;
        }

        Location loc = b.getLocation();
        return Slimefun.getTickerTask().isOccupiedSoon(loc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        /*
         * Trees and huge mushrooms can grow into positions that hold (leftover or
         * reserved) block data. Remove only the conflicting blocks so the rest of
         * the structure can still grow.
         */
        e.getBlocks().removeIf(state -> BlockStorage.hasBlockInfo(state.getLocation()) || Slimefun.getTickerTask().isOccupiedSoon(state.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        // Do not let fire destroy a block that still holds Slimefun data
        if (BlockStorage.hasBlockInfo(e.getBlock())) {
            SlimefunItem item = BlockStorage.check(e.getBlock());

            // A leftover-data block (item == null) always stays protected. For a real
            // Slimefun block, a registered listener may veto the protection to let it burn.
            if (item == null || !isBurnProtectionVetoed(item, e.getBlock())) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Fires a {@link SlimefunBlockBurnEvent} if any listener is registered and returns
     * whether the burn protection was vetoed. Without listeners this costs nothing and
     * the old behavior is preserved.
     */
    private boolean isBurnProtectionVetoed(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block) {
        if (SlimefunBlockBurnEvent.getHandlerList().getRegisteredListeners().length > 0) {
            SlimefunBlockBurnEvent event = new SlimefunBlockBurnEvent(slimefunItem, block);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent e) {
        Block block = e.getToBlock();
        Material type = block.getType();

        // Check if this Material can be destroyed by fluids
        if (SlimefunTag.FLUID_SENSITIVE_MATERIALS.isTagged(type)) {
            // Check if this Block holds any data
            if (BlockStorage.hasBlockInfo(block)) {
                e.setCancelled(true);
            } else {
                Location loc = block.getLocation();

                // Fixes #2496 - Make sure it is not a moving block
                if (Slimefun.getTickerTask().isOccupiedSoon(loc)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBucketUse(PlayerBucketEmptyEvent e) {
        // Fix for placing water on player heads
        Location l = e.getBlockClicked().getRelative(e.getBlockFace()).getLocation();

        if (BlockStorage.hasBlockInfo(l) || Slimefun.getTickerTask().isOccupiedSoon(l)) {
            e.setCancelled(true);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockExplosionEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockExplosionEvent.Cause;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.WitherProof;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * The {@link ExplosionsListener} is a {@link Listener} which listens to any explosion events.
 * Any {@link WitherProof} block is excluded from these explosions and this {@link Listener} also
 * calls the explosive part of the {@link BlockBreakHandler}.
 *
 * @author TheBusyBiscuit
 *
 * @see BlockBreakHandler
 * @see WitherProof
 * @see SlimefunBlockExplosionEvent
 *
 */
public class ExplosionsListener implements Listener {

    public ExplosionsListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        removeResistantBlocks(e.blockList().iterator(), Cause.ENTITY_EXPLOSION);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        removeResistantBlocks(e.blockList().iterator(), Cause.BLOCK_EXPLOSION);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBreak(EntityChangeBlockEvent e) {
        if (e.getEntity().getType() == EntityType.WITHER || e.getEntity().getType() == EntityType.WITHER_SKULL) {
            removeResistantBlock(e.getBlock());
        }
    }

    private void removeResistantBlocks(@Nonnull Iterator<Block> blocks, @Nonnull Cause cause) {
        while (blocks.hasNext()) {
            Block block = blocks.next();
            SlimefunItem item = BlockStorage.check(block);

            if (item != null) {
                blocks.remove();
                removeResistantBlockOnExplosion(block, item, cause);
            }
        }
    }

    private void removeResistantBlock(@Nonnull Block block) {
        SlimefunItem slimefunItem = BlockStorage.check(block);

        if (slimefunItem != null) {
            removeResistantBlock(block, slimefunItem);
        }
    }

    private void removeResistantBlock(@Nonnull Block block, @Nonnull SlimefunItem slimefunItem) {
        // Fixes #3414 - This check removes the ghost block created by withers.
        if (!(slimefunItem instanceof WitherProof)
            && !slimefunItem.callItemHandler(BlockBreakHandler.class, handler -> handleExplosion(handler, block))
        ) {
            destroy(block);
        }
    }

    /**
     * Explosion-only variant of {@link #removeResistantBlock(Block, SlimefunItem)} that
     * additionally fires a {@link SlimefunBlockExplosionEvent}, giving addons a chance to
     * protect the block. Without listeners the behavior is identical to the original.
     */
    private void removeResistantBlockOnExplosion(@Nonnull Block block, @Nonnull SlimefunItem slimefunItem, @Nonnull Cause cause) {
        // WitherProof blocks survive explosions on their own and fire no event.
        if (slimefunItem instanceof WitherProof) {
            return;
        }

        if (slimefunItem.callItemHandler(BlockBreakHandler.class, handler -> handleExplosion(handler, block, slimefunItem, cause))) {
            // The handler decided (and possibly destroyed the block with its drops) already.
            return;
        }

        if (!isExplosionProtectionVetoed(slimefunItem, block, cause)) {
            destroy(block);
        }
    }

    @ParametersAreNonnullByDefault
    private void handleExplosion(BlockBreakHandler handler, Block block) {
        if (handler.isExplosionAllowed(block)) {
            // Collect drops BEFORE destroying the block: SimpleBlockBreakHandler.onExplode delegates
            // to onBlockBreak which reads BlockStorage.getInventory(block) to drop the machine's
            // contents. If destroy() (which calls clearBlockInfo) runs first, the inventory is gone
            // and the contents are silently voided.
            List<ItemStack> drops = new ArrayList<>();
            handler.onExplode(block, drops);

            destroy(block);

            for (ItemStack drop : drops) {
                if (drop != null && !drop.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }
    }

    /**
     * Explosion-only variant of {@link #handleExplosion(BlockBreakHandler, Block)} that
     * additionally fires a {@link SlimefunBlockExplosionEvent} before dropping items.
     */
    @ParametersAreNonnullByDefault
    private void handleExplosion(BlockBreakHandler handler, Block block, SlimefunItem slimefunItem, Cause cause) {
        if (!handler.isExplosionAllowed(block)) {
            return;
        }

        if (isExplosionProtectionVetoed(slimefunItem, block, cause)) {
            return;
        }

        // Collect drops BEFORE destroying the block: SimpleBlockBreakHandler.onExplode delegates
        // to onBlockBreak which reads BlockStorage.getInventory(block) to drop the machine's
        // contents. If destroy() (which calls clearBlockInfo) runs first, the inventory is gone
        // and the contents are silently voided.
        List<ItemStack> drops = new ArrayList<>();
        handler.onExplode(block, drops);

        destroy(block);

        for (ItemStack drop : drops) {
            if (drop != null && !drop.getType().isAir()) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }

    /**
     * Clears the {@link BlockStorage} data of the given block and turns it into air.
     * Any Networks are notified before the data is cleared, otherwise a destroyed
     * connector/regulator would never trigger a re-classification and the Network would
     * silently keep working across the gap (or a dead Network would linger forever,
     * blocking new Networks at the same spot).
     */
    private void destroy(@Nonnull Block block) {
        Slimefun.getNetworkManager().updateAllNetworks(block.getLocation());
        BlockStorage.clearBlockInfo(block);
        block.setType(Material.AIR);
    }

    /**
     * Fires a {@link SlimefunBlockExplosionEvent} if any listener is registered and
     * returns whether the explosion protection was vetoed. Without listeners this costs
     * nothing and the old behavior is preserved.
     */
    @ParametersAreNonnullByDefault
    private boolean isExplosionProtectionVetoed(SlimefunItem slimefunItem, Block block, Cause cause) {
        if (SlimefunBlockExplosionEvent.getHandlerList().getRegisteredListeners().length > 0) {
            SlimefunBlockExplosionEvent event = new SlimefunBlockExplosionEvent(slimefunItem, block, cause);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }
}

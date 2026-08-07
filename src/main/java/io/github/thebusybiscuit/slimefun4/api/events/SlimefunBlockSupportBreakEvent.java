package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;

/**
 * This {@link PlayerEvent} is fired whenever a sensitive {@link SlimefunItem} block -
 * one whose {@link org.bukkit.Material} is listed in {@link SlimefunTag#SENSITIVE_MATERIALS},
 * such as an elevator plate - is about to be destroyed because the {@link Block}
 * supporting it was broken.
 * <p>
 * Cancelling this event protects the sensitive block entirely: it stays in place
 * (now floating), keeps its {@code BlockStorage} data and its
 * {@code BlockBreakHandler} is not called. Without listeners the block is destroyed
 * with its drops, exactly like before.
 *
 * @author Zurker
 *
 * @see SlimefunBlockBreakEvent
 * @see SlimefunBlockExplosionEvent
 */
public class SlimefunBlockSupportBreakEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack item;
    private final Block block;
    private final Block supportingBlock;
    private final SlimefunItem slimefunItem;

    private boolean cancelled;

    public SlimefunBlockSupportBreakEvent(@Nonnull Player player, @Nonnull ItemStack item, @Nonnull Block block, @Nonnull Block supportingBlock, @Nonnull SlimefunItem slimefunItem) {
        super(player);
        Validate.notNull(item, "The ItemStack must not be null");
        Validate.notNull(block, "The sensitive Block must not be null");
        Validate.notNull(supportingBlock, "The supporting Block must not be null");
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");

        this.item = item;
        this.block = block;
        this.supportingBlock = supportingBlock;
        this.slimefunItem = slimefunItem;
    }

    /**
     * This returns the {@link ItemStack} the {@link Player} held while breaking the
     * supporting block. This may be air.
     *
     * @return The held {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the sensitive {@link Block} that is about to be destroyed.
     *
     * @return The sensitive {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link Block} whose destruction causes the sensitive block
     * above it to break.
     *
     * @return The supporting {@link Block}
     */
    @Nonnull
    public Block getSupportingBlock() {
        return supportingBlock;
    }

    /**
     * This returns the {@link SlimefunItem} of the sensitive block.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}

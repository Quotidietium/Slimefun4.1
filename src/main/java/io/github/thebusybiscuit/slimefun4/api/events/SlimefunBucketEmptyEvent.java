package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} empties a bucket (water or lava)
 * at a position that holds a {@link SlimefunItem} block and Slimefun is about to cancel the
 * placement: the liquid must not overwrite Slimefun block data (e.g. a player-head Slimefun block).
 * <p>
 * Cancelling this event vetoes the protection: the liquid is placed and overwrites the block.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.listeners.BlockPhysicsListener
 */
public class SlimefunBucketEmptyEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;

    private boolean cancelled;

    public SlimefunBucketEmptyEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull Block block) {
        super(player);
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
    }

    /**
     * This returns the {@link SlimefunItem} whose block the liquid would overwrite.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link Block} the liquid is about to be placed on.
     *
     * @return The {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
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

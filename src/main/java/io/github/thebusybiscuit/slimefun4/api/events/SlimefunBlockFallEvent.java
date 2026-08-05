package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link Event} is fired whenever a placed {@link SlimefunItem} block that is subject to
 * gravity (e.g. a sand-like block) is about to turn into a falling block and Slimefun is about
 * to cancel the fall: Slimefun blocks are protected from gravity so they do not vanish or
 * attach their data to the wrong block.
 * <p>
 * Cancelling this event vetoes the protection: the block falls like any vanilla gravity block.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.listeners.BlockPhysicsListener
 * @see SlimefunBlockBurnEvent
 */
public class SlimefunBlockFallEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;

    private boolean cancelled;

    public SlimefunBlockFallEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
    }

    /**
     * This returns the {@link SlimefunItem} of the block that is about to fall.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link Block} that is about to fall.
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

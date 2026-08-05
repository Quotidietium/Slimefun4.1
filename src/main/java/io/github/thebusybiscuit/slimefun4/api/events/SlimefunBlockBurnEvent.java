package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link Event} is fired whenever fire is about to destroy a placed {@link SlimefunItem}
 * block and Slimefun is about to cancel the burn: Slimefun blocks are fireproof by default.
 * <p>
 * Cancelling this event vetoes the protection: the block burns away like any vanilla
 * flammable block.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.listeners.BlockPhysicsListener
 * @see SlimefunBlockExplosionEvent
 */
public class SlimefunBlockBurnEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;

    private boolean cancelled;

    public SlimefunBlockBurnEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
    }

    /**
     * This returns the {@link SlimefunItem} of the block that is about to burn.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link Block} that is about to burn.
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

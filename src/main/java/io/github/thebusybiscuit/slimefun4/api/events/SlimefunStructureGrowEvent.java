package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link Event} is fired for each block a growing structure (a tree or a huge mushroom)
 * is about to occupy that already holds a {@link SlimefunItem} block, and Slimefun is about to
 * skip that block of the structure so the Slimefun block is preserved.
 * <p>
 * By default the Slimefun block is kept and the structure grows around it. Cancelling this
 * event lets the structure overwrite the Slimefun block instead (the Slimefun data is lost).
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.listeners.BlockPhysicsListener
 */
public class SlimefunStructureGrowEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;

    private boolean cancelled;

    public SlimefunStructureGrowEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
    }

    /**
     * This returns the {@link SlimefunItem} whose block the structure is about to grow into.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link Block} the structure is about to grow into.
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

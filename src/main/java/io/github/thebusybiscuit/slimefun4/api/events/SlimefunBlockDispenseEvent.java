package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockDispenseEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockDispenseHandler;

/**
 * This {@link Event} is fired whenever a {@link SlimefunItem} placed as a dispenser
 * {@link Block} is triggered, before its {@link BlockDispenseHandler} is called.
 * <p>
 * Cancelling this event skips the {@link BlockDispenseHandler}. Note that the vanilla
 * dispense still happens unless the underlying {@link BlockDispenseEvent} is cancelled
 * as well.
 *
 * @author Zurker
 *
 * @see BlockDispenseHandler
 */
public class SlimefunBlockDispenseEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;
    private final BlockDispenseEvent dispenseEvent;

    private boolean cancelled;

    public SlimefunBlockDispenseEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block, @Nonnull BlockDispenseEvent dispenseEvent) {
        super();

        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The block must not be null");
        Validate.notNull(dispenseEvent, "The dispense event must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
        this.dispenseEvent = dispenseEvent;
    }

    /**
     * This returns the {@link SlimefunItem} the dispenser {@link Block} belongs to.
     *
     * @return The {@link SlimefunItem} of the dispenser
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the dispenser {@link Block} that was triggered.
     *
     * @return The dispenser {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the underlying {@link BlockDispenseEvent}, giving access to the
     * dispensed item and allowing listeners to cancel the vanilla dispense too.
     *
     * @return The underlying {@link BlockDispenseEvent}
     */
    @Nonnull
    public BlockDispenseEvent getDispenseEvent() {
        return dispenseEvent;
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

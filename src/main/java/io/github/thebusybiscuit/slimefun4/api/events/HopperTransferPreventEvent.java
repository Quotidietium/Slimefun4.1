package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotHopperable;

/**
 * This {@link Event} is fired whenever a hopper is about to transfer an {@link ItemStack}
 * into a {@link SlimefunItem} block marked as {@link NotHopperable} and Slimefun is about
 * to cancel the transfer: {@link NotHopperable} machines are not meant to be fed (or
 * drained) by hoppers.
 * <p>
 * Cancelling this event vetoes the prevention: the hopper transfer is allowed to proceed.
 *
 * @author Zurker
 *
 * @see NotHopperable
 */
public class HopperTransferPreventEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Inventory source;
    private final Inventory destination;
    private final ItemStack item;
    private final InventoryMoveItemEvent moveEvent;

    private boolean cancelled;

    public HopperTransferPreventEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Inventory source, @Nonnull Inventory destination, @Nonnull ItemStack item, @Nonnull InventoryMoveItemEvent moveEvent) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(source, "The source inventory must not be null");
        Validate.notNull(destination, "The destination inventory must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(moveEvent, "The move event must not be null");

        this.slimefunItem = slimefunItem;
        this.source = source;
        this.destination = destination;
        this.item = item;
        this.moveEvent = moveEvent;
    }

    /**
     * This returns the {@link NotHopperable} {@link SlimefunItem} the hopper is transferring into.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the hopper {@link Inventory} the item is moving from.
     *
     * @return The source {@link Inventory}
     */
    @Nonnull
    public Inventory getSource() {
        return source;
    }

    /**
     * This returns the {@link NotHopperable} machine {@link Inventory} the item is moving into.
     *
     * @return The destination {@link Inventory}
     */
    @Nonnull
    public Inventory getDestination() {
        return destination;
    }

    /**
     * This returns the {@link ItemStack} being transferred.
     *
     * @return The {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the original {@link InventoryMoveItemEvent} for this transfer.
     *
     * @return The {@link InventoryMoveItemEvent}
     */
    @Nonnull
    public InventoryMoveItemEvent getMoveEvent() {
        return moveEvent;
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

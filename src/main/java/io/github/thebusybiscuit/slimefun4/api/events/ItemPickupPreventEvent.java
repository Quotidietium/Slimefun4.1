package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * This {@link Event} is fired whenever Slimefun is about to prevent a dropped
 * {@link Item} from being picked up, either by an entity or by a hopper. The
 * {@link PreventReason} tells the two cases apart:
 * <ul>
 * <li>{@link PreventReason#NO_PICKUP_FLAG}: the item was flagged through
 * {@link SlimefunUtils#markAsNoPickup(Item, String)}, only the pickup is prevented.</li>
 * <li>{@link PreventReason#ALTAR_PROBE}: the item is an {@link AncientPedestal} probe
 * or display item, the pickup is prevented and the item is about to be removed.</li>
 * </ul>
 * Cancelling this event vetoes the prevention: the item is neither kept from being
 * picked up nor removed and the vanilla pickup behavior applies.
 *
 * @author Zurker
 *
 * @see SlimefunUtils#markAsNoPickup(Item, String)
 * @see AncientPedestal
 */
public class ItemPickupPreventEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The reason why the pickup of the {@link Item} is about to be prevented.
     */
    public enum PreventReason {

        /**
         * The item was flagged as not pickupable via
         * {@link SlimefunUtils#markAsNoPickup(Item, String)}.
         */
        NO_PICKUP_FLAG,

        /**
         * The item is an {@link AncientPedestal} probe or display item and is about to
         * be removed alongside the prevented pickup.
         */
        ALTAR_PROBE
    }

    private final Item item;
    private final PreventReason reason;

    private boolean cancelled;

    public ItemPickupPreventEvent(@Nonnull Item item, @Nonnull PreventReason reason) {
        Validate.notNull(item, "The Item must not be null");
        Validate.notNull(reason, "The PreventReason must not be null");

        this.item = item;
        this.reason = reason;
    }

    /**
     * This returns the dropped {@link Item} whose pickup is about to be prevented.
     *
     * @return The {@link Item}
     */
    @Nonnull
    public Item getItem() {
        return item;
    }

    /**
     * This returns the {@link PreventReason} for this prevention.
     *
     * @return The {@link PreventReason}
     */
    @Nonnull
    public PreventReason getReason() {
        return reason;
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

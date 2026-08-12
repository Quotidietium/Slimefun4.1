package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.LimitedUseItem;

/**
 * This {@link PlayerEvent} is fired whenever a {@link LimitedUseItem} is about to
 * consume one of its limited charges after being used.
 * <p>
 * Cancelling this event vetoes the consumption: the charge count stays as it is
 * and the item does not break. Note that the item's effect has already been applied
 * at this point, cancelling only makes the use free of charge. When the used stack
 * held more than one item, the stack was already split and the separated item is
 * handed to the {@link Player} with its charges untouched.
 * <p>
 * When {@link #willBreak()} is true, this is the last charge: without a veto the
 * item breaks right after this event. The event is fired synchronously, since item
 * uses happen on the main thread.
 * <p>
 * Addons may also adjust how expensive this use is via {@link #setUsesLeftAfter(int)},
 * e.g. to make a heavy use consume several charges or to break the item early. A use
 * must always cost at least one charge: cancel the event for a free use.
 *
 * @author Zurker
 *
 * @see LimitedUseItem
 * @see SlimefunItemUseEvent
 */
public class LimitedUseItemConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final LimitedUseItem item;
    private final ItemStack itemStack;
    private final int usesLeftBefore;

    private int usesLeftAfter;
    private boolean cancelled;

    public LimitedUseItemConsumeEvent(@Nonnull Player player, @Nonnull LimitedUseItem item, @Nonnull ItemStack itemStack, int usesLeftBefore) {
        super(player);
        Validate.notNull(item, "The LimitedUseItem must not be null");
        Validate.notNull(itemStack, "The ItemStack must not be null");
        Validate.isTrue(usesLeftBefore >= 1, "The uses left before this use must be at least 1");

        this.item = item;
        this.itemStack = itemStack;
        this.usesLeftBefore = usesLeftBefore;
        this.usesLeftAfter = usesLeftBefore - 1;
    }

    /**
     * This returns the {@link LimitedUseItem} definition being consumed.
     *
     * @return The {@link LimitedUseItem}
     */
    @Nonnull
    public LimitedUseItem getItem() {
        return item;
    }

    /**
     * This returns the live {@link ItemStack} whose charge is being consumed -
     * treat it as read-only context.
     *
     * @return The {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * This returns the number of charges left before this use.
     *
     * @return The charges left before this use
     */
    public int getUsesLeftBefore() {
        return usesLeftBefore;
    }

    /**
     * This returns whether this use breaks the item: without a veto the item breaks
     * right after this event. This reflects {@link #setUsesLeftAfter(int)}: setting the
     * remaining charges to zero breaks the item.
     *
     * @return Whether the item is about to break
     */
    public boolean willBreak() {
        return usesLeftAfter == 0;
    }

    /**
     * This returns the number of charges left after this use. It defaults to
     * {@link #getUsesLeftBefore()} minus one; zero means the item breaks.
     *
     * @return The charges left after this use
     */
    public int getUsesLeftAfter() {
        return usesLeftAfter;
    }

    /**
     * This sets the number of charges left after this use, overriding how expensive
     * this use is. Setting it to zero breaks the item. A use must always cost at least
     * one charge, so the value must stay below {@link #getUsesLeftBefore()}: cancel the
     * event for a free use.
     *
     * @param usesLeftAfter
     *            The charges left after this use, between 0 (inclusive) and
     *            {@link #getUsesLeftBefore()} (exclusive)
     */
    public void setUsesLeftAfter(int usesLeftAfter) {
        Validate.isTrue(usesLeftAfter >= 0 && usesLeftAfter < usesLeftBefore, "The uses left after must be at least 0 and below the uses left before");

        this.usesLeftAfter = usesLeftAfter;
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

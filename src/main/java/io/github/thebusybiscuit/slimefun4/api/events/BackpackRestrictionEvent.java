package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;

/**
 * This {@link PlayerEvent} is fired whenever Slimefun is about to restrict an
 * interaction involving a {@link SlimefunBackpack} that the {@link Player} is
 * currently viewing: moving a disallowed item into it, dropping a backpack while
 * viewing one, or swapping the viewed backpack itself to the off hand.
 * <p>
 * Cancelling this event vetoes the restriction: the interaction is allowed to
 * proceed. Which restriction was hit is told by {@link #getReason()}.
 * <p>
 * The event is fired synchronously, since inventory interactions happen on the main
 * thread.
 *
 * @author Zurker
 *
 * @see PlayerBackpackOpenEvent
 * @see PlayerBackpackCloseEvent
 * @see SlimefunBackpack
 */
public class BackpackRestrictionEvent extends PlayerEvent implements Cancellable {

    /**
     * This enum describes which backpack restriction was hit.
     */
    public enum Reason {

        /**
         * An item that the backpack does not allow (per
         * {@link SlimefunBackpack#isItemAllowed}) was about to be moved into it,
         * be it by clicking, shift-clicking, number keys, off-hand swapping or
         * dragging.
         */
        DISALLOWED_ITEM,

        /**
         * A {@link SlimefunBackpack} was about to be dropped while the
         * {@link Player} is viewing a backpack.
         */
        BACKPACK_DROP,

        /**
         * The viewed backpack itself was about to be swapped to the off hand.
         */
        BACKPACK_OFFHAND
    }

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunBackpack backpack;
    private final ItemStack backpackItem;
    private final ItemStack item;
    private final Reason reason;

    private boolean cancelled;

    public BackpackRestrictionEvent(@Nonnull Player player, @Nonnull SlimefunBackpack backpack, @Nonnull ItemStack backpackItem, @Nonnull ItemStack item, @Nonnull Reason reason) {
        super(player);
        Validate.notNull(backpack, "The SlimefunBackpack must not be null");
        Validate.notNull(backpackItem, "The viewed backpack item must not be null");
        Validate.notNull(item, "The restricted item must not be null");
        Validate.notNull(reason, "The reason must not be null");

        this.backpack = backpack;
        this.backpackItem = backpackItem;
        this.item = item;
        this.reason = reason;
    }

    /**
     * This returns the {@link SlimefunBackpack} definition whose restriction was hit.
     *
     * @return The {@link SlimefunBackpack}
     */
    @Nonnull
    public SlimefunBackpack getBackpack() {
        return backpack;
    }

    /**
     * This returns the live {@link ItemStack} of the backpack the {@link Player} is
     * viewing - treat it as read-only context.
     *
     * @return The viewed backpack {@link ItemStack}
     */
    @Nonnull
    public ItemStack getBackpackItem() {
        return backpackItem;
    }

    /**
     * This returns the {@link ItemStack} the restriction applies to: the disallowed
     * item being moved into the backpack, or the backpack being dropped or swapped
     * to the off hand.
     *
     * @return The restricted {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns which backpack restriction was hit.
     *
     * @return The {@link Reason}
     */
    @Nonnull
    public Reason getReason() {
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

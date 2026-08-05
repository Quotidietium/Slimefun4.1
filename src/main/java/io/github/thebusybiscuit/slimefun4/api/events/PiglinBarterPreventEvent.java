package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link Event} is fired whenever Slimefun is about to prevent a {@link Piglin} from
 * obtaining a {@link SlimefunItem} that merely looks like a gold ingot. The {@link Reason}
 * tells the two cases apart:
 * <ul>
 * <li>{@link Reason#PICKUP}: a {@link Piglin} tried to pick up the Slimefun gold on its own.
 * No {@link Player} is involved.</li>
 * <li>{@link Reason#BARTER}: a {@link Player} tried to barter with the {@link Piglin} using
 * the Slimefun gold. The {@link Player} is the one who right-clicked.</li>
 * </ul>
 * Cancelling this event vetoes the prevention: the {@link Piglin} can obtain the gold and
 * (for a barter) produce a vanilla barter result.
 *
 * @author Zurker
 *
 * @see SlimefunItem
 */
public class PiglinBarterPreventEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The way the {@link Piglin} was about to obtain the Slimefun gold.
     */
    public enum Reason {

        /**
         * The {@link Piglin} tried to pick up the Slimefun gold on its own.
         */
        PICKUP,

        /**
         * A {@link Player} tried to barter with the {@link Piglin} using the Slimefun gold.
         */
        BARTER
    }

    private final SlimefunItem slimefunItem;
    private final Piglin piglin;
    private final ItemStack item;
    private final Player player;
    private final Reason reason;

    private boolean cancelled;

    public PiglinBarterPreventEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Piglin piglin, @Nonnull ItemStack item, @Nullable Player player, @Nonnull Reason reason) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(piglin, "The Piglin must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(reason, "The Reason must not be null");

        this.slimefunItem = slimefunItem;
        this.piglin = piglin;
        this.item = item;
        this.player = player;
        this.reason = reason;
    }

    /**
     * This returns the {@link SlimefunItem} (the gold look-alike) involved.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link Piglin} involved.
     *
     * @return The {@link Piglin}
     */
    @Nonnull
    public Piglin getPiglin() {
        return piglin;
    }

    /**
     * This returns the gold {@link ItemStack}.
     *
     * @return The {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the {@link Player} who tried to barter, or {@code null} for a
     * {@link Reason#PICKUP}.
     *
     * @return The {@link Player}, or {@code null}
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * This returns the {@link Reason} the {@link Piglin} was about to obtain the gold.
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

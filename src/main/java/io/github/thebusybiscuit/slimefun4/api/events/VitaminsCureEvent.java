package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.medical.Vitamins;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks {@link Vitamins}:
 * the vitamins are about to be consumed, any fire extinguished, negative potion effects and
 * radiation exposure cleared and the player healed.
 * <p>
 * Cancelling this event skips the use entirely: no vitamins are consumed and no curing happens.
 * <p>
 * The heal amount (how many half-hearts the player is healed) can be modified via
 * {@link #setHealAmount(int)} before the cure is applied, allowing addons to boost
 * or reduce the healing without cancelling the entire cure.
 *
 * @author Zurker
 *
 * @see Vitamins
 */
public class VitaminsCureEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Vitamins vitamins;
    private int healAmount;

    private boolean cancelled;

    public VitaminsCureEvent(@Nonnull Player player, @Nonnull Vitamins vitamins, int healAmount) {
        super(player);
        Validate.notNull(vitamins, "The Vitamins must not be null");
        Validate.isTrue(healAmount >= 0, "The heal amount must not be negative");

        this.vitamins = vitamins;
        this.healAmount = healAmount;
    }

    /**
     * This returns the {@link Vitamins} that are being consumed.
     *
     * @return The {@link Vitamins}
     */
    @Nonnull
    public Vitamins getVitamins() {
        return vitamins;
    }

    /**
     * This returns the heal amount that will be applied (in half-hearts).
     *
     * @return The heal amount
     */
    public int getHealAmount() {
        return healAmount;
    }

    /**
     * This sets the heal amount that will be applied.
     *
     * @param healAmount
     *            The new heal amount, must be at least 0
     */
    public void setHealAmount(int healAmount) {
        Validate.isTrue(healAmount >= 0, "The heal amount must not be negative");
        this.healAmount = healAmount;
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

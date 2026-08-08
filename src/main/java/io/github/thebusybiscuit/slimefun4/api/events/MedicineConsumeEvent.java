package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.medical.Medicine;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} consumes {@link Medicine}:
 * the medicine is about to extinguish the player, clear negative potion effects and
 * radiation exposure, and heal them.
 * <p>
 * Cancelling this event skips the curing entirely: no fire is extinguished, no effects or
 * radiation are cleared and no healing happens.
 * <p>
 * The heal amount (how many half-hearts the player is healed) can be modified via
 * {@link #setHealAmount(int)} before the cure is applied.
 *
 * @author Zurker
 *
 * @see Medicine
 * @see VitaminsCureEvent
 * @see BandageHealEvent
 */
public class MedicineConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Medicine medicine;
    private int healAmount;
    private boolean cancelled;

    public MedicineConsumeEvent(@Nonnull Player player, @Nonnull Medicine medicine, int healAmount) {
        super(player);
        Validate.notNull(medicine, "The Medicine must not be null");
        Validate.isTrue(healAmount >= 0, "The heal amount must not be negative");

        this.medicine = medicine;
        this.healAmount = healAmount;
    }

    /**
     * This returns the {@link Medicine} that is being consumed.
     *
     * @return The {@link Medicine}
     */
    @Nonnull
    public Medicine getMedicine() {
        return medicine;
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

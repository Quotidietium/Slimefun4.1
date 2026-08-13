package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.VampireBlade;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} holding a {@link VampireBlade}
 * hits an entity and the blade's lifesteal chance succeeds: the {@link Player} is about to be
 * healed by the blade's healing amount.
 * <p>
 * The healing amount is modifiable via {@link #setHealAmount(double)} - an addon may increase
 * or decrease the lifesteal. Cancelling this event prevents the heal entirely (no health is
 * restored and no sound plays).
 *
 * @author Zurker
 *
 * @see VampireBlade
 */
public class VampireBladeHealEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final VampireBlade vampireBlade;
    private double healAmount;

    private boolean cancelled;

    public VampireBladeHealEvent(@Nonnull Player player, @Nonnull VampireBlade vampireBlade, double healAmount) {
        super(player);
        Validate.notNull(vampireBlade, "The VampireBlade must not be null");

        this.vampireBlade = vampireBlade;
        this.healAmount = healAmount;
    }

    /**
     * This returns the {@link VampireBlade} that triggered the heal.
     *
     * @return The {@link VampireBlade}
     */
    @Nonnull
    public VampireBlade getVampireBlade() {
        return vampireBlade;
    }

    /**
     * This returns the amount of health that is about to be restored. Use
     * {@link #setHealAmount(double)} to change it.
     *
     * @return The healing amount
     */
    public double getHealAmount() {
        return healAmount;
    }

    /**
     * This overrides the amount of health that will be restored.
     *
     * @param healAmount
     *            The new healing amount, must be a finite number and not negative
     */
    public void setHealAmount(double healAmount) {
        Validate.isTrue(Double.isFinite(healAmount), "The healing amount must be a finite number, received: " + healAmount);
        Validate.isTrue(healAmount >= 0, "The healing amount must not be negative, received: " + healAmount);

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

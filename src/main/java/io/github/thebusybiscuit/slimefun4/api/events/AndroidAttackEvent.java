package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ButcherAndroid;

/**
 * This {@link Event} is fired before a {@link ButcherAndroid} damages a
 * {@link LivingEntity}. If this {@link Event} is cancelled, the entity will not
 * be damaged and the android continues with the next entity in its facing direction.
 * <p>
 * The damage can be adjusted via {@link #setDamage(double)}.
 *
 * @author Zurker
 *
 * @see AndroidMineEvent
 * @see AndroidFarmEvent
 */
public class AndroidAttackEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AndroidInstance android;
    private final LivingEntity target;
    private double damage;
    private boolean cancelled;

    /**
     * @param android
     *            The {@link AndroidInstance} that triggered this {@link Event}
     * @param target
     *            The {@link LivingEntity} about to be damaged
     * @param damage
     *            The amount of damage to deal
     */
    @ParametersAreNonnullByDefault
    public AndroidAttackEvent(AndroidInstance android, LivingEntity target, double damage) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        this.android = android;
        this.target = target;
        this.damage = damage;
    }

    /**
     * This method returns the {@link AndroidInstance} who
     * triggered this {@link Event}.
     *
     * @return the involved {@link AndroidInstance}
     */
    @Nonnull
    public AndroidInstance getAndroid() {
        return android;
    }

    /**
     * This method returns the {@link LivingEntity} that is about to be damaged.
     *
     * @return the target {@link LivingEntity}
     */
    @Nonnull
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * This method returns the amount of damage the android will deal.
     *
     * @return the damage
     */
    public double getDamage() {
        return damage;
    }

    /**
     * This method sets the amount of damage the android will deal.
     *
     * @param damage
     *            The new damage, must not be negative
     */
    public void setDamage(double damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage must not be negative");
        }

        this.damage = damage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.StomperBoots;

/**
 * This {@link PlayerEvent} is fired for every nearby {@link LivingEntity} that a pair of
 * {@link StomperBoots} is about to push away and damage when the wearer takes fall damage.
 * <p>
 * Cancelling this event skips the push and the damage for this entity only; other nearby
 * entities are still affected. Addons may also adjust the dealt damage via
 * {@link #setDamage(double)}.
 * <p>
 * Addons may also override the push itself via {@link #setPushVelocity(Vector)}, e.g. to
 * fling victims upwards instead of away. By default the velocity is {@code null} and the
 * entity is pushed with the computed shockwave (away from the wearer, scaled by 1.4),
 * exactly as before; setting it back to {@code null} restores that default.
 *
 * @author Zurker
 *
 * @see StomperBoots
 */
public class StomperBootsPushEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final StomperBoots boots;
    private final LivingEntity entity;

    private double damage;
    private Vector pushVelocity;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public StomperBootsPushEvent(Player player, StomperBoots boots, LivingEntity entity, double damage) {
        super(player);
        Validate.notNull(boots, "The StomperBoots must not be null");
        Validate.notNull(entity, "The pushed Entity must not be null");

        this.boots = boots;
        this.entity = entity;
        this.damage = damage;
    }

    /**
     * This returns the {@link StomperBoots} that caused the stomp.
     *
     * @return The {@link StomperBoots}
     */
    @Nonnull
    public StomperBoots getBoots() {
        return boots;
    }

    /**
     * This returns the {@link LivingEntity} that is about to be pushed and damaged.
     *
     * @return The affected {@link LivingEntity}
     */
    @Nonnull
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * This returns the damage this entity is about to take (half the fall damage by default).
     *
     * @return The damage to deal
     */
    public double getDamage() {
        return damage;
    }

    /**
     * This sets the damage this entity will take.
     *
     * @param damage
     *            The damage to deal, must be a finite number (not NaN or infinite)
     */
    public void setDamage(double damage) {
        Validate.isTrue(Double.isFinite(damage), "The damage must be a finite number, received: " + damage);

        this.damage = damage;
    }

    /**
     * This returns the velocity this entity will be pushed with, or {@code null} when the
     * computed shockwave (away from the wearer, scaled by 1.4) is applied.
     *
     * @return The push velocity, or {@code null} for the computed shockwave
     * @see #setPushVelocity(Vector)
     */
    @Nullable
    public Vector getPushVelocity() {
        return pushVelocity;
    }

    /**
     * This overrides the velocity this entity is pushed with, redirecting or
     * strengthening the push. Passing {@code null} restores the computed shockwave.
     *
     * @param pushVelocity
     *            The push velocity, or {@code null} for the computed shockwave
     */
    public void setPushVelocity(@Nullable Vector pushVelocity) {
        this.pushVelocity = pushVelocity;
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

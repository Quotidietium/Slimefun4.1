package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.TelepositionScroll;

/**
 * This {@link PlayerEvent} is fired for every nearby {@link LivingEntity} that a
 * {@link TelepositionScroll} is about to rotate by 180 degrees.
 * <p>
 * Cancelling this event skips the rotation for this entity only; other nearby entities
 * are still rotated. Addons may also change the applied yaw via {@link #setNewYaw(float)}.
 *
 * @author Zurker
 *
 * @see TelepositionScroll
 */
public class TelepositionScrollRotateEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final TelepositionScroll scroll;
    private final LivingEntity entity;

    private float newYaw;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public TelepositionScrollRotateEvent(Player player, TelepositionScroll scroll, LivingEntity entity, float newYaw) {
        super(player);
        Validate.notNull(scroll, "The TelepositionScroll must not be null");
        Validate.notNull(entity, "The rotated Entity must not be null");

        this.scroll = scroll;
        this.entity = entity;
        this.newYaw = newYaw;
    }

    /**
     * This returns the {@link TelepositionScroll} that is being used.
     *
     * @return The {@link TelepositionScroll}
     */
    @Nonnull
    public TelepositionScroll getScroll() {
        return scroll;
    }

    /**
     * This returns the {@link LivingEntity} that is about to be rotated.
     *
     * @return The rotated {@link LivingEntity}
     */
    @Nonnull
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * This returns the yaw the {@link LivingEntity} is about to be rotated to.
     *
     * @return The new yaw
     */
    public float getNewYaw() {
        return newYaw;
    }

    /**
     * This sets the yaw the {@link LivingEntity} will be rotated to.
     *
     * @param newYaw
     *            The new yaw, must be a finite value
     */
    public void setNewYaw(float newYaw) {
        Validate.isTrue(Float.isFinite(newYaw), "The new yaw must be finite, received: " + newYaw);
        this.newYaw = newYaw;
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

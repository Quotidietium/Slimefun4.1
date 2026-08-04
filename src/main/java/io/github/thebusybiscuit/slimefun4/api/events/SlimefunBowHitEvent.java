package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SlimefunBow;

/**
 * This event is fired whenever an {@link Arrow} that was fired from a {@link SlimefunBow}
 * hits a {@link LivingEntity}, after the underlying {@link EntityDamageByEntityEvent} passed
 * the cancellation checks but before the bow's {@link BowShootHandler} is invoked.
 *
 * <p>
 * Cancelling this event skips the {@link BowShootHandler}: none of the bow's special hit
 * effects are applied. The underlying {@link EntityDamageByEntityEvent} is left untouched,
 * so the arrow still deals its normal damage, mirroring the semantics of
 * {@link SlimefunBowShootEvent}.
 * </p>
 *
 * @author Zurker
 *
 * @see SlimefunBowShootEvent
 * @see SlimefunBow
 * @see BowShootHandler
 *
 */
public class SlimefunBowHitEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunBow bow;
    private final Arrow arrow;
    private final LivingEntity target;
    private final EntityDamageByEntityEvent underlyingEvent;
    private boolean cancelled;

    public SlimefunBowHitEvent(@Nonnull Player player, @Nonnull SlimefunBow bow, @Nonnull Arrow arrow, @Nonnull LivingEntity target, @Nonnull EntityDamageByEntityEvent underlyingEvent) {
        super(player);

        Validate.notNull(bow, "The SlimefunBow cannot be null");
        Validate.notNull(arrow, "The Arrow cannot be null");
        Validate.notNull(target, "The target cannot be null");
        Validate.notNull(underlyingEvent, "The underlying EntityDamageByEntityEvent cannot be null");

        this.bow = bow;
        this.arrow = arrow;
        this.target = target;
        this.underlyingEvent = underlyingEvent;
    }

    /**
     * The {@link SlimefunBow} the {@link Arrow} was fired from.
     *
     * @return The {@link SlimefunBow}
     */
    @Nonnull
    public SlimefunBow getBow() {
        return bow;
    }

    /**
     * The {@link Arrow} projectile that hit the target.
     *
     * @return The {@link Arrow}
     */
    @Nonnull
    public Arrow getArrow() {
        return arrow;
    }

    /**
     * The {@link LivingEntity} that was hit by the {@link Arrow}.
     *
     * @return The hit {@link LivingEntity}
     */
    @Nonnull
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * The underlying Bukkit {@link EntityDamageByEntityEvent}.
     *
     * @return The wrapped {@link EntityDamageByEntityEvent}
     */
    @Nonnull
    public EntityDamageByEntityEvent getEntityDamageByEntityEvent() {
        return underlyingEvent;
    }

    /**
     * Whether the bow's hit effect will be suppressed.
     *
     * @return {@code true} if the {@link BowShootHandler} will be skipped
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * If cancelled, the {@link BowShootHandler} is not invoked and none of the bow's
     * hit effects apply. The arrow still deals its normal damage.
     *
     * @param cancel
     *            {@code true} to skip the bow's hit effect
     */
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

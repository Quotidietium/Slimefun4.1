package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves.WindStaff;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link WindStaff}
 * with enough food and the staff is about to launch them: a forward velocity is about to be
 * applied (and a little hunger consumed).
 * <p>
 * The launch velocity is modifiable via {@link #setVelocity(Vector)} - an addon may redirect or
 * scale the launch. Cancelling this event skips the launch entirely: no velocity is applied and
 * no hunger is consumed.
 *
 * @author Zurker
 *
 * @see WindStaff
 */
public class WindStaffLaunchEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final WindStaff windStaff;
    private Vector velocity;

    private boolean cancelled;

    public WindStaffLaunchEvent(@Nonnull Player player, @Nonnull WindStaff windStaff, @Nonnull Vector velocity) {
        super(player);
        Validate.notNull(windStaff, "The WindStaff must not be null");
        Validate.notNull(velocity, "The velocity must not be null");

        this.windStaff = windStaff;
        this.velocity = velocity;
    }

    /**
     * This returns the {@link WindStaff} that is launching the player.
     *
     * @return The {@link WindStaff}
     */
    @Nonnull
    public WindStaff getWindStaff() {
        return windStaff;
    }

    /**
     * This returns the launch velocity that is about to be applied. Use
     * {@link #setVelocity(Vector)} to change it.
     *
     * @return The launch velocity
     */
    @Nonnull
    public Vector getVelocity() {
        return velocity;
    }

    /**
     * This overrides the launch velocity that will be applied.
     *
     * @param velocity
     *            The new velocity, not {@code null}
     */
    public void setVelocity(@Nullable Vector velocity) {
        Validate.notNull(velocity, "The velocity must not be null");
        Validate.isTrue(Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ()), "The vector must have finite components, received: " + velocity);

        this.velocity = velocity;
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

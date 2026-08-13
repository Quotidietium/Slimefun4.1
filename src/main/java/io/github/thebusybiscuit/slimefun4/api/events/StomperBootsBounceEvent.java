package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.StomperBoots;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} wearing {@link StomperBoots} takes
 * fall damage and is about to be launched back up by the stomp effect, before any nearby entity
 * is pushed away.
 * <p>
 * Addons may adjust the launch strength via {@link #setBounceVelocity(Vector)} or cancel the
 * launch entirely. Cancelling this event only suppresses the wearer's own bounce; the shockwave
 * that pushes and damages nearby entities still runs and can be controlled per entity via
 * {@link StomperBootsPushEvent}.
 *
 * @author Zurker
 *
 * @see StomperBoots
 * @see StomperBootsPushEvent
 */
public class StomperBootsBounceEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final StomperBoots boots;
    private Vector bounceVelocity;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public StomperBootsBounceEvent(Player player, StomperBoots boots, Vector bounceVelocity) {
        super(player);
        Validate.notNull(boots, "The StomperBoots must not be null");
        Validate.notNull(bounceVelocity, "The bounce velocity must not be null");

        this.boots = boots;
        this.bounceVelocity = bounceVelocity;
    }

    /**
     * This returns the {@link StomperBoots} that triggered the bounce.
     *
     * @return The {@link StomperBoots}
     */
    @Nonnull
    public StomperBoots getBoots() {
        return boots;
    }

    /**
     * This returns the velocity that is about to launch the {@link Player} back up.
     *
     * @return The bounce velocity
     */
    @Nonnull
    public Vector getBounceVelocity() {
        return bounceVelocity;
    }

    /**
     * This sets the velocity that will launch the {@link Player} back up.
     *
     * @param bounceVelocity
     *            The new bounce velocity, must not be null
     */
    public void setBounceVelocity(@Nonnull Vector bounceVelocity) {
        Validate.notNull(bounceVelocity, "The bounce velocity must not be null");
        Validate.isTrue(Double.isFinite(bounceVelocity.getX()) && Double.isFinite(bounceVelocity.getY()) && Double.isFinite(bounceVelocity.getZ()), "The vector must have finite components, received: " + bounceVelocity);

        this.bounceVelocity = bounceVelocity;
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

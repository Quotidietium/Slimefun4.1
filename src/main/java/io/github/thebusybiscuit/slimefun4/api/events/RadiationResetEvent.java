package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} dies while carrying a
 * radiation exposure level and Slimefun is about to clear it: death cleanses any
 * accumulated radiation.
 * <p>
 * Cancelling this event vetoes the reset: the exposure level carries through the
 * death and is still present when the {@link Player} respawns. The post-death grace
 * period is skipped in that case as well, so radiation symptoms resume as soon as
 * the respawned {@link Player} is ticked again.
 * <p>
 * The event is not fired when the exposure level is already zero: nothing would be
 * reset. It is fired synchronously from the {@link PlayerDeathEvent}. Note that the
 * continuous exposure accumulation and decay is governed by the
 * {@link RadiationExposureEvent} and the application of symptoms by the
 * {@link RadiationDamageEvent} - both are fired from the radiation task instead.
 *
 * @author Zurker
 *
 * @see RadiationExposureEvent
 * @see RadiationDamageEvent
 * @see RadiationUtils
 */
public class RadiationResetEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final int exposureBefore;
    private final PlayerDeathEvent deathEvent;

    private boolean cancelled;

    public RadiationResetEvent(@Nonnull Player player, int exposureBefore, @Nonnull PlayerDeathEvent deathEvent) {
        super(player);

        Validate.isTrue(exposureBefore > 0, "The exposure level before the reset must be positive");
        Validate.notNull(deathEvent, "The death event must not be null");

        this.exposureBefore = exposureBefore;
        this.deathEvent = deathEvent;
    }

    /**
     * This returns the exposure level the {@link Player} died with, which is about
     * to be cleared.
     *
     * @return The exposure level before the reset
     */
    public int getExposureBefore() {
        return exposureBefore;
    }

    /**
     * This returns the original {@link PlayerDeathEvent} that triggered this reset.
     *
     * @return The {@link PlayerDeathEvent}
     */
    @Nonnull
    public PlayerDeathEvent getDeathEvent() {
        return deathEvent;
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

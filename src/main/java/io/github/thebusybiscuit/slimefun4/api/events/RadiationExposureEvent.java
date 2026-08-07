package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.RadiationTask;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player}'s radiation exposure
 * level is about to change during a run of the {@link RadiationTask}.
 * <p>
 * The exposure rises while the {@link Player} carries {@link Radioactive} items
 * (a positive change, proportional to the items' radioactivity) and decays by one
 * otherwise (a negative change), including while the {@link Player} is fully
 * protected or in creative mode. Cancelling this event vetoes the change: the
 * exposure level stays as it is. When an accumulation is vetoed, the
 * first-exposure warning message is skipped as well.
 * <p>
 * The event is not fired for a decay that would not change anything (the exposure
 * level is already zero). Note that the {@link RadiationTask} runs on an
 * asynchronous thread, so this event is asynchronous in production. The
 * application of radiation symptoms is governed separately by the
 * {@link RadiationDamageEvent}, which is fired after the exposure has settled.
 *
 * @author Zurker
 *
 * @see RadiationDamageEvent
 * @see RadiationTask
 * @see RadiationUtils
 * @see Radioactive
 */
public class RadiationExposureEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final int exposureBefore;
    private final int exposureChange;

    private boolean cancelled;

    public RadiationExposureEvent(@Nonnull Player player, int exposureBefore, int exposureChange) {
        super(player, !Bukkit.isPrimaryThread());

        Validate.notNull(player, "The Player must not be null");
        Validate.isTrue(exposureBefore >= 0, "The exposure level before the change must not be negative");
        Validate.isTrue(exposureChange != 0, "An exposure change of zero is meaningless");

        this.exposureBefore = exposureBefore;
        this.exposureChange = exposureChange;
    }

    /**
     * This returns the exposure level before the change is applied.
     *
     * @return The exposure level before the change
     */
    public int getExposureBefore() {
        return exposureBefore;
    }

    /**
     * This returns the signed exposure delta that is about to be applied.
     * A positive value means the {@link Player} is accumulating exposure from
     * {@link Radioactive} items, a negative value means the exposure is decaying.
     *
     * @return The signed exposure change
     */
    public int getExposureChange() {
        return exposureChange;
    }

    /**
     * This returns the exposure level after the change, clamped to the range
     * enforced by {@link RadiationUtils} (0 to 100).
     *
     * @return The resulting exposure level
     */
    public int getExposureAfter() {
        // Mirrors the clamping of RadiationUtils.addExposure() / removeExposure()
        return Math.max(0, Math.min(100, exposureBefore + exposureChange));
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

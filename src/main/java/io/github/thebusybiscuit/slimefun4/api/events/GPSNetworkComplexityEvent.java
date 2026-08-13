package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This {@link Event} is fired when the GPS network complexity for a player changes, as a
 * result of a {@link io.github.thebusybiscuit.slimefun4.implementation.items.gps.GPSTransmitter}
 * going online or offline.
 * <p>
 * The complexity determines the maximum teleportation range and speed. Higher complexity
 * (taller transmitters, more transmitters) means faster and farther teleports. The event
 * is informational only (not cancellable): the transmitter status has already changed.
 * <p>
 * The event fires from the (asynchronous) transmitter tick thread, so
 * {@link #isAsynchronous()} reports {@code true} and listeners must not touch live
 * Bukkit state without scheduling a sync task. Add-ons can use it for monitoring
 * network upgrades/downgrades or triggering actions on complexity thresholds.
 *
 * @author Zurker
 *
 * @see GPSTransmitterStatusEvent
 * @see io.github.thebusybiscuit.slimefun4.api.gps.GPSNetwork
 */
public class GPSNetworkComplexityEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final UUID uuid;
    private final int oldComplexity;
    private final int newComplexity;

    public GPSNetworkComplexityEvent(@Nonnull UUID uuid, int oldComplexity, int newComplexity) {
        /*
         * Fired from the GPSTransmitter's (asynchronous) BlockTicker - calling a
         * synchronously-declared event from that thread would throw instead.
         */
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(uuid, "The UUID must not be null");

        this.uuid = uuid;
        this.oldComplexity = oldComplexity;
        this.newComplexity = newComplexity;
    }

    /**
     * This returns the {@link UUID} of the player whose network complexity changed.
     *
     * @return The player {@link UUID}
     */
    @Nonnull
    public UUID getUUID() {
        return uuid;
    }

    /**
     * This returns the network complexity before the change.
     *
     * @return The old complexity
     */
    public int getOldComplexity() {
        return oldComplexity;
    }

    /**
     * This returns the network complexity after the change.
     *
     * @return The new complexity
     */
    public int getNewComplexity() {
        return newComplexity;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.gps.GPSNetwork;
import io.github.thebusybiscuit.slimefun4.implementation.items.gps.GPSTransmitter;

/**
 * This {@link Event} is fired whenever a {@link GPSTransmitter} actually changes
 * its status in the {@link GPSNetwork}: when it comes online or goes offline.
 * <p>
 * The event only fires on real flips. A transmitter's ticker re-asserts its
 * status every tick, so idempotent updates (marking an online transmitter online
 * again) do not fire this event.
 * <p>
 * This event is not cancellable: the ticker re-asserts the state on the next
 * tick anyway, so a veto would be silently overridden. To keep a transmitter
 * off a network, gate its energy supply instead. It may be fired from the
 * asynchronous ticker Thread, use {@link Event#isAsynchronous()} to check and
 * {@link Bukkit#getPlayer(UUID)} (or {@link #getOwnerPlayer()}) to resolve the
 * owner, who may be offline.
 *
 * @author Zurker
 *
 * @see GEOScanEvent
 * @see TeleportationStartEvent
 */
public class GPSTransmitterStatusEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Location location;
    private final UUID owner;
    private final boolean online;

    public GPSTransmitterStatusEvent(@Nonnull Location location, @Nonnull UUID owner, boolean online) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(owner, "The owner UUID must not be null");

        this.location = location;
        this.owner = owner;
        this.online = online;
    }

    /**
     * This returns the {@link Location} of the {@link GPSTransmitter} that
     * changed its status.
     *
     * @return The {@link Location} of the {@link GPSTransmitter}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the {@link UUID} of the {@link Player} who owns the
     * {@link GPSTransmitter}.
     *
     * @return The owner's {@link UUID}
     */
    @Nonnull
    public UUID getOwner() {
        return owner;
    }

    /**
     * This returns the {@link Player} who owns the {@link GPSTransmitter},
     * or null if they are not online.
     *
     * @return The owning {@link Player}, or null
     */
    @Nullable
    public Player getOwnerPlayer() {
        return Bukkit.getPlayer(owner);
    }

    /**
     * This returns the new status of the {@link GPSTransmitter}: true if it
     * came online, false if it went offline.
     *
     * @return Whether the {@link GPSTransmitter} is now online
     */
    public boolean isOnline() {
        return online;
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

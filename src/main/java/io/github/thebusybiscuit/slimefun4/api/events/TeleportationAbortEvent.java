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

import io.github.thebusybiscuit.slimefun4.api.gps.TeleportationManager;

/**
 * This {@link Event} is fired whenever an active teleportation through the
 * {@link TeleportationManager} is aborted before the {@link Player} arrives, either
 * because they interrupted it (moved away, disconnected or otherwise became invalid)
 * or because the asynchronous teleport itself failed.
 * <p>
 * This event is not cancellable - the teleportation is already being torn down. It
 * complements {@link TeleportationStartEvent} and {@link TeleportationCompleteEvent},
 * so addons can observe the full lifecycle of a teleportation.
 *
 * @author Zurker
 *
 * @see TeleportationStartEvent
 * @see TeleportationCompleteEvent
 * @see TeleportationManager
 */
public class TeleportationAbortEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The reason a teleportation was aborted.
     */
    public enum AbortReason {

        /**
         * The {@link Player} interrupted the teleportation, e.g. by moving away from
         * the teleporter, disconnecting or switching worlds.
         */
        INTERRUPTED,

        /**
         * The asynchronous teleport to the destination failed.
         */
        TELEPORT_FAILED
    }

    private final UUID uuid;
    private final Location destination;
    private final AbortReason reason;

    public TeleportationAbortEvent(@Nonnull UUID uuid, @Nonnull Location destination, @Nonnull AbortReason reason) {
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(uuid, "The UUID must not be null");
        Validate.notNull(destination, "The destination must not be null");
        Validate.notNull(reason, "The abort reason must not be null");

        this.uuid = uuid;
        this.destination = destination;
        this.reason = reason;
    }

    /**
     * This returns the {@link UUID} of the {@link Player} whose teleportation was
     * aborted.
     *
     * @return The {@link UUID} of the {@link Player}
     */
    @Nonnull
    public UUID getUUID() {
        return uuid;
    }

    /**
     * This returns the {@link Player} whose teleportation was aborted, or null if they
     * are offline.
     *
     * @return The affected {@link Player} or null
     */
    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /**
     * This returns the {@link Location} the {@link Player} would have been teleported
     * to.
     *
     * @return The destination {@link Location}
     */
    @Nonnull
    public Location getDestination() {
        return destination;
    }

    /**
     * This returns why the teleportation was aborted.
     *
     * @return The {@link AbortReason}
     */
    @Nonnull
    public AbortReason getReason() {
        return reason;
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

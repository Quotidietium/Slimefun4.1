package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.gps.TeleportationManager;
import io.github.thebusybiscuit.slimefun4.api.gps.Waypoint;

/**
 * This {@link Event} is fired whenever a {@link Player} starts a teleportation
 * through the {@link TeleportationManager}, e.g. by clicking a {@link Waypoint}
 * in a Teleporter GUI.
 * <p>
 * Cancelling this event prevents the teleportation from starting: the progress
 * sequence never begins and the {@link Player} stays where they are.
 * <p>
 * The destination can be redirected via {@link #setDestination(Location)} before the
 * teleportation begins, allowing addons to redirect or adjust the target location.
 * <p>
 * The teleportation time is normally derived from the network complexity and the
 * distance. Addons may override it via {@link #setTeleportationTime(int)} - e.g. for
 * fixed-time or instant teleports - independently of the complexity/distance formula.
 *
 * @author Zurker
 *
 * @see TeleportationCompleteEvent
 * @see TeleportationManager
 */
public class TeleportationStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final UUID uuid;
    private final int complexity;
    private final Location source;
    private Location destination;
    private final boolean resistance;

    private int teleportationTime = -1;
    private boolean cancelled;

    public TeleportationStartEvent(@Nonnull UUID uuid, int complexity, @Nonnull Location source, @Nonnull Location destination, boolean resistance) {
        super(!Bukkit.isPrimaryThread());

        // Mirror the setter invariant (setDestination): a destination without a World or with
        // non-finite coordinates would break the teleport computation downstream.
        Validate.notNull(uuid, "The UUID must not be null");
        Validate.notNull(source, "The source must not be null");
        Validate.notNull(destination, "The destination must not be null");
        Validate.notNull(destination.getWorld(), "The destination must have a World");
        Validate.isTrue(Double.isFinite(destination.getX()) && Double.isFinite(destination.getY()) && Double.isFinite(destination.getZ()), "The destination must have finite coordinates, received: " + destination);

        this.uuid = uuid;
        this.complexity = complexity;
        this.source = source;
        this.destination = destination;
        this.resistance = resistance;
    }

    /**
     * This returns the {@link UUID} of the teleporting {@link Player}.
     *
     * @return The {@link UUID} of the teleporting {@link Player}
     */
    @Nonnull
    public UUID getUUID() {
        return uuid;
    }

    /**
     * This returns the teleporting {@link Player}, or null if they are offline.
     *
     * @return The teleporting {@link Player} or null
     */
    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /**
     * This returns the complexity of the GPS network used for this teleportation.
     *
     * @return The network complexity
     */
    public int getComplexity() {
        return complexity;
    }

    /**
     * This returns the {@link Location} the {@link Player} teleports from.
     *
     * @return The source {@link Location}
     */
    @Nonnull
    public Location getSource() {
        return source;
    }

    /**
     * This returns the {@link Location} the {@link Player} teleports to.
     *
     * @return The destination {@link Location}
     */
    @Nonnull
    public Location getDestination() {
        return destination;
    }

    /**
     * This sets the {@link Location} the {@link Player} will teleport to, overriding
     * the original destination. The teleportation time is recalculated from the new
     * distance.
     *
     * @param destination
     *            The new destination {@link Location}, must not be null, must have a
     *            non-null {@link org.bukkit.World} and finite coordinates
     */
    public void setDestination(@Nonnull Location destination) {
        Validate.notNull(destination, "The destination must not be null");
        Validate.notNull(destination.getWorld(), "The destination must have a World");
        Validate.isTrue(Double.isFinite(destination.getX()) && Double.isFinite(destination.getY()) && Double.isFinite(destination.getZ()), "The destination must have finite coordinates, received: " + destination);
        this.destination = destination;
    }

    /**
     * This returns whether the {@link Player} will receive a brief invulnerability
     * effect upon arrival.
     *
     * @return Whether the resistance effect will be applied
     */
    public boolean hasResistance() {
        return resistance;
    }

    /**
     * This returns the teleportation time override, measured in 500ms intervals
     * (a value of {@code 2} means one second, {@code 100} means fifty seconds).
     * A value of {@code -1} means no override: the time is derived from the network
     * complexity and the distance, see
     * {@link TeleportationManager#getTeleportationTime(int, Location, Location)}.
     *
     * @return The teleportation time override, or {@code -1} for the computed default
     */
    public int getTeleportationTime() {
        return teleportationTime;
    }

    /**
     * This overrides the teleportation time, measured in 500ms intervals
     * (a value of {@code 2} means one second, {@code 100} means fifty seconds).
     * A value of {@code 1} teleports on the next progress update, effectively instantly.
     *
     * @param teleportationTime
     *            The teleportation time in 500ms intervals, must be at least 1
     */
    public void setTeleportationTime(int teleportationTime) {
        Validate.isTrue(teleportationTime >= 1, "The teleportation time must be at least 1");
        this.teleportationTime = teleportationTime;
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

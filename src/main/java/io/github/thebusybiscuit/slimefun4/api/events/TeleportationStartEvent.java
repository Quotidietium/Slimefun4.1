package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    private final Location destination;
    private final boolean resistance;

    private boolean cancelled;

    public TeleportationStartEvent(@Nonnull UUID uuid, int complexity, @Nonnull Location source, @Nonnull Location destination, boolean resistance) {
        super(!Bukkit.isPrimaryThread());

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
     * This returns whether the {@link Player} will receive a brief invulnerability
     * effect upon arrival.
     *
     * @return Whether the resistance effect will be applied
     */
    public boolean hasResistance() {
        return resistance;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.gps.TeleportationManager;

/**
 * This {@link Event} is fired whenever a teleportation through the
 * {@link TeleportationManager} has successfully completed and the {@link Player}
 * has arrived at the destination.
 * <p>
 * This event is not cancellable - the teleportation already happened. It is fired
 * on the main thread.
 *
 * @author Zurker
 *
 * @see TeleportationStartEvent
 * @see TeleportationManager
 */
public class TeleportationCompleteEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final UUID uuid;
    private final Location destination;
    private final boolean resistance;

    public TeleportationCompleteEvent(@Nonnull UUID uuid, @Nonnull Location destination, boolean resistance) {
        super(!Bukkit.isPrimaryThread());

        this.uuid = uuid;
        this.destination = destination;
        this.resistance = resistance;
    }

    /**
     * This returns the {@link UUID} of the teleported {@link Player}.
     *
     * @return The {@link UUID} of the teleported {@link Player}
     */
    @Nonnull
    public UUID getUUID() {
        return uuid;
    }

    /**
     * This returns the teleported {@link Player}, or null if they are offline.
     *
     * @return The teleported {@link Player} or null
     */
    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /**
     * This returns the {@link Location} the {@link Player} arrived at.
     *
     * @return The destination {@link Location}
     */
    @Nonnull
    public Location getDestination() {
        return destination;
    }

    /**
     * This returns whether the {@link Player} received the brief invulnerability
     * effect upon arrival.
     *
     * @return Whether the resistance effect was applied
     */
    public boolean hasResistance() {
        return resistance;
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

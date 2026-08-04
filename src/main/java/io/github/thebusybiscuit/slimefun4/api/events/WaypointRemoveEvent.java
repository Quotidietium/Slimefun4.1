package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.gps.Waypoint;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;

/**
 * This {@link Event} is fired whenever a {@link Waypoint} is about to be removed
 * from a {@link PlayerProfile}.
 * <p>
 * Cancelling this event keeps the {@link Waypoint} in the {@link PlayerProfile}.
 *
 * @author Zurker
 *
 * @see WaypointCreateEvent
 * @see Waypoint
 */
public class WaypointRemoveEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerProfile profile;
    private final Waypoint waypoint;

    private boolean cancelled;

    public WaypointRemoveEvent(@Nonnull PlayerProfile profile, @Nonnull Waypoint waypoint) {
        this.profile = profile;
        this.waypoint = waypoint;
    }

    /**
     * This returns the {@link PlayerProfile} the {@link Waypoint} is removed from.
     *
     * @return The {@link PlayerProfile}
     */
    @Nonnull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * This returns the {@link UUID} of the owning {@link Player}.
     *
     * @return The {@link UUID} of the owner
     */
    @Nonnull
    public UUID getUUID() {
        return profile.getUUID();
    }

    /**
     * This returns the owning {@link Player}, or null if they are offline.
     *
     * @return The owning {@link Player} or null
     */
    @Nullable
    public Player getPlayer() {
        return profile.getPlayer();
    }

    /**
     * This returns the {@link Waypoint} that is about to be removed.
     *
     * @return The {@link Waypoint} to remove
     */
    @Nonnull
    public Waypoint getWaypoint() {
        return waypoint;
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

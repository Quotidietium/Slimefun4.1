package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves.StormStaff;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} casts a {@link StormStaff}:
 * a lightning bolt is about to strike the targeted {@link Location}, costing hunger and
 * one use of the staff.
 * <p>
 * Cancelling this event skips the cast entirely: no lightning strikes, no hunger is consumed
 * and the staff is not damaged; the underlying interaction is also not consumed, so it is
 * processed as if the staff had not been cast. Addons may also redirect the strike via
 * {@link #setLocation(Location)}.
 *
 * @author Zurker
 *
 * @see StormStaff
 * @see WindStaffLaunchEvent
 * @see WaterStaffExtinguishEvent
 */
public class StormStaffStrikeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final StormStaff staff;

    private Location location;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public StormStaffStrikeEvent(Player player, StormStaff staff, Location location) {
        super(player);
        Validate.notNull(staff, "The StormStaff must not be null");
        Validate.notNull(location, "The strike Location must not be null");

        this.staff = staff;
        this.location = location;
    }

    /**
     * This returns the {@link StormStaff} that is being cast.
     *
     * @return The {@link StormStaff}
     */
    @Nonnull
    public StormStaff getStaff() {
        return staff;
    }

    /**
     * This returns the {@link Location} the lightning bolt is about to strike.
     *
     * @return The strike {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This sets the {@link Location} the lightning bolt will strike.
     *
     * @param location
     *            The strike {@link Location}
     */
    public void setLocation(@Nonnull Location location) {
        Validate.notNull(location, "The strike Location must not be null");

        this.location = location;
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

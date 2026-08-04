package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorFloor;
import io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} has selected an
 * {@link ElevatorFloor} in an {@link ElevatorPlate}'s floor selector and is about
 * to be teleported there.
 * <p>
 * Cancelling this event prevents the teleportation; the floor selector stays
 * open and the {@link Player} may pick another floor.
 *
 * @author Zurker
 *
 * @see ElevatorPlate
 * @see ElevatorFloor
 */
public class ElevatorTeleportEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ElevatorFloor floor;

    private boolean cancelled;

    public ElevatorTeleportEvent(@Nonnull Player player, @Nonnull ElevatorFloor floor) {
        super(player);

        Validate.notNull(floor, "The floor must not be null");
        this.floor = floor;
    }

    /**
     * This returns the {@link ElevatorFloor} the {@link Player} is about to
     * travel to.
     *
     * @return The destination {@link ElevatorFloor}
     */
    @Nonnull
    public ElevatorFloor getFloor() {
        return floor;
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

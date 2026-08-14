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
 * <p>
 * Addons may also redirect the travel via {@link #setFloor(ElevatorFloor)}, e.g. to
 * send the {@link Player} to a floor they did not pick: both the teleport
 * destination and the arrival title are taken from the replacement floor.
 *
 * @author Zurker
 *
 * @see ElevatorPlate
 * @see ElevatorFloor
 */
public class ElevatorTeleportEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private ElevatorFloor floor;

    private boolean cancelled;

    public ElevatorTeleportEvent(@Nonnull Player player, @Nonnull ElevatorFloor floor) {
        super(player);

        // Mirror the setter invariants (setFloor): a null floor or a floor in a different
        // World than the player must be rejected here as well.
        Validate.notNull(floor, "The floor must not be null");
        Validate.isTrue(floor.getLocation().getWorld().equals(player.getWorld()), "The floor must be in the same World as the player");
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

    /**
     * This sets the {@link ElevatorFloor} the {@link Player} will travel to,
     * overriding the floor they picked. Both the teleport destination and the
     * arrival title are taken from the replacement floor.
     *
     * @param floor
     *            The new destination {@link ElevatorFloor}, must not be null and must
     *            be in the same {@link org.bukkit.World} as the {@link Player} (the
     *            teleport destination is computed in the Player's world, so a floor
     *            from another world would drop them at wrong, unchecked coordinates)
     */
    public void setFloor(@Nonnull ElevatorFloor floor) {
        Validate.notNull(floor, "The floor must not be null");
        Validate.isTrue(floor.getLocation().getWorld().equals(player.getWorld()), "The floor must be in the same World as the player");

        this.floor = floor;
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

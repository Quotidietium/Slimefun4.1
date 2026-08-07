package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorFloor;
import io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} has entered a new
 * name for an {@link ElevatorPlate}'s {@link ElevatorFloor} in the floor editor
 * and the name is about to be stored.
 * <p>
 * Cancelling this event vetoes the rename: the floor keeps its old name, no
 * confirmation is sent and the editor is not re-opened. The proposed name can
 * be adjusted via {@link #setNewName(String)}; the stored name still has any
 * legacy section signs replaced with '&amp;' afterwards, exactly like a typed
 * name would.
 *
 * @author Zurker
 *
 * @see ElevatorTeleportEvent
 * @see ElevatorPlate
 * @see HologramProjectorTextChangeEvent
 */
public class ElevatorFloorRenameEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block elevator;
    private final String previousName;

    private String newName;
    private boolean cancelled;

    public ElevatorFloorRenameEvent(@Nonnull Player player, @Nonnull Block elevator, @Nullable String previousName, @Nonnull String newName) {
        super(player);

        Validate.notNull(elevator, "The elevator block must not be null");
        Validate.notNull(newName, "The new name must not be null");

        this.elevator = elevator;
        this.previousName = previousName;
        this.newName = newName;
    }

    /**
     * This returns the {@link Block} of the {@link ElevatorPlate} being renamed.
     *
     * @return The {@link ElevatorPlate} {@link Block}
     */
    @Nonnull
    public Block getElevator() {
        return elevator;
    }

    /**
     * This returns the name the {@link ElevatorFloor} currently has, or null if
     * it was never named (the floor selector displays a generated name in that
     * case).
     *
     * @return The current floor name, or null
     */
    @Nullable
    public String getPreviousName() {
        return previousName;
    }

    /**
     * This returns the proposed new name for the {@link ElevatorFloor}.
     *
     * @return The new floor name
     */
    @Nonnull
    public String getNewName() {
        return newName;
    }

    /**
     * This sets the name that will be stored for the {@link ElevatorFloor}.
     *
     * @param newName
     *            The name to store, must not be null
     */
    public void setNewName(@Nonnull String newName) {
        Validate.notNull(newName, "The new name must not be null");
        this.newName = newName;
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

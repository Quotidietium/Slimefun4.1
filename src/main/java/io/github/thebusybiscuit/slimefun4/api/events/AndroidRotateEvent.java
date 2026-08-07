package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.Instruction;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;

/**
 * This {@link Event} is fired whenever a {@link ProgrammableAndroid} executes a
 * {@link Instruction#TURN_LEFT} or {@link Instruction#TURN_RIGHT} instruction and is
 * about to rotate: its head and its stored rotation are about to be turned to the
 * next {@link BlockFace} in the rotation cycle.
 * <p>
 * Cancelling this event keeps the {@link ProgrammableAndroid} facing its current
 * direction: neither the head nor the stored rotation is changed.
 *
 * @author Zurker
 *
 * @see ProgrammableAndroid
 * @see AndroidMoveEvent
 */
public class AndroidRotateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AndroidInstance android;
    private final BlockFace previousRotation;
    private final BlockFace newRotation;
    private boolean cancelled;

    /**
     * @param android
     *            The {@link AndroidInstance} that is about to rotate
     * @param previousRotation
     *            The {@link BlockFace} the android is currently facing
     * @param newRotation
     *            The {@link BlockFace} the android is about to face
     */
    @ParametersAreNonnullByDefault
    public AndroidRotateEvent(AndroidInstance android, BlockFace previousRotation, BlockFace newRotation) {
        Validate.notNull(android, "The AndroidInstance must not be null");
        Validate.notNull(previousRotation, "The previous rotation must not be null");
        Validate.notNull(newRotation, "The new rotation must not be null");

        this.android = android;
        this.previousRotation = previousRotation;
        this.newRotation = newRotation;
    }

    /**
     * This method returns the {@link AndroidInstance} that is about to rotate.
     *
     * @return The involved {@link AndroidInstance}
     */
    @Nonnull
    public AndroidInstance getAndroid() {
        return android;
    }

    /**
     * This method returns the {@link BlockFace} the android is currently facing.
     *
     * @return The current rotation
     */
    @Nonnull
    public BlockFace getPreviousRotation() {
        return previousRotation;
    }

    /**
     * This method returns the {@link BlockFace} the android is about to face.
     *
     * @return The new rotation
     */
    @Nonnull
    public BlockFace getNewRotation() {
        return newRotation;
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

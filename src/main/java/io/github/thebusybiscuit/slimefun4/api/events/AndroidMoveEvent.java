package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.Instruction;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;

/**
 * This {@link Event} is fired whenever a {@link ProgrammableAndroid} is about to move into
 * the {@link Block} it is facing, be it through the {@link Instruction#MOVE_FORWARD}
 * instruction or a {@link io.github.thebusybiscuit.slimefun4.implementation.items.androids.MinerAndroid}
 * digging its way forward: the destination was verified to be empty, inside the world
 * border and within the world height, and the android's owner was verified to have
 * permission to place blocks there.
 * <p>
 * Cancelling this event keeps the {@link ProgrammableAndroid} where it is: the destination
 * block is untouched and no block data is moved.
 *
 * @author Zurker
 *
 * @see ProgrammableAndroid
 * @see AndroidMineEvent
 */
public class AndroidMoveEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AndroidInstance android;
    private final Block to;
    private final BlockFace face;
    private boolean cancelled;

    /**
     * @param android
     *            The {@link AndroidInstance} that is about to move
     * @param to
     *            The destination {@link Block}
     * @param face
     *            The {@link BlockFace} the android is moving towards
     */
    @ParametersAreNonnullByDefault
    public AndroidMoveEvent(AndroidInstance android, Block to, BlockFace face) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(android, "The AndroidInstance must not be null");
        Validate.notNull(to, "The destination Block must not be null");
        Validate.notNull(face, "The BlockFace must not be null");

        this.android = android;
        this.to = to;
        this.face = face;
    }

    /**
     * This method returns the {@link AndroidInstance} that is about to move.
     *
     * @return The involved {@link AndroidInstance}
     */
    @Nonnull
    public AndroidInstance getAndroid() {
        return android;
    }

    /**
     * This method returns the destination {@link Block} the android is moving into.
     *
     * @return The destination {@link Block}
     */
    @Nonnull
    public Block getTo() {
        return to;
    }

    /**
     * This method returns the {@link BlockFace} the android is moving towards.
     *
     * @return The movement direction
     */
    @Nonnull
    public BlockFace getFace() {
        return face;
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

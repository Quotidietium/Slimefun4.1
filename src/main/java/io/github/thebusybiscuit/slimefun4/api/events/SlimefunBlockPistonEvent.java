package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This {@link Event} is fired whenever a piston is about to move (extend or retract) and
 * Slimefun is about to cancel the move because it would push, pull or otherwise affect a
 * Slimefun block: pistons must not be abused to break or duplicate Slimefun blocks.
 * <p>
 * The {@code protectedBlock} is the Slimefun block that triggered the cancellation - either
 * the piston itself or one of the blocks in its moved list. Cancelling this event vetoes the
 * protection: the piston is allowed to move and the affected Slimefun block is left to
 * vanilla piston behavior.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.listeners.BlockPhysicsListener
 */
public class SlimefunBlockPistonEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block piston;
    private final BlockFace direction;
    private final Block protectedBlock;
    private final boolean retract;

    private boolean cancelled;

    public SlimefunBlockPistonEvent(@Nonnull Block piston, @Nonnull BlockFace direction, @Nonnull Block protectedBlock, boolean retract) {
        Validate.notNull(piston, "The piston Block must not be null");
        Validate.notNull(direction, "The direction must not be null");
        Validate.notNull(protectedBlock, "The protected Block must not be null");

        this.piston = piston;
        this.direction = direction;
        this.protectedBlock = protectedBlock;
        this.retract = retract;
    }

    /**
     * This returns the {@link Block} of the piston.
     *
     * @return The piston {@link Block}
     */
    @Nonnull
    public Block getPiston() {
        return piston;
    }

    /**
     * This returns the {@link BlockFace} the piston is pushing toward (extend) or pulling
     * from (retract).
     *
     * @return The {@link BlockFace}
     */
    @Nonnull
    public BlockFace getDirection() {
        return direction;
    }

    /**
     * This returns the Slimefun {@link Block} that triggered the cancellation: either the
     * piston itself (when it holds Slimefun data) or one of the blocks the piston tried to
     * move.
     *
     * @return The protected {@link Block}
     */
    @Nonnull
    public Block getProtectedBlock() {
        return protectedBlock;
    }

    /**
     * This returns whether the piston is retracting (sticky piston pulling). {@code false}
     * means the piston is extending (pushing).
     *
     * @return Whether this is a retraction
     */
    public boolean isRetract() {
        return retract;
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

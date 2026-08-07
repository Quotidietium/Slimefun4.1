package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.HologramProjector;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} adjusts the vertical
 * offset of a {@link HologramProjector}'s hologram through its editor.
 * <p>
 * The new offset is mutable: {@link #setNewOffset(double)} lets addons clamp or
 * override it before it is applied. Cancelling this event vetoes the adjustment
 * entirely: the hologram keeps its previous offset and the editor is not reopened.
 *
 * @author Zurker
 *
 * @see HologramProjector
 * @see HologramProjectorTextChangeEvent
 */
public class HologramProjectorOffsetChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final HologramProjector projector;
    private final Block block;
    private final double previousOffset;

    private double newOffset;
    private boolean cancelled;

    public HologramProjectorOffsetChangeEvent(@Nonnull Player player, @Nonnull HologramProjector projector, @Nonnull Block block, double previousOffset, double newOffset) {
        super(player);
        Validate.notNull(projector, "The HologramProjector must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.isTrue(Double.isFinite(previousOffset), "The previous offset must be a finite number");
        Validate.isTrue(Double.isFinite(newOffset), "The new offset must be a finite number");

        this.projector = projector;
        this.block = block;
        this.previousOffset = previousOffset;
        this.newOffset = newOffset;
    }

    /**
     * This returns the {@link HologramProjector} whose hologram offset is being adjusted.
     *
     * @return The {@link HologramProjector}
     */
    @Nonnull
    public HologramProjector getProjector() {
        return projector;
    }

    /**
     * This returns the {@link Block} of the {@link HologramProjector}.
     *
     * @return The {@link HologramProjector} {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the offset the hologram currently floats at, relative to the
     * projector {@link Block}.
     *
     * @return The previous offset
     */
    public double getPreviousOffset() {
        return previousOffset;
    }

    /**
     * This returns the offset the hologram is about to be moved to, relative to the
     * projector {@link Block}.
     *
     * @return The new offset
     */
    public double getNewOffset() {
        return newOffset;
    }

    /**
     * Overrides the offset the hologram is about to be moved to, e.g. to clamp it.
     *
     * @param newOffset
     *            The replacement offset
     */
    public void setNewOffset(double newOffset) {
        Validate.isTrue(Double.isFinite(newOffset), "The new offset must be a finite number");
        this.newOffset = newOffset;
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

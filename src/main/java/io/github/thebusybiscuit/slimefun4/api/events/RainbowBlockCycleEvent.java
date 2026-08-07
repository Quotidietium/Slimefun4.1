package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.RainbowBlock;

/**
 * This {@link Event} is fired whenever a rainbow block is about to cycle to the next
 * {@link Material} of its color sequence.
 * <p>
 * The event fires once per block per tick. The target {@link Material} is mutable:
 * {@link #setNextMaterial(Material)} lets addons override the color the block changes
 * into. Cancelling this event skips the color change for this tick, so the block keeps
 * its current {@link Material}.
 * <p>
 * Note that the color sequence itself is global to the item's ticker and advances
 * independently of this event, so a cancelled block simply rejoins the sequence at
 * whatever color comes next.
 *
 * @author Zurker
 *
 * @see RainbowBlock
 */
public class RainbowBlockCycleEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem item;
    private final Block block;
    private final Material previousMaterial;

    private Material nextMaterial;
    private boolean cancelled;

    public RainbowBlockCycleEvent(@Nonnull SlimefunItem item, @Nonnull Block block, @Nonnull Material previousMaterial, @Nonnull Material nextMaterial) {
        Validate.notNull(item, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(previousMaterial, "The previous Material must not be null");
        Validate.notNull(nextMaterial, "The next Material must not be null");

        this.item = item;
        this.block = block;
        this.previousMaterial = previousMaterial;
        this.nextMaterial = nextMaterial;
    }

    /**
     * This returns the {@link SlimefunItem} of the cycling block, e.g. a
     * {@link RainbowBlock}.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getItem() {
        return item;
    }

    /**
     * This returns the {@link Block} that is about to change its color.
     *
     * @return The cycling {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link Material} the {@link Block} currently has.
     *
     * @return The current {@link Material}
     */
    @Nonnull
    public Material getPreviousMaterial() {
        return previousMaterial;
    }

    /**
     * This returns the {@link Material} the {@link Block} is about to change into.
     *
     * @return The next {@link Material}
     */
    @Nonnull
    public Material getNextMaterial() {
        return nextMaterial;
    }

    /**
     * Overrides the {@link Material} the {@link Block} is about to change into.
     *
     * @param nextMaterial
     *            The {@link Material} to change into instead
     */
    public void setNextMaterial(@Nonnull Material nextMaterial) {
        Validate.notNull(nextMaterial, "The next Material must not be null");
        this.nextMaterial = nextMaterial;
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

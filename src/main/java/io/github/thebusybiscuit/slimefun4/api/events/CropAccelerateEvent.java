package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.CropGrowthAccelerator;

/**
 * This {@link Event} is fired whenever a {@link CropGrowthAccelerator} has found a
 * crop within its radius that can still grow and is about to accelerate its growth:
 * the energy and the fertilizer are about to be consumed and the crop's age is about
 * to be increased.
 * <p>
 * Cancelling this event skips this crop: no energy or fertilizer is consumed, the crop
 * keeps its age and the accelerator continues scanning the other crops within its
 * radius during this tick.
 *
 * @author Zurker
 *
 * @see CropGrowthAccelerator
 */
public class CropAccelerateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final CropGrowthAccelerator accelerator;
    private final Block block;
    private final Block crop;
    private final ItemStack fertilizer;

    private boolean cancelled;

    public CropAccelerateEvent(@Nonnull CropGrowthAccelerator accelerator, @Nonnull Block block, @Nonnull Block crop, @Nonnull ItemStack fertilizer) {
        Validate.notNull(accelerator, "The CropGrowthAccelerator must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(crop, "The crop Block must not be null");
        Validate.notNull(fertilizer, "The fertilizer must not be null");

        this.accelerator = accelerator;
        this.block = block;
        this.crop = crop;
        this.fertilizer = fertilizer;
    }

    /**
     * This returns the {@link CropGrowthAccelerator} that is about to accelerate.
     *
     * @return The {@link CropGrowthAccelerator}
     */
    @Nonnull
    public CropGrowthAccelerator getAccelerator() {
        return accelerator;
    }

    /**
     * This returns the {@link Block} of the {@link CropGrowthAccelerator}.
     *
     * @return The accelerator {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the crop {@link Block} that is about to be accelerated.
     *
     * @return The crop {@link Block}
     */
    @Nonnull
    public Block getCrop() {
        return crop;
    }

    /**
     * This returns the fertilizer {@link ItemStack} that is about to be consumed.
     *
     * @return The fertilizer {@link ItemStack}
     */
    @Nonnull
    public ItemStack getFertilizer() {
        return fertilizer;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.TreeGrowthAccelerator;

/**
 * This {@link Event} is fired whenever a {@link TreeGrowthAccelerator} has found a
 * sapling within its radius and is about to boost its growth: the energy and the
 * fertilizer are about to be consumed and bonemeal is about to be applied to the
 * sapling.
 * <p>
 * Cancelling this event skips this sapling: no energy or fertilizer is consumed, the
 * sapling is left alone and the accelerator continues scanning the other saplings
 * within its radius during this tick.
 * <p>
 * Addons may also adjust the growth boost via {@link #setGrowthBoost(int)}, e.g. to
 * scale the acceleration with an upgrade module. On 1.17+ this is the number of
 * bonemeal applications simulated; on legacy versions it is the number of growth
 * stages added (clamped to the sapling's maximum stage).
 *
 * @author Zurker
 *
 * @see TreeGrowthAccelerator
 * @see CropAccelerateEvent
 */
public class TreeAccelerateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final TreeGrowthAccelerator accelerator;
    private final Block block;
    private final Block sapling;
    private final ItemStack fertilizer;

    private int growthBoost = 1;
    private boolean cancelled;

    /**
     * The maximum growth boost an addon may apply to a single sapling.
     * The consumer applies bonemeal in a loop of this size on the tick thread,
     * so an unbounded value would let a faulty listener stall the server.
     * 100 bonemeal applications grow any tree many times over (the regular
     * boost is {@code 1}), so this ceiling does not constrain legitimate use.
     */
    public static final int MAX_GROWTH_BOOST = 100;

    public TreeAccelerateEvent(@Nonnull TreeGrowthAccelerator accelerator, @Nonnull Block block, @Nonnull Block sapling, @Nonnull ItemStack fertilizer) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(accelerator, "The TreeGrowthAccelerator must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(sapling, "The sapling Block must not be null");
        Validate.notNull(fertilizer, "The fertilizer must not be null");

        this.accelerator = accelerator;
        this.block = block;
        this.sapling = sapling;
        this.fertilizer = fertilizer;
    }

    /**
     * This returns the {@link TreeGrowthAccelerator} that is about to accelerate.
     *
     * @return The {@link TreeGrowthAccelerator}
     */
    @Nonnull
    public TreeGrowthAccelerator getAccelerator() {
        return accelerator;
    }

    /**
     * This returns the {@link Block} of the {@link TreeGrowthAccelerator}.
     *
     * @return The accelerator {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the sapling {@link Block} that is about to be boosted.
     *
     * @return The sapling {@link Block}
     */
    @Nonnull
    public Block getSapling() {
        return sapling;
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

    /**
     * This returns the growth boost applied to the sapling. It defaults to {@code 1},
     * the {@link TreeGrowthAccelerator}'s regular boost.
     *
     * @return The growth boost
     * @see #setGrowthBoost(int)
     */
    public int getGrowthBoost() {
        return growthBoost;
    }

    /**
     * This sets the growth boost applied to the sapling. A boost of {@code 0} leaves
     * the sapling untouched (the energy and fertilizer are still consumed).
     *
     * @param growthBoost
     *            The growth boost, between {@code 0} and {@link #MAX_GROWTH_BOOST}
     */
    public void setGrowthBoost(int growthBoost) {
        Validate.isTrue(growthBoost >= 0, "The growth boost must not be negative");
        Validate.isTrue(growthBoost <= MAX_GROWTH_BOOST, "The growth boost must not exceed " + MAX_GROWTH_BOOST);

        this.growthBoost = growthBoost;
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

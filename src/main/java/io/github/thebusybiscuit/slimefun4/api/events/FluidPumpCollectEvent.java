package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.FluidPump;

/**
 * This {@link Event} is fired whenever a {@link FluidPump} is about to drain a fluid
 * {@link Block} from the world: the energy has been paid, an empty container consumed
 * and the filled container pushed out, right before any of that happens.
 * <p>
 * Cancelling this event skips this pumping operation entirely: the fluid stays in the
 * world, the energy is kept, the empty container is not consumed and nothing is
 * produced. The pump retries on its next tick.
 * <p>
 * Addons may also replace the produced container via {@link #setFilledContainer(ItemStack)},
 * e.g. to turn pumped water into a custom fluid container. The replacement is pushed to
 * the output slots as-is, without being re-checked against the space that was verified
 * for the original container; the consumed empty container is still the one matched in
 * the input slot.
 *
 * @author Zurker
 *
 * @see FluidPump
 */
public class FluidPumpCollectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final FluidPump pump;
    private final Block block;
    private final Block fluid;

    private ItemStack filledContainer;
    private boolean cancelled;

    public FluidPumpCollectEvent(@Nonnull FluidPump pump, @Nonnull Block block, @Nonnull Block fluid, @Nonnull ItemStack filledContainer) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(pump, "The FluidPump must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(fluid, "The fluid Block must not be null");
        Validate.notNull(filledContainer, "The filled container must not be null");

        this.pump = pump;
        this.block = block;
        this.fluid = fluid;
        this.filledContainer = filledContainer;
    }

    /**
     * This returns the {@link FluidPump} that is pumping.
     *
     * @return The {@link FluidPump}
     */
    @Nonnull
    public FluidPump getPump() {
        return pump;
    }

    /**
     * This returns the {@link Block} of the {@link FluidPump}.
     *
     * @return The pump {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the fluid {@link Block} that is about to be drained from the world.
     * For water sources this is the block below the pump itself; for lava it can be any
     * source block of the connected vein.
     *
     * @return The fluid {@link Block} about to be drained
     */
    @Nonnull
    public Block getFluid() {
        return fluid;
    }

    /**
     * This returns the filled container the {@link FluidPump} is about to produce,
     * e.g. a water bucket or a water bottle.
     *
     * @return The filled container
     */
    @Nonnull
    public ItemStack getFilledContainer() {
        return filledContainer;
    }

    /**
     * This replaces the filled container the {@link FluidPump} will produce.
     * The replacement is pushed to the output slots as-is, without being re-checked
     * against the space that was verified for the original container; the consumed
     * empty container is still the one matched in the input slot.
     *
     * @param filledContainer
     *            The new filled container, must not be null
     */
    public void setFilledContainer(@Nonnull ItemStack filledContainer) {
        Validate.notNull(filledContainer, "The filled container must not be null");

        this.filledContainer = filledContainer;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.AnimalProduce;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.ProduceCollector;

/**
 * This {@link Event} is fired whenever a {@link ProduceCollector} has matched an
 * {@link AnimalProduce}: a valid input item sits in its input slot, the output fits and
 * a matching animal was found nearby. The input item is about to be consumed and the
 * produce's operation is about to start.
 * <p>
 * Cancelling this event vetoes this {@link AnimalProduce} only: the input item is not
 * consumed and the {@link ProduceCollector} keeps scanning its remaining produces and
 * input slots for another match.
 *
 * @author Zurker
 *
 * @see ProduceCollector
 * @see AnimalProduce
 * @see AutoBreedEvent
 */
public class ProduceCollectorCollectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ProduceCollector collector;
    private final Block block;
    private final AnimalProduce produce;
    private ItemStack result;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public ProduceCollectorCollectEvent(ProduceCollector collector, Block block, AnimalProduce produce) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(collector, "The ProduceCollector must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(produce, "The AnimalProduce must not be null");

        this.collector = collector;
        this.block = block;
        this.produce = produce;
        this.result = produce.getOutput()[0];
    }

    /**
     * This returns the {@link ProduceCollector} that is collecting.
     *
     * @return The {@link ProduceCollector}
     */
    @Nonnull
    public ProduceCollector getCollector() {
        return collector;
    }

    /**
     * This returns the {@link Block} of the {@link ProduceCollector}.
     *
     * @return The collector {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link AnimalProduce} that was matched.
     *
     * @return The matched {@link AnimalProduce}
     */
    @Nonnull
    public AnimalProduce getProduce() {
        return produce;
    }

    /**
     * This returns the {@link ItemStack} that will be produced, initialized from
     * {@link AnimalProduce#getOutput()} but overridable via {@link #setResult(ItemStack)}.
     *
     * @return The result {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This sets the {@link ItemStack} that will be produced, overriding the produce's
     * default output.
     *
     * @param result
     *            The replacement result, must not be null
     */
    public void setResult(@Nonnull ItemStack result) {
        Validate.notNull(result, "The result must not be null");
        this.result = result;
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

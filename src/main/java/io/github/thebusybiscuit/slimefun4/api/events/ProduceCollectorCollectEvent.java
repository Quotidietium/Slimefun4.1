package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public ProduceCollectorCollectEvent(ProduceCollector collector, Block block, AnimalProduce produce) {
        Validate.notNull(collector, "The ProduceCollector must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(produce, "The AnimalProduce must not be null");

        this.collector = collector;
        this.block = block;
        this.produce = produce;
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

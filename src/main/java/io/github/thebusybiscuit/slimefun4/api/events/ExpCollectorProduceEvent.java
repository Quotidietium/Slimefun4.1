package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.ExpCollector;

/**
 * This {@link Event} is fired whenever an {@link ExpCollector} is about to produce a
 * Flask of Knowledge from its stored experience, right before the flask is pushed to the
 * output slots.
 * <p>
 * Cancelling this event prevents that single flask from being produced — the stored
 * experience is not consumed for the cancelled flask, so it remains available for the
 * next production attempt. The flask item can be replaced via {@link #setResult(ItemStack)}.
 * <p>
 * The event fires once per flask (every 10 stored XP points), so a single tick may fire
 * it multiple times if enough XP has been accumulated.
 *
 * @author Zurker
 *
 * @see ExpCollectorCollectEvent
 * @see ExpCollector
 */
public class ExpCollectorProduceEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ExpCollector collector;
    private final Block block;
    private final int experienceCost;
    private ItemStack result;

    private boolean cancelled;

    public ExpCollectorProduceEvent(@Nonnull ExpCollector collector, @Nonnull Block block, int experienceCost, @Nonnull ItemStack result) {

        // The ExpCollector ticks synchronously (isSynchronized = true), so this fires on the main
        // thread. The adaptive declaration reports the actual context regardless.
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(collector, "The ExpCollector must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.isTrue(experienceCost > 0, "The experience cost must be positive");
        Validate.notNull(result, "The result must not be null");

        this.collector = collector;
        this.block = block;
        this.experienceCost = experienceCost;
        this.result = result;
    }

    /**
     * This returns the {@link ExpCollector} that is producing the flask.
     *
     * @return The {@link ExpCollector}
     */
    @Nonnull
    public ExpCollector getCollector() {
        return collector;
    }

    /**
     * This returns the {@link Block} of the {@link ExpCollector}.
     *
     * @return The {@link ExpCollector} {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the amount of stored experience that will be consumed for this flask.
     * Currently always 10 XP per flask.
     *
     * @return The experience cost in points
     */
    public int getExperienceCost() {
        return experienceCost;
    }

    /**
     * This returns the flask item that will be produced.
     *
     * @return The result {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This sets the item that will be produced.
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

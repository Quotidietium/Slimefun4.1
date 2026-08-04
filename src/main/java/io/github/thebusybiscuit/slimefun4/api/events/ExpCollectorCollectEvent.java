package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.ExpCollector;

/**
 * This {@link Event} is fired whenever an {@link ExpCollector} is about to collect an
 * {@link ExperienceOrb} during its tick, after the energy check but before the orb is
 * removed and the experience is stored.
 * <p>
 * Cancelling this event skips this {@link ExperienceOrb}: it is not removed and no
 * experience is stored. The {@link ExpCollector} continues to scan for other nearby
 * {@link ExperienceOrb ExperienceOrbs} and will try this one again on its next tick.
 * <p>
 * Since an {@link ExpCollector} placed at a farm can collect an orb every tick, this
 * event is only allocated and fired when at least one listener is registered.
 *
 * @author Zurker
 *
 * @see ExpCollector
 */
public class ExpCollectorCollectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ExpCollector collector;
    private final Block block;
    private final ExperienceOrb orb;

    private boolean cancelled;

    public ExpCollectorCollectEvent(@Nonnull ExpCollector collector, @Nonnull Block block, @Nonnull ExperienceOrb orb) {
        Validate.notNull(collector, "The ExpCollector must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(orb, "The ExperienceOrb must not be null");

        this.collector = collector;
        this.block = block;
        this.orb = orb;
    }

    /**
     * This returns the {@link ExpCollector} that is about to collect the orb.
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
     * @return The collector {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link ExperienceOrb} that is about to be collected.
     *
     * @return The {@link ExperienceOrb}
     */
    @Nonnull
    public ExperienceOrb getOrb() {
        return orb;
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

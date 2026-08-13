package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
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
 * The amount of experience stored from this orb can be adjusted via
 * {@link #setExperience(int)} - e.g. to scale the yield of certain farms. Setting it
 * to 0 consumes the orb without storing anything. The orb itself is never modified.
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

    private int experience;
    private boolean cancelled;

    public ExpCollectorCollectEvent(@Nonnull ExpCollector collector, @Nonnull Block block, @Nonnull ExperienceOrb orb) {

        // The ExpCollector ticks synchronously (isSynchronized = true), so this fires on the main
        // thread. The adaptive declaration reports the actual context regardless.
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(collector, "The ExpCollector must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(orb, "The ExperienceOrb must not be null");

        this.collector = collector;
        this.block = block;
        this.orb = orb;
        this.experience = orb.getExperience();
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

    /**
     * This returns the amount of experience that will be stored from this orb.
     * It defaults to the orb's own experience value.
     *
     * @return The experience to be stored
     */
    public int getExperience() {
        return experience;
    }

    /**
     * This sets the amount of experience that will be stored from this orb. The orb
     * is consumed regardless; setting this to 0 stores nothing. The orb's own
     * experience value is never modified.
     *
     * @param experience
     *            The experience to be stored, must not be negative
     */
    public void setExperience(int experience) {
        Validate.isTrue(experience >= 0, "The experience must not be negative, received: " + experience);

        this.experience = experience;
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

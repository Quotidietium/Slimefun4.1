package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This {@link Event} is fired at the end of each Slimefun ticker cycle, after all
 * asynchronous block ticks have been dispatched and the profiler has recorded the
 * tick's elapsed time.
 * <p>
 * The event is informational only (not cancellable): the tick has already completed.
 * It fires on the ticker thread (asynchronous in production), using adaptive
 * asynchronicity so listeners can distinguish main-thread ticks (e.g. when driven
 * synchronously in tests) from production async ticks. Add-ons can use it for
 * monitoring, logging, or triggering periodic actions aligned with the Slimefun tick.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask
 * @see EnergyNetTickEvent
 * @see CargoNetTickEvent
 */
public class SlimefunTickEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final long tickDurationNanos;

    public SlimefunTickEvent(long tickDurationNanos) {
        super(!Bukkit.isPrimaryThread());
        Validate.isTrue(tickDurationNanos >= 0, "The tick duration must not be negative");

        this.tickDurationNanos = tickDurationNanos;
    }

    /**
     * This returns the total elapsed time of this Slimefun tick cycle, in nanoseconds.
     *
     * @return The tick duration in nanoseconds
     */
    public long getTickDurationNanos() {
        return tickDurationNanos;
    }

    /**
     * This returns the total elapsed time of this Slimefun tick cycle, in milliseconds.
     *
     * @return The tick duration in milliseconds
     */
    public double getTickDurationMillis() {
        return tickDurationNanos / 1_000_000.0;
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

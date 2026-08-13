package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;

/**
 * This {@link Event} is fired whenever a {@link Reactor} is about to burn a fuel item
 * and start a new fuel operation.
 * <p>
 * Cancelling this event vetoes the burn: the fuel stays in the reactor, no operation
 * is started and the reactor idles for this tick (it will try again on the next one).
 * <p>
 * The duration of the fuel operation that is about to start can be adjusted via
 * {@link #setTicks(int)}. The adjustment applies only to this single operation: the
 * shared {@link MachineFuel} definition is never modified.
 *
 * @author Zurker
 *
 * @see Reactor
 * @see ReactorCoolantConsumeEvent
 */
public class ReactorFuelBurnEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Reactor reactor;
    private final Location location;
    private final MachineFuel fuel;
    private final int slot;

    private int ticks;
    private boolean cancelled;

    public ReactorFuelBurnEvent(@Nonnull Reactor reactor, @Nonnull Location location, @Nonnull MachineFuel fuel, int slot) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(reactor, "The Reactor must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(fuel, "The fuel must not be null");
        Validate.isTrue(slot >= 0, "The slot must not be negative");

        this.reactor = reactor;
        this.location = location;
        this.fuel = fuel;
        this.slot = slot;
        this.ticks = fuel.getTicks();
    }

    /**
     * This returns the {@link Reactor} that is about to burn the fuel.
     *
     * @return The {@link Reactor}
     */
    @Nonnull
    public Reactor getReactor() {
        return reactor;
    }

    /**
     * This returns the {@link Location} of the {@link Reactor}.
     *
     * @return The {@link Location} of the {@link Reactor}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the {@link MachineFuel} that is about to be burned.
     *
     * @return The {@link MachineFuel}
     */
    @Nonnull
    public MachineFuel getFuel() {
        return fuel;
    }

    /**
     * This returns the inventory slot the fuel is about to be consumed from.
     *
     * @return The fuel slot
     */
    public int getSlot() {
        return slot;
    }

    /**
     * This returns the duration (in ticks) of the fuel operation that is about to
     * start. It defaults to the {@link MachineFuel}'s own duration.
     *
     * @return The duration of the fuel operation in ticks
     */
    public int getTicks() {
        return ticks;
    }

    /**
     * This sets the duration (in ticks) of the fuel operation that is about to
     * start. Only this single operation is affected: the reactor's shared
     * {@link MachineFuel} definition keeps its original duration.
     *
     * @param ticks
     *            The duration of the fuel operation in ticks, must be at least 1
     */
    public void setTicks(int ticks) {
        Validate.isTrue(ticks >= 1, "The fuel operation duration must be at least 1 tick, received: " + ticks);

        this.ticks = ticks;
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

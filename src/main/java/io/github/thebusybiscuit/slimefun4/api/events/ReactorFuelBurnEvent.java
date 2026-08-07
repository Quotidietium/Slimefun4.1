package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
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

    private boolean cancelled;

    public ReactorFuelBurnEvent(@Nonnull Reactor reactor, @Nonnull Location location, @Nonnull MachineFuel fuel, int slot) {
        Validate.notNull(reactor, "The Reactor must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(fuel, "The fuel must not be null");
        Validate.isTrue(slot >= 0, "The slot must not be negative");

        this.reactor = reactor;
        this.location = location;
        this.fuel = fuel;
        this.slot = slot;
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

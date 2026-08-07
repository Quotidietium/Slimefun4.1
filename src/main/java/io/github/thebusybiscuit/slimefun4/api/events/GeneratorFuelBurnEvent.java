package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.AbstractEnergyProvider;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;

/**
 * This {@link Event} is fired whenever an {@link AGenerator} (such as a coal or
 * combustion generator) is about to burn a fuel item and start a new fuel operation.
 * <p>
 * Cancelling this event vetoes the burn: the fuel stays in the generator, no operation
 * is started and the generator idles for this tick (it will try again on the next one).
 *
 * @author Zurker
 *
 * @see AbstractEnergyProvider
 * @see ReactorFuelBurnEvent
 * @see GeneratorProduceByproductEvent
 */
public class GeneratorFuelBurnEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AGenerator generator;
    private final Location location;
    private final MachineFuel fuel;
    private final int slot;

    private boolean cancelled;

    public GeneratorFuelBurnEvent(@Nonnull AGenerator generator, @Nonnull Location location, @Nonnull MachineFuel fuel, int slot) {
        Validate.notNull(generator, "The AGenerator must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(fuel, "The fuel must not be null");
        Validate.isTrue(slot >= 0, "The slot must not be negative");

        this.generator = generator;
        this.location = location;
        this.fuel = fuel;
        this.slot = slot;
    }

    /**
     * This returns the {@link AGenerator} that is about to burn the fuel.
     *
     * @return The {@link AGenerator}
     */
    @Nonnull
    public AGenerator getGenerator() {
        return generator;
    }

    /**
     * This returns the {@link Location} of the {@link AGenerator}.
     *
     * @return The {@link Location} of the {@link AGenerator}
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
     * This returns the input slot the fuel item was found in.
     *
     * @return The input slot of the fuel
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

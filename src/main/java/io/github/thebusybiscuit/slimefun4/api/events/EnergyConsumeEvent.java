package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;

/**
 * This {@link Event} is fired whenever an {@link EnergyNet} settlement is about to push
 * energy into a consumer (a machine or any other {@link EnergyNetComponent} with free
 * capacity) during the network tick.
 *
 * <p>
 * It allows add-ons to <b>modify or suppress</b> the per-consumer transfer on a given
 * tick - e.g. throttling a machine's intake, implementing consumption taxes or reserving
 * power for other consumers:
 * </p>
 * <ul>
 * <li>{@link #setEnergy(int)} scales the transferred energy, between {@code 0} and
 * {@link #getMaxTransfer()}.</li>
 * <li>{@link #setCancelled(boolean)} cancels the transfer - the consumer receives no
 * energy this tick and the energy stays in the network pool for the remaining
 * consumers.</li>
 * </ul>
 *
 * <p>
 * The event is only fired when at least one listener is registered
 * ({@link #getHandlerList()}.{@link HandlerList#getRegisteredListeners() getRegisteredListeners()}),
 * so it is effectively zero-cost when unused.
 * </p>
 *
 * <p>
 * It is raised on the <b>async ticker thread</b>; listeners must not call sync-only Bukkit API.
 * </p>
 *
 * @author Zurker
 *
 * @see EnergyNetComponent
 * @see EnergyGenerateEvent
 * @see EnergyNetTickEvent
 *
 */
public class EnergyConsumeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EnergyNet network;
    private final EnergyNetComponent component;
    private final Location location;
    private final int maxTransfer;
    private int energy;
    private boolean cancelled;

    public EnergyConsumeEvent(@Nonnull EnergyNet network, @Nonnull EnergyNetComponent component, @Nonnull Location location, int energy, int maxTransfer) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(network, "The EnergyNet cannot be null");
        Validate.notNull(component, "The EnergyNetComponent cannot be null");
        Validate.notNull(location, "The Location cannot be null");
        Validate.isTrue(energy >= 0, "The energy must not be negative");
        Validate.isTrue(maxTransfer >= energy, "The maximum transfer must not be smaller than the energy");
        Validate.isTrue(maxTransfer <= component.getCapacity(), "The maximum transfer must not exceed the capacity");

        this.network = network;
        this.component = component;
        this.location = location;
        this.energy = energy;
        this.maxTransfer = maxTransfer;
    }

    /**
     * The {@link EnergyNet} that is settling.
     *
     * @return The settling {@link EnergyNet}
     */
    @Nonnull
    public EnergyNet getNetwork() {
        return network;
    }

    /**
     * The consumer {@link EnergyNetComponent} about to receive energy.
     *
     * @return The consuming {@link EnergyNetComponent}
     */
    @Nonnull
    public EnergyNetComponent getComponent() {
        return component;
    }

    /**
     * The {@link Location} of the consumer.
     *
     * @return The consumer's {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * The amount of energy (in J) about to be transferred into the consumer this tick.
     *
     * @return The energy amount to transfer
     */
    public int getEnergy() {
        return energy;
    }

    /**
     * Sets the amount of energy transferred into the consumer this tick. Any energy not
     * transferred stays in the network pool for the remaining consumers.
     *
     * @param energy
     *            The new energy amount, between {@code 0} and {@link #getMaxTransfer()}
     */
    public void setEnergy(int energy) {
        Validate.isTrue(energy >= 0, "The energy must not be negative");
        Validate.isTrue(energy <= maxTransfer, "The energy must not exceed the maximum transfer of " + maxTransfer);

        this.energy = energy;
    }

    /**
     * The maximum amount of energy (in J) that could be transferred into this consumer
     * this tick: the smaller of the network's remaining pooled energy and the consumer's
     * free capacity.
     *
     * @return The maximum transferable energy
     */
    public int getMaxTransfer() {
        return maxTransfer;
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

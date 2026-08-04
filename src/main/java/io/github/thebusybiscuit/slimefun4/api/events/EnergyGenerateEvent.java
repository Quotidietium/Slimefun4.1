package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;

/**
 * This {@link Event} is fired whenever an {@link EnergyNetProvider} (generator) produces energy
 * during an {@link io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet} tick.
 *
 * <p>
 * It allows add-ons to <b>modify or suppress</b> how much energy a generator contributes to the
 * network on a given tick - e.g. boosting solar panels under clear skies or curtailing a generator
 * that violates a custom rule:
 * </p>
 * <ul>
 * <li>{@link #setEnergy(int)} scales the contributed energy (must be {@code >= 0}).</li>
 * <li>{@link #setCancelled(boolean)} cancels the contribution - the generator feeds {@code 0} J into
 * the network this tick (its stored charge is unaffected).</li>
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
 * @see EnergyNetProvider
 * @see EnergyNetTickEvent
 *
 */
public class EnergyGenerateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EnergyNetProvider provider;
    private final Location location;
    private int energy;
    private boolean cancelled;

    public EnergyGenerateEvent(@Nonnull EnergyNetProvider provider, @Nonnull Location location, int energy) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(provider, "The EnergyNetProvider cannot be null");
        Validate.notNull(location, "The Location cannot be null");

        this.provider = provider;
        this.location = location;
        this.energy = energy;
    }

    /**
     * The {@link EnergyNetProvider} that generated the energy.
     *
     * @return The generating {@link EnergyNetProvider}
     */
    @Nonnull
    public EnergyNetProvider getProvider() {
        return provider;
    }

    /**
     * The {@link Location} of the generator.
     *
     * @return The generator's {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * The amount of energy (in J) this generator will contribute to the network this tick.
     *
     * @return The contributed energy amount
     */
    public int getEnergy() {
        return energy;
    }

    /**
     * Sets the amount of energy this generator will contribute.
     *
     * @param energy
     *            The new energy amount (must be {@code >= 0})
     */
    public void setEnergy(int energy) {
        Validate.isTrue(energy >= 0, "Energy must be zero or greater!");
        this.energy = energy;
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

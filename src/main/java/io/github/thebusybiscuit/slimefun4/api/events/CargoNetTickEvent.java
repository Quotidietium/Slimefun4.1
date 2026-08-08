package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;

/**
 * This {@link Event} is fired at the start of each {@link CargoNet} routing tick, after
 * the regulator has been validated. It is the cargo-network counterpart to
 * {@link EnergyNetTickEvent}.
 * <p>
 * The event is informational only (not cancellable): the tick proceeds regardless. It
 * fires synchronously from the {@link io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask},
 * so addons can use it for monitoring, logging or synchronized side-effects.
 *
 * @author Zurker
 *
 * @see EnergyNetTickEvent
 * @see NetworkCreateEvent
 * @see CargoNet
 */
public class CargoNetTickEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final CargoNet network;
    private final Block regulator;

    public CargoNetTickEvent(@Nonnull CargoNet network, @Nonnull Block regulator) {
        Validate.notNull(network, "The CargoNet must not be null");
        Validate.notNull(regulator, "The regulator Block must not be null");

        this.network = network;
        this.regulator = regulator;
    }

    /**
     * This returns the {@link CargoNet} that is ticking.
     *
     * @return The {@link CargoNet}
     */
    @Nonnull
    public CargoNet getNetwork() {
        return network;
    }

    /**
     * This returns the regulator {@link Block} (the Cargo Manager) that triggered this tick.
     *
     * @return The regulator {@link Block}
     */
    @Nonnull
    public Block getRegulator() {
        return regulator;
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

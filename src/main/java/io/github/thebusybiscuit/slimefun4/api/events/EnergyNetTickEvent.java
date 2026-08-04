package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;

/**
 * This {@link Event} is fired after an {@link EnergyNet} has settled its energy distribution for
 * a tick, carrying the total energy <b>supply</b> (what was available) and <b>demand</b> (what
 * consumers could absorb).
 *
 * <p>
 * It is an observational, <b>non-cancellable</b> hook intended for telemetry, HUDs, surplus/deficit
 * detection and the like. Use {@link EnergyGenerateEvent} if you need to modify generation.
 * </p>
 *
 * <p>
 * The event is only fired when at least one listener is registered, so it is effectively
 * zero-cost when unused. It is raised on the <b>async ticker thread</b>.
 * </p>
 *
 * @author Zurker
 *
 * @see EnergyNet
 * @see EnergyGenerateEvent
 *
 */
public class EnergyNetTickEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final EnergyNet network;
    private final Block regulator;
    private final int supply;
    private final int demand;

    public EnergyNetTickEvent(@Nonnull EnergyNet network, @Nonnull Block regulator, int supply, int demand) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(network, "The EnergyNet cannot be null");
        Validate.notNull(regulator, "The regulator Block cannot be null");

        this.network = network;
        this.regulator = regulator;
        this.supply = supply;
        this.demand = demand;
    }

    /**
     * The {@link EnergyNet} that was ticked.
     *
     * @return The {@link EnergyNet}
     */
    @Nonnull
    public EnergyNet getNetwork() {
        return network;
    }

    /**
     * The regulator {@link Block} of the network.
     *
     * @return The regulator {@link Block}
     */
    @Nonnull
    public Block getRegulator() {
        return regulator;
    }

    /**
     * The total energy (in J) that was available this tick (generators + capacitors).
     *
     * @return The energy supply
     */
    public int getSupply() {
        return supply;
    }

    /**
     * The total energy (in J) that consumers were able to absorb this tick.
     *
     * @return The energy demand
     */
    public int getDemand() {
        return demand;
    }

    /**
     * The net energy flow this tick: {@code supply - demand}.
     *
     * @return A positive value for a surplus, a negative value for a deficit, {@code 0} if balanced
     */
    public int getNetEnergy() {
        return supply - demand;
    }

    /**
     * Whether the network produced more energy than consumers could absorb (a surplus).
     *
     * @return {@code true} if there is leftover energy
     */
    public boolean isSurplus() {
        return supply > demand;
    }

    /**
     * Whether consumers wanted more energy than the network could provide (a deficit / brownout).
     *
     * @return {@code true} if demand exceeds supply
     */
    public boolean isDeficit() {
        return demand > supply;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.network.Network;

/**
 * This {@link Event} is fired whenever a {@link Network} is registered with the
 * {@link io.github.thebusybiscuit.slimefun4.core.networks.NetworkManager}, i.e. when a
 * new network has just been created and indexed.
 * <p>
 * The event is informational only: it is not cancellable, since the network has already
 * been added to the manager and cancelling would break the contract of the
 * {@code getNetworkFromLocationOrCreate} factory methods that return the just-registered
 * network. Add-ons that want to react to a network coming into existence can listen here.
 * <p>
 * The event is fired synchronously from the thread that registered the network.
 *
 * @author Zurker
 *
 * @see NetworkDisposeEvent
 * @see Network
 */
public class NetworkCreateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Network network;

    public NetworkCreateEvent(@Nonnull Network network) {
        Validate.notNull(network, "The Network must not be null");

        this.network = network;
    }

    /**
     * This returns the {@link Network} that was just created.
     *
     * @return The {@link Network}
     */
    @Nonnull
    public Network getNetwork() {
        return network;
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

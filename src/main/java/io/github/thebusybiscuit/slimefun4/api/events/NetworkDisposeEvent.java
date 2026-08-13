package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.network.Network;

/**
 * This {@link Event} is fired whenever a {@link Network} is removed from the
 * {@link io.github.thebusybiscuit.slimefun4.core.networks.NetworkManager}, i.e. when a
 * network has been disposed of (its regulator or all of its components were destroyed).
 * <p>
 * The event is informational only: it is not cancellable, since the network has already
 * been removed from the manager. Add-ons that want to react to a network going away can
 * listen here.
 * <p>
 * The event is fired synchronously from the thread that unregistered the network.
 * <p>
 * Note that networks may be unregistered from the asynchronous ticker thread
 * (e.g. during an asynchronous network rebuild), in which case this event fires
 * asynchronously.
 *
 * @author Zurker
 *
 * @see NetworkCreateEvent
 * @see Network
 */
public class NetworkDisposeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Network network;

    public NetworkDisposeEvent(@Nonnull Network network) {
        /* Networks can be unregistered from the async ticker thread. */
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(network, "The Network must not be null");

        this.network = network;
    }

    /**
     * This returns the {@link Network} that was just disposed of.
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

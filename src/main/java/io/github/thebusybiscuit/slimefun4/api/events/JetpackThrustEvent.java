package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.Jetpack;

/**
 * This {@link PlayerEvent} is fired every time a {@link Jetpack}-wearing
 * {@link Player} thrusts, right before the charge cost is deducted and the
 * velocity is applied.
 * <p>
 * Cancelling this event skips this thrust: no charge is consumed and no
 * velocity is applied. The jetpack task keeps running, so the event fires
 * again on the next thrust tick.
 *
 * @author Zurker
 *
 * @see Jetpack
 */
public class JetpackThrustEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Jetpack jetpack;
    private final float cost;

    private boolean cancelled;

    public JetpackThrustEvent(@Nonnull Player player, @Nonnull Jetpack jetpack, float cost) {
        super(player);

        Validate.notNull(jetpack, "The jetpack must not be null");
        this.jetpack = jetpack;
        this.cost = cost;
    }

    /**
     * This returns the {@link Jetpack} the {@link Player} is wearing.
     *
     * @return The {@link Jetpack}
     */
    @Nonnull
    public Jetpack getJetpack() {
        return jetpack;
    }

    /**
     * This returns the amount of charge this thrust will consume.
     *
     * @return The charge cost of this thrust
     */
    public float getCost() {
        return cost;
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

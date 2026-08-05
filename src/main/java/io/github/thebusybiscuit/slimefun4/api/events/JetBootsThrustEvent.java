package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.JetBoots;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} wearing charged {@link JetBoots}
 * is sneaking and the boots are about to thrust: a small amount of charge is about to be
 * consumed and a forward velocity is about to be applied.
 * <p>
 * Cancelling this event skips this thrust: no charge is consumed and no velocity is applied,
 * but the boots keep running.
 *
 * @author Zurker
 *
 * @see JetBoots
 * @see JetpackThrustEvent
 */
public class JetBootsThrustEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final JetBoots jetBoots;
    private final float cost;

    private boolean cancelled;

    public JetBootsThrustEvent(@Nonnull Player player, @Nonnull JetBoots jetBoots, float cost) {
        super(player);
        Validate.notNull(jetBoots, "The JetBoots must not be null");

        this.jetBoots = jetBoots;
        this.cost = cost;
    }

    /**
     * This returns the {@link JetBoots} that are thrusting.
     *
     * @return The {@link JetBoots}
     */
    @Nonnull
    public JetBoots getJetBoots() {
        return jetBoots;
    }

    /**
     * This returns the amount of charge a single thrust consumes.
     *
     * @return The charge cost per thrust
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

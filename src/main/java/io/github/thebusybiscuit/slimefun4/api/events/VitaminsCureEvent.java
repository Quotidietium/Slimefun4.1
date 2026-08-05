package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.medical.Vitamins;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks {@link Vitamins}:
 * the vitamins are about to be consumed, any fire extinguished, negative potion effects and
 * radiation exposure cleared and the player healed.
 * <p>
 * Cancelling this event skips the use entirely: no vitamins are consumed and no curing happens.
 *
 * @author Zurker
 *
 * @see Vitamins
 */
public class VitaminsCureEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Vitamins vitamins;

    private boolean cancelled;

    public VitaminsCureEvent(@Nonnull Player player, @Nonnull Vitamins vitamins) {
        super(player);
        Validate.notNull(vitamins, "The Vitamins must not be null");

        this.vitamins = vitamins;
    }

    /**
     * This returns the {@link Vitamins} that are being consumed.
     *
     * @return The {@link Vitamins}
     */
    @Nonnull
    public Vitamins getVitamins() {
        return vitamins;
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

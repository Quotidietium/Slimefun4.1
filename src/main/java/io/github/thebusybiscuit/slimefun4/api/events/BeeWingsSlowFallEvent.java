package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.BeeWings;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} gliding with
 * {@link BeeWings} approaches the ground and the wings are about to slow the fall:
 * a message is sent, the fall distance reset and a slow-falling effect applied.
 * <p>
 * Cancelling this event skips this one application: no message is sent and no effect
 * is applied. The wings keep watching the descent and the event fires again the next
 * time the {@link Player} comes close to the ground.
 *
 * @author Zurker
 *
 * @see BeeWings
 */
public class BeeWingsSlowFallEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PotionEffect effect;

    private boolean cancelled;

    public BeeWingsSlowFallEvent(@Nonnull Player player, @Nonnull PotionEffect effect) {
        super(player);
        Validate.notNull(effect, "The PotionEffect must not be null");

        this.effect = effect;
    }

    /**
     * This returns the slow-falling {@link PotionEffect} that is about to be applied
     * to the {@link Player}.
     *
     * @return The {@link PotionEffect} about to be applied
     */
    @Nonnull
    public PotionEffect getEffect() {
        return effect;
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

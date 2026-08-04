package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.LongFallBoots;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.StomperBoots;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} wearing protective
 * Slimefun boots (e.g. {@link StomperBoots} or {@link LongFallBoots}) takes fall damage,
 * right before the boots kick in: the damage is about to be cancelled and the boots'
 * effect (a stomp shockwave or a sound) about to be applied.
 * <p>
 * Cancelling this event keeps the vanilla behavior: the {@link Player} takes the fall
 * damage normally and no boots effect is applied.
 *
 * @author Zurker
 *
 * @see StomperBoots
 * @see LongFallBoots
 */
public class SlimefunBootsFallEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem boots;
    private final EntityDamageEvent damageEvent;

    private boolean cancelled;

    public SlimefunBootsFallEvent(@Nonnull Player player, @Nonnull SlimefunItem boots, @Nonnull EntityDamageEvent damageEvent) {
        super(player);
        Validate.notNull(boots, "The boots must not be null");
        Validate.notNull(damageEvent, "The EntityDamageEvent must not be null");

        this.boots = boots;
        this.damageEvent = damageEvent;
    }

    /**
     * This returns the boots the {@link Player} is wearing, either a pair of
     * {@link StomperBoots} or {@link LongFallBoots}.
     *
     * @return The boots {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getBoots() {
        return boots;
    }

    /**
     * This returns the underlying {@link EntityDamageEvent}. It has not been cancelled
     * yet; its damage can still be adjusted directly before the stomp effect reads it.
     *
     * @return The underlying {@link EntityDamageEvent}
     */
    @Nonnull
    public EntityDamageEvent getDamageEvent() {
        return damageEvent;
    }

    /**
     * This is a convenience method that returns the fall damage the {@link Player} is
     * about to be protected from.
     *
     * @return The fall damage
     */
    public double getFallDamage() {
        return damageEvent.getDamage();
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

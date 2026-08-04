package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.ElytraCap;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;

/**
 * This {@link PlayerEvent} is fired whenever a gliding {@link Player} takes fall or
 * fly-into-wall damage while wearing a {@link SlimefunArmorPiece} helmet that grants
 * flying-into-wall protection (e.g. the {@link ElytraCap}), right before the protection
 * kicks in: the damage is about to be cancelled and the helmet about to take durability
 * damage.
 * <p>
 * Cancelling this event keeps the vanilla behavior: the {@link Player} takes the damage
 * normally and the helmet is left untouched.
 *
 * @author Zurker
 *
 * @see ElytraCap
 * @see SlimefunArmorPiece
 */
public class ElytraImpactEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunArmorPiece helmet;
    private final EntityDamageEvent damageEvent;

    private boolean cancelled;

    public ElytraImpactEvent(@Nonnull Player player, @Nonnull SlimefunArmorPiece helmet, @Nonnull EntityDamageEvent damageEvent) {
        super(player);
        Validate.notNull(helmet, "The helmet must not be null");
        Validate.notNull(damageEvent, "The EntityDamageEvent must not be null");

        this.helmet = helmet;
        this.damageEvent = damageEvent;
    }

    /**
     * This returns the {@link SlimefunArmorPiece} helmet the {@link Player} is wearing.
     *
     * @return The helmet {@link SlimefunArmorPiece}
     */
    @Nonnull
    public SlimefunArmorPiece getHelmet() {
        return helmet;
    }

    /**
     * This returns the underlying {@link EntityDamageEvent}. It has not been cancelled
     * yet; its damage can still be adjusted directly.
     *
     * @return The underlying {@link EntityDamageEvent}
     */
    @Nonnull
    public EntityDamageEvent getDamageEvent() {
        return damageEvent;
    }

    /**
     * This is a convenience method that returns the {@link DamageCause} of the impact,
     * either {@link DamageCause#FALL} or {@link DamageCause#FLY_INTO_WALL}.
     *
     * @return The {@link DamageCause} of the impact
     */
    @Nonnull
    public DamageCause getDamageCause() {
        return damageEvent.getCause();
    }

    /**
     * This is a convenience method that returns the damage the {@link Player} is about
     * to be protected from.
     *
     * @return The impact damage
     */
    public double getDamage() {
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

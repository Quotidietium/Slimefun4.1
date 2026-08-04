package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectiveArmor;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} wearing a full set of
 * {@link ProtectiveArmor} against {@link ProtectionType#BEES} (e.g. the Hazmat suit)
 * is stung by a {@link Bee} and the armor is about to absorb the sting: every armor
 * piece is about to take one point of durability damage and the sting's damage is
 * about to be reduced to zero.
 * <p>
 * Cancelling this event skips the protection entirely: the armor stays undamaged and
 * the {@link Player} takes the sting's damage normally.
 *
 * @author Zurker
 *
 * @see ProtectiveArmor
 * @see ProtectionType#BEES
 */
public class BeeStingProtectionEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Bee bee;
    private final EntityDamageByEntityEvent damageEvent;

    private boolean cancelled;

    public BeeStingProtectionEvent(@Nonnull Player player, @Nonnull Bee bee, @Nonnull EntityDamageByEntityEvent damageEvent) {
        super(player);
        Validate.notNull(bee, "The Bee must not be null");
        Validate.notNull(damageEvent, "The damage event must not be null");

        this.bee = bee;
        this.damageEvent = damageEvent;
    }

    /**
     * This returns the {@link Bee} that stung the {@link Player}.
     *
     * @return The {@link Bee}
     */
    @Nonnull
    public Bee getBee() {
        return bee;
    }

    /**
     * This returns the original {@link EntityDamageByEntityEvent} for this sting.
     *
     * @return The {@link EntityDamageByEntityEvent}
     */
    @Nonnull
    public EntityDamageByEntityEvent getDamageEvent() {
        return damageEvent;
    }

    /**
     * This returns the damage the sting would deal without protection,
     * a convenience method for {@code getDamageEvent().getDamage()}.
     *
     * @return The original damage
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

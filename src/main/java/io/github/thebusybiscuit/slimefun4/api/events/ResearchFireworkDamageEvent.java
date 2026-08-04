package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import io.github.thebusybiscuit.slimefun4.api.researches.Research;

/**
 * This {@link Event} is fired whenever the celebration {@link Firework} of a
 * {@link Research} unlock would deal damage to an {@link Entity} and that damage is
 * about to be nullified: research fireworks are meant to be harmless eye candy.
 * <p>
 * Cancelling this event skips the nullification: the firework deals its damage to the
 * victim like any other firework would.
 *
 * @author Zurker
 *
 * @see Research
 */
public class ResearchFireworkDamageEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Firework firework;
    private final Entity victim;
    private final EntityDamageByEntityEvent damageEvent;

    private boolean cancelled;

    public ResearchFireworkDamageEvent(@Nonnull Firework firework, @Nonnull Entity victim, @Nonnull EntityDamageByEntityEvent damageEvent) {
        Validate.notNull(firework, "The Firework must not be null");
        Validate.notNull(victim, "The victim must not be null");
        Validate.notNull(damageEvent, "The damage event must not be null");

        this.firework = firework;
        this.victim = victim;
        this.damageEvent = damageEvent;
    }

    /**
     * This returns the research {@link Firework} that would have dealt damage.
     *
     * @return The {@link Firework}
     */
    @Nonnull
    public Firework getFirework() {
        return firework;
    }

    /**
     * This returns the {@link Entity} that would have been damaged.
     *
     * @return The victim {@link Entity}
     */
    @Nonnull
    public Entity getVictim() {
        return victim;
    }

    /**
     * This returns the original {@link EntityDamageByEntityEvent} for this damage.
     *
     * @return The {@link EntityDamageByEntityEvent}
     */
    @Nonnull
    public EntityDamageByEntityEvent getDamageEvent() {
        return damageEvent;
    }

    /**
     * This returns the damage the firework would have dealt,
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

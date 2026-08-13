package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.ExplosiveBow;

/**
 * This {@link Event} is fired whenever an arrow shot from an {@link ExplosiveBow} hits a
 * {@link LivingEntity}: the fake explosion is about to push away and damage any nearby
 * {@link LivingEntity LivingEntities}.
 * <p>
 * Cancelling this event skips the explosion entirely: no particles, no sound, no knockback and
 * no area damage. Addons may also remove entries from {@link #getAffectedEntities()} to spare
 * specific entities; the hit target itself is never affected by the area damage. Anything that
 * is not a {@link LivingEntity} is ignored at detonation time.
 *
 * @author Zurker
 *
 * @see ExplosiveBow
 * @see SlimefunBowHitEvent
 */
public class ExplosiveBowExplodeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ExplosiveBow bow;
    private final LivingEntity target;
    private final Collection<Entity> affectedEntities;

    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public ExplosiveBowExplodeEvent(ExplosiveBow bow, LivingEntity target, Collection<Entity> affectedEntities) {
        super();
        Validate.notNull(bow, "The ExplosiveBow must not be null");
        Validate.notNull(target, "The target must not be null");
        Validate.notNull(affectedEntities, "The affected entities must not be null");

        this.bow = bow;
        this.target = target;
        this.affectedEntities = affectedEntities;
    }

    /**
     * This returns the {@link ExplosiveBow} the arrow was shot from.
     *
     * @return The {@link ExplosiveBow}
     */
    @Nonnull
    public ExplosiveBow getBow() {
        return bow;
    }

    /**
     * This returns the {@link LivingEntity} that was hit by the arrow.
     *
     * @return The hit {@link LivingEntity}
     */
    @Nonnull
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * This returns the mutable {@link Collection} of nearby {@link Entity Entities} that are
     * about to be pushed away and damaged. Removing entries spares those entities.
     *
     * @return The affected {@link Entity Entities}
     */
    @Nonnull
    public Collection<Entity> getAffectedEntities() {
        return affectedEntities;
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

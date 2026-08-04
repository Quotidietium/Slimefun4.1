package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDeathEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ButcherAndroid;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;

/**
 * This {@link Event} is fired whenever a {@link ButcherAndroid} has killed a
 * {@link LivingEntity} and is about to harvest the kill: nearby dropped items are
 * about to be collected into the android, any extra drops (e.g. a wither skeleton
 * skull) are about to be added and an experience orb is about to be spawned.
 * <p>
 * Cancelling this event skips the harvest entirely: the drops stay on the ground,
 * the android receives nothing and no experience orb is spawned.
 *
 * @author Zurker
 *
 * @see ButcherAndroid
 * @see ProgrammableAndroid
 */
public class ButcherAndroidKillEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ProgrammableAndroid android;
    private final Block block;
    private final LivingEntity entity;
    private final EntityDeathEvent deathEvent;

    private boolean cancelled;

    public ButcherAndroidKillEvent(@Nonnull ProgrammableAndroid android, @Nonnull Block block, @Nonnull LivingEntity entity, @Nonnull EntityDeathEvent deathEvent) {
        Validate.notNull(android, "The ProgrammableAndroid must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(entity, "The killed entity must not be null");
        Validate.notNull(deathEvent, "The death event must not be null");

        this.android = android;
        this.block = block;
        this.entity = entity;
        this.deathEvent = deathEvent;
    }

    /**
     * This returns the {@link ProgrammableAndroid} that killed the entity.
     *
     * @return The {@link ProgrammableAndroid}
     */
    @Nonnull
    public ProgrammableAndroid getAndroid() {
        return android;
    }

    /**
     * This returns the {@link Block} of the {@link ProgrammableAndroid}.
     *
     * @return The android {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link LivingEntity} that was killed.
     *
     * @return The killed {@link LivingEntity}
     */
    @Nonnull
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * This returns the original {@link EntityDeathEvent} for this kill.
     *
     * @return The {@link EntityDeathEvent}
     */
    @Nonnull
    public EntityDeathEvent getDeathEvent() {
        return deathEvent;
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

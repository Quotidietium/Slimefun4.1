package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.concurrent.ThreadLocalRandom;

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
 * <p>
 * Addons may also adjust the experience yield via {@link #setExperience(int)}, e.g. to
 * reward more experience for rare mobs; setting it to zero suppresses the experience
 * orb entirely.
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

    private int experience;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public ButcherAndroidKillEvent(ProgrammableAndroid android, Block block, LivingEntity entity, EntityDeathEvent deathEvent) {
        this(android, block, entity, deathEvent, 1 + ThreadLocalRandom.current().nextInt(6));
    }

    @ParametersAreNonnullByDefault
    public ButcherAndroidKillEvent(ProgrammableAndroid android, Block block, LivingEntity entity, EntityDeathEvent deathEvent, int experience) {
        Validate.notNull(android, "The ProgrammableAndroid must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(entity, "The killed entity must not be null");
        Validate.notNull(deathEvent, "The death event must not be null");
        Validate.isTrue(experience >= 0, "The experience must not be negative");

        this.android = android;
        this.block = block;
        this.entity = entity;
        this.deathEvent = deathEvent;
        this.experience = experience;
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

    /**
     * This returns the experience the spawned orb will carry. It defaults to the roll
     * the {@link ButcherAndroid} performs for every kill (between 1 and 6).
     *
     * @return The experience yield
     * @see #setExperience(int)
     */
    public int getExperience() {
        return experience;
    }

    /**
     * This sets the experience the spawned orb will carry. Setting it to zero
     * suppresses the experience orb entirely; the drops are still harvested.
     *
     * @param experience
     *            The experience yield, must not be negative
     */
    public void setExperience(int experience) {
        Validate.isTrue(experience >= 0, "The experience must not be negative");

        this.experience = experience;
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

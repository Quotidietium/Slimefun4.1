package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.IcyBow;

/**
 * This {@link Event} is fired whenever an arrow shot from an {@link IcyBow} hits a
 * {@link LivingEntity}: the target is about to be frozen (players only), showered in ice
 * particles and slowed down.
 * <p>
 * Cancelling this event skips all of the above. Addons may adjust the freeze ticks via
 * {@link #setFreezeTicks(int)} and remove or replace the {@link PotionEffect PotionEffects}
 * in {@link #getEffects()}. Note that a {@link Player} target who successfully blocked the
 * arrow still receives no effects, regardless of this event.
 *
 * @author Zurker
 *
 * @see IcyBow
 * @see SlimefunBowHitEvent
 */
public class IcyBowFreezeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final IcyBow bow;
    private final LivingEntity target;
    private final List<PotionEffect> effects;

    private int freezeTicks;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public IcyBowFreezeEvent(IcyBow bow, LivingEntity target, int freezeTicks, List<PotionEffect> effects) {
        super();
        Validate.notNull(bow, "The IcyBow must not be null");
        Validate.notNull(target, "The target must not be null");
        Validate.isTrue(freezeTicks >= 0, "The freeze ticks must not be negative");
        Validate.notNull(effects, "The effects must not be null");

        this.bow = bow;
        this.target = target;
        this.freezeTicks = freezeTicks;
        this.effects = effects;
    }

    /**
     * This returns the {@link IcyBow} the arrow was shot from.
     *
     * @return The {@link IcyBow}
     */
    @Nonnull
    public IcyBow getBow() {
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
     * This returns the freeze ticks a {@link Player} target will be set to.
     *
     * @return The freeze ticks
     */
    public int getFreezeTicks() {
        return freezeTicks;
    }

    /**
     * This sets the freeze ticks a {@link Player} target will be set to.
     *
     * @param freezeTicks
     *            The freeze ticks
     */
    public void setFreezeTicks(int freezeTicks) {
        Validate.isTrue(freezeTicks >= 0, "The freeze ticks must not be negative");

        this.freezeTicks = freezeTicks;
    }

    /**
     * This returns the mutable {@link List} of {@link PotionEffect PotionEffects} that are
     * about to be applied to the target. Removing or adding entries changes the applied
     * effects.
     *
     * @return The {@link PotionEffect PotionEffects} to apply
     */
    @Nonnull
    public List<PotionEffect> getEffects() {
        return effects;
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

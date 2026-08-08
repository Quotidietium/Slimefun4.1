package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.medical.Bandage;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link Bandage}
 * while injured or burning: the bandage is about to be consumed, the player healed and any
 * fire extinguished.
 * <p>
 * Cancelling this event skips the use entirely: no bandage is consumed, no healing happens
 * and the fire keeps burning.
 * <p>
 * The healing {@link PotionEffect} (INSTANT_HEALTH by default) can be replaced via
 * {@link #setEffect(PotionEffect)} before it is applied, allowing addons to boost,
 * reduce or replace the healing.
 *
 * @author Zurker
 *
 * @see Bandage
 * @see SplintHealEvent
 */
public class BandageHealEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Bandage bandage;
    private PotionEffect effect;

    private boolean cancelled;

    public BandageHealEvent(@Nonnull Player player, @Nonnull Bandage bandage, @Nonnull PotionEffect effect) {
        super(player);
        Validate.notNull(bandage, "The Bandage must not be null");
        Validate.notNull(effect, "The PotionEffect must not be null");

        this.bandage = bandage;
        this.effect = effect;
    }

    /**
     * This returns the {@link Bandage} that is being used.
     *
     * @return The {@link Bandage}
     */
    @Nonnull
    public Bandage getBandage() {
        return bandage;
    }

    /**
     * This returns the healing {@link PotionEffect} that will be applied.
     *
     * @return The healing {@link PotionEffect}
     */
    @Nonnull
    public PotionEffect getEffect() {
        return effect;
    }

    /**
     * This sets the healing {@link PotionEffect} that will be applied, overriding the
     * default INSTANT_HEALTH effect.
     *
     * @param effect
     *            The replacement {@link PotionEffect}, must not be null
     */
    public void setEffect(@Nonnull PotionEffect effect) {
        Validate.notNull(effect, "The PotionEffect must not be null");
        this.effect = effect;
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

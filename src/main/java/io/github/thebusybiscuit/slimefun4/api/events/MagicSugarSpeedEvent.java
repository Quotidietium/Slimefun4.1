package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.food.MagicSugar;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks {@link MagicSugar}:
 * the sugar is about to be consumed and the Speed {@link PotionEffect} applied.
 * <p>
 * Cancelling this event skips the use entirely: no sugar is consumed and no effect is applied.
 * Addons may also replace the applied {@link PotionEffect} via {@link #setEffect(PotionEffect)}.
 *
 * @author Zurker
 *
 * @see MagicSugar
 */
public class MagicSugarSpeedEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final MagicSugar magicSugar;

    private PotionEffect effect;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public MagicSugarSpeedEvent(Player player, MagicSugar magicSugar, PotionEffect effect) {
        super(player);
        Validate.notNull(magicSugar, "The MagicSugar must not be null");
        Validate.notNull(effect, "The PotionEffect must not be null");

        this.magicSugar = magicSugar;
        this.effect = effect;
    }

    /**
     * This returns the {@link MagicSugar} that is being consumed.
     *
     * @return The {@link MagicSugar}
     */
    @Nonnull
    public MagicSugar getMagicSugar() {
        return magicSugar;
    }

    /**
     * This returns the Speed {@link PotionEffect} that is about to be applied.
     *
     * @return The {@link PotionEffect} to apply
     */
    @Nonnull
    public PotionEffect getEffect() {
        return effect;
    }

    /**
     * This sets the {@link PotionEffect} that will be applied.
     *
     * @param effect
     *            The {@link PotionEffect} to apply
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

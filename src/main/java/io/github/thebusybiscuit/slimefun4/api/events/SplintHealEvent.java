package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.medical.Splint;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link Splint}
 * while injured or burning: the splint is about to be consumed and its {@link PotionEffect}
 * (instant health by default) applied.
 * <p>
 * Cancelling this event skips the splint's effect: it is not consumed, no sound is played and
 * no {@link PotionEffect} is applied. The underlying interaction is still cancelled, matching
 * the behavior of a completed splint use. Addons may also replace the applied effect via
 * {@link #setEffect(PotionEffect)}.
 *
 * @author Zurker
 *
 * @see Splint
 * @see BandageHealEvent
 */
public class SplintHealEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Splint splint;

    private PotionEffect effect;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public SplintHealEvent(Player player, Splint splint, PotionEffect effect) {
        super(player);
        Validate.notNull(splint, "The Splint must not be null");
        Validate.notNull(effect, "The PotionEffect must not be null");

        this.splint = splint;
        this.effect = effect;
    }

    /**
     * This returns the {@link Splint} that is being used.
     *
     * @return The {@link Splint}
     */
    @Nonnull
    public Splint getSplint() {
        return splint;
    }

    /**
     * This returns the {@link PotionEffect} that is about to be applied to the {@link Player}.
     *
     * @return The {@link PotionEffect} to apply
     */
    @Nonnull
    public PotionEffect getEffect() {
        return effect;
    }

    /**
     * This sets the {@link PotionEffect} that will be applied to the {@link Player}.
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.food.DietCookie;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} consumes a {@link DietCookie}:
 * the cookie's message and sound are about to be played and its {@link PotionEffect}
 * (levitation by default) applied.
 * <p>
 * Cancelling this event skips the whole consumption effect: no message, no sound and no
 * {@link PotionEffect}. Addons may also replace the applied effect via
 * {@link #setEffect(PotionEffect)}. Note that any pre-existing levitation is still removed
 * before the (possibly replaced) effect is applied.
 *
 * @author Zurker
 *
 * @see DietCookie
 * @see MagicSugarSpeedEvent
 */
public class DietCookieConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final DietCookie cookie;

    private PotionEffect effect;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public DietCookieConsumeEvent(Player player, DietCookie cookie, PotionEffect effect) {
        super(player);
        Validate.notNull(cookie, "The DietCookie must not be null");
        Validate.notNull(effect, "The PotionEffect must not be null");

        this.cookie = cookie;
        this.effect = effect;
    }

    /**
     * This returns the {@link DietCookie} that was consumed.
     *
     * @return The {@link DietCookie}
     */
    @Nonnull
    public DietCookie getCookie() {
        return cookie;
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

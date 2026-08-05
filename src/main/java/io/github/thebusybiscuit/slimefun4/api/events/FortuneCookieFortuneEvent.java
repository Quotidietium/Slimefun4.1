package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.food.FortuneCookie;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} consumes a {@link FortuneCookie}:
 * the rolled fortune message is about to be sent to the {@link Player}.
 * <p>
 * Cancelling this event skips the fortune entirely: no message is sent. Addons may also
 * replace the rolled message via {@link #setMessage(String)}. The message is colorized
 * with {@code ChatColors.color(String)} before being sent.
 *
 * @author Zurker
 *
 * @see FortuneCookie
 */
public class FortuneCookieFortuneEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final FortuneCookie cookie;

    private String message;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public FortuneCookieFortuneEvent(Player player, FortuneCookie cookie, String message) {
        super(player);
        Validate.notNull(cookie, "The FortuneCookie must not be null");
        Validate.notNull(message, "The fortune message must not be null");

        this.cookie = cookie;
        this.message = message;
    }

    /**
     * This returns the {@link FortuneCookie} that was consumed.
     *
     * @return The {@link FortuneCookie}
     */
    @Nonnull
    public FortuneCookie getCookie() {
        return cookie;
    }

    /**
     * This returns the rolled fortune message that is about to be sent.
     *
     * @return The rolled fortune message
     */
    @Nonnull
    public String getMessage() {
        return message;
    }

    /**
     * This sets the fortune message that will be sent to the {@link Player}.
     *
     * @param message
     *            The fortune message to send
     */
    public void setMessage(@Nonnull String message) {
        Validate.notNull(message, "The fortune message must not be null");

        this.message = message;
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

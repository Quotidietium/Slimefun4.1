package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.core.services.LocalizationService;
import io.github.thebusybiscuit.slimefun4.core.services.localization.Language;

/**
 * This {@link Event} gets called when a {@link Player} is about to switch their
 * {@link Language}: the new language has been picked but not applied yet.
 * <p>
 * Cancelling this event vetoes the change: the selection stays as it is, no
 * confirmation message is sent and the settings menu is not reopened. Addons may
 * also redirect the change via {@link #setNewLanguage(Language)}, e.g. to force a
 * language for a certain group of players.
 *
 * @author TheBusyBiscuit
 * @author Zurker
 *
 * @see Language
 * @see LocalizationService
 *
 */
public class PlayerLanguageChangeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Language from;
    private Language to;
    private boolean cancelled;

    public PlayerLanguageChangeEvent(@Nonnull Player p, @Nonnull Language from, @Nonnull Language to) {
        // Mirror the setter invariant (setNewLanguage): the @Nonnull getters' contract must
        // not be bypassable via the constructor.
        Validate.notNull(p, "The Player must not be null");
        Validate.notNull(from, "The previous Language must not be null");
        Validate.notNull(to, "The new Language must not be null");

        player = p;
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the {@link Player} who triggered this {@link Event},
     * the {@link Player} who switched his {@link Language} to be precise.
     *
     * @return The {@link Player} who switched his {@link Language}
     */
    @Nonnull
    public Player getPlayer() {
        return player;
    }

    /**
     * This returns the {@link Language} that this {@link Player} was using before.
     *
     * @return The previous {@link Language} of our {@link Player}
     */
    @Nonnull
    public Language getPreviousLanguage() {
        return from;
    }

    /**
     * This returns the {@link Language} that this {@link Player} wants to switch to.
     *
     * @return The new {@link Language}
     * @see #setNewLanguage(Language)
     */
    @Nonnull
    public Language getNewLanguage() {
        return to;
    }

    /**
     * This redirects the language change to a different {@link Language}: the given
     * language is applied instead of the picked one, as if the {@link Player} had
     * selected it.
     *
     * @param to
     *            The {@link Language} to apply instead, must not be null
     */
    public void setNewLanguage(@Nonnull Language to) {
        Validate.notNull(to, "The new Language must not be null");

        this.to = to;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

}

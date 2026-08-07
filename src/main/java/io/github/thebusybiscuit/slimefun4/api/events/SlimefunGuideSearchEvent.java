package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} runs a search in the
 * Slimefun guide, either by typing a search term into the chat prompt or by re-running
 * a term from their search history.
 * <p>
 * The event carries the raw input exactly as it was typed, before it is stripped of
 * colors and lowercased for matching. Replacing it via {@link #setSearchTerm(String)}
 * rewrites what is searched for, what the results menu is titled with and what gets
 * recorded in the {@link Player}'s guide history. Cancelling this event suppresses the
 * search entirely: no results menu is opened and nothing is added to the history.
 *
 * @author Zurker
 *
 * @see SlimefunGuideImplementation
 * @see SlimefunGuideOpenEvent
 */
public class SlimefunGuideSearchEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private String searchTerm;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public SlimefunGuideSearchEvent(Player player, String searchTerm) {
        super(player);
        Validate.notNull(searchTerm, "The search term must not be null");

        this.searchTerm = searchTerm;
    }

    /**
     * This returns the search term exactly as it was typed.
     *
     * @return The raw search term
     */
    @Nonnull
    public String getSearchTerm() {
        return searchTerm;
    }

    /**
     * This replaces the search term. The new term is used for matching items, for the
     * results menu title and for the guide history entry.
     *
     * @param searchTerm
     *            The new search term, must not be null
     */
    public void setSearchTerm(@Nonnull String searchTerm) {
        Validate.notNull(searchTerm, "The search term must not be null");
        this.searchTerm = searchTerm;
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

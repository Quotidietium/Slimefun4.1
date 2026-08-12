package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;

/**
 * This {@link PlayerEvent} is fired for every candidate {@link SlimefunItem} while the
 * Slimefun guide evaluates a search: after the item passed the hidden/item-group checks,
 * the guide asks whether the item matches the search term.
 * <p>
 * The event carries the guide's own verdict in {@link #isMatching()} - the built-in
 * matching compares the item's name against the term. {@link #setMatching(boolean)} lets
 * addons override that verdict for this item, e.g. to match custom keywords or to hide
 * specific items from the results. The override applies to this evaluation only.
 * <p>
 * The search term is the processed one: stripped of colors and lowercased, exactly what
 * the built-in matching compares against. The raw term as typed (and a veto of the whole
 * search) is covered by {@link SlimefunGuideSearchEvent}.
 * <p>
 * This event is not cancellable and fires once per candidate item per search, so
 * listeners should be cheap.
 *
 * @author Zurker
 *
 * @see SlimefunGuideSearchEvent
 * @see SlimefunGuideImplementation
 */
public class SlimefunGuideSearchFilterEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem item;
    private final String searchTerm;

    private boolean matching;

    @ParametersAreNonnullByDefault
    public SlimefunGuideSearchFilterEvent(Player player, SlimefunItem item, String searchTerm, boolean matching) {
        super(player);
        Validate.notNull(item, "The SlimefunItem must not be null");
        Validate.notNull(searchTerm, "The search term must not be null");

        this.item = item;
        this.searchTerm = searchTerm;
        this.matching = matching;
    }

    /**
     * This returns the candidate {@link SlimefunItem} the guide is currently
     * evaluating for the search results.
     *
     * @return The candidate {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getItem() {
        return item;
    }

    /**
     * This returns the processed search term (stripped of colors, lowercased) that
     * the guide is matching against.
     *
     * @return The processed search term
     */
    @Nonnull
    public String getSearchTerm() {
        return searchTerm;
    }

    /**
     * This returns whether the item will appear in the search results. It defaults
     * to the guide's own verdict (the built-in name matching).
     *
     * @return Whether the item matches the search
     */
    public boolean isMatching() {
        return matching;
    }

    /**
     * This overrides whether the item will appear in the search results. The
     * override applies to this single evaluation only.
     *
     * @param matching
     *            Whether the item matches the search
     */
    public void setMatching(boolean matching) {
        this.matching = matching;
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

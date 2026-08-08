package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;

/**
 * This {@link Event} is fired when a player unlocks their last remaining enabled
 * {@link io.github.thebusybiscuit.slimefun4.api.researches.Research}, completing the
 * entire Slimefun research tree.
 * <p>
 * The event is informational only (not cancellable): the research has already been
 * unlocked. It fires at most once per player per server lifetime — after this point the
 * player has no more researches to unlock. It uses adaptive asynchronicity because the
 * research unlock path may run on the profile-loading thread.
 * <p>
 * Add-ons can use this for milestone rewards, announcements, or achievement tracking.
 *
 * @author Zurker
 *
 * @see ResearchUnlockEvent
 * @see ResearchCostEvent
 * @see io.github.thebusybiscuit.slimefun4.api.researches.Research
 */
public class PlayerAllResearchesUnlockEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final PlayerProfile profile;
    private final int totalResearches;

    public PlayerAllResearchesUnlockEvent(@Nonnull Player player, @Nonnull PlayerProfile profile, int totalResearches) {
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(player, "The Player must not be null");
        Validate.notNull(profile, "The PlayerProfile must not be null");
        Validate.isTrue(totalResearches > 0, "The total research count must be positive");

        this.player = player;
        this.profile = profile;
        this.totalResearches = totalResearches;
    }

    /**
     * This returns the {@link Player} who completed all researches.
     *
     * @return The {@link Player}
     */
    @Nonnull
    public Player getPlayer() {
        return player;
    }

    /**
     * This returns the {@link PlayerProfile} of the completing player.
     *
     * @return The {@link PlayerProfile}
     */
    @Nonnull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * This returns the total number of enabled researches that were completed.
     *
     * @return The total enabled research count
     */
    public int getTotalResearches() {
        return totalResearches;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;

/**
 * This {@link Event} is fired whenever a player's research rank <b>title</b> changes.
 *
 * <p>
 * The title is derived from the fraction of unlocked {@link io.github.thebusybiscuit.slimefun4.api.researches.Research Researches}
 * (see {@link PlayerProfile#getTitle()}) and is configured via the {@code research-ranks}
 * list in {@code config.yml}. This event is raised right after a research was unlocked
 * or re-locked and only when that change caused the player to cross a rank boundary.
 * </p>
 *
 * <p>
 * The event is purely observational and therefore <b>not cancellable</b>. It is raised
 * on the same thread that performed the {@link PlayerProfile#setResearched(Research, boolean)}
 * call (typically the main thread).
 * </p>
 *
 * <p>
 * If no {@code research-ranks} are configured the event is never fired.
 * </p>
 *
 * @author Zurker
 *
 * @see PlayerProfile#getTitle()
 *
 */
public class PlayerResearchRankChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerProfile profile;
    private final String previousTitle;
    private final String newTitle;

    public PlayerResearchRankChangeEvent(@Nonnull PlayerProfile profile, @Nonnull String previousTitle, @Nonnull String newTitle) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(profile, "The PlayerProfile cannot be null");
        Validate.notNull(previousTitle, "The previous title cannot be null");
        Validate.notNull(newTitle, "The new title cannot be null");

        this.profile = profile;
        this.previousTitle = previousTitle;
        this.newTitle = newTitle;
    }

    /**
     * The {@link PlayerProfile} whose rank title changed.
     *
     * @return The affected {@link PlayerProfile}
     */
    @Nonnull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * The {@link UUID} of the player whose rank title changed.
     *
     * @return The {@link UUID} of the owning player
     */
    @Nonnull
    public UUID getUUID() {
        return profile.getUUID();
    }

    /**
     * The online {@link Player}, or {@code null} if the player is currently offline.
     *
     * @return The {@link Player} or null
     */
    @Nullable
    public Player getPlayer() {
        return profile.getPlayer();
    }

    /**
     * The research rank title the player held <b>before</b> the change.
     *
     * @return The previous title
     */
    @Nonnull
    public String getPreviousTitle() {
        return previousTitle;
    }

    /**
     * The research rank title the player holds <b>after</b> the change.
     *
     * @return The new title
     */
    @Nonnull
    public String getNewTitle() {
        return newTitle;
    }

    /**
     * The direction of the change.
     *
     * @return {@code true} if the player was promoted to a higher rank, {@code false} if demoted.
     */
    public boolean isPromotion() {
        return !previousTitle.equals(newTitle);
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

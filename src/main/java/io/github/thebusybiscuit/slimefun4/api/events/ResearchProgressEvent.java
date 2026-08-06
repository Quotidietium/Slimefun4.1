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
import io.github.thebusybiscuit.slimefun4.api.researches.Research;

/**
 * This {@link Event} is fired whenever a player's unlocked-research count actually changes,
 * i.e. whenever a {@link Research} is newly unlocked or re-locked <b>and</b> that call flips
 * the player's ownership of it. A no-op call (unlocking an already unlocked research, or
 * locking one that was never unlocked) never fires this event.
 *
 * <p>
 * Unlike {@link PlayerResearchRankChangeEvent}, which only fires when the player crosses a
 * configured {@code research-ranks} title boundary (and never fires at all when no ranks are
 * configured), this event fires on <b>every</b> genuine research-count change and carries
 * the exact before/after counts. Add-ons can therefore build their own progression trackers,
 * progress bars, milestones or achievement pop-ups without depending on the rank
 * configuration.
 * </p>
 *
 * <p>
 * The event is purely observational and therefore <b>not cancellable</b>. It is raised on the
 * same thread that performed the {@link PlayerProfile#setResearched(Research, boolean)} call
 * (typically the main thread), right after the profile was mutated and marked dirty and right
 * before the optional rank-change event.
 * </p>
 *
 * <p>
 * Counts follow the same "non-empty research" definition used by
 * {@link PlayerProfile#getTitle()}: only {@link Research Researches} that have at least one
 * enabled item are counted, so {@link #getProgressFraction()} stays consistent with the
 * in-game research title. Note that the affected {@link Research} itself may carry no enabled
 * items, in which case the ownership still flips (the event still fires) but
 * {@link #getDelta()} is {@code 0}.
 * </p>
 *
 * @author Zurker
 *
 * @see PlayerProfile#setResearched(Research, boolean)
 * @see PlayerResearchRankChangeEvent
 * @see ResearchUnlockEvent
 * @see ResearchLockEvent
 */
public class ResearchProgressEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerProfile profile;
    private final Research research;
    private final boolean unlocked;
    private final int previousCount;
    private final int newCount;
    private final int totalResearches;

    public ResearchProgressEvent(@Nonnull PlayerProfile profile, @Nonnull Research research, boolean unlocked, int previousCount, int newCount, int totalResearches) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(profile, "The PlayerProfile cannot be null");
        Validate.notNull(research, "The Research cannot be null");
        Validate.isTrue(previousCount >= 0, "The previous count must not be negative");
        Validate.isTrue(newCount >= 0, "The new count must not be negative");
        Validate.isTrue(totalResearches >= 0, "The total research count must not be negative");
        Validate.isTrue(newCount <= totalResearches, "The new count must not exceed the total research count");

        this.profile = profile;
        this.research = research;
        this.unlocked = unlocked;
        this.previousCount = previousCount;
        this.newCount = newCount;
        this.totalResearches = totalResearches;
    }

    /**
     * The {@link PlayerProfile} whose research count changed.
     *
     * @return The affected {@link PlayerProfile}
     */
    @Nonnull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * The {@link UUID} of the player whose research count changed.
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
     * The {@link Research} whose ownership changed.
     *
     * @return The affected {@link Research}
     */
    @Nonnull
    public Research getResearch() {
        return research;
    }

    /**
     * Whether the research was newly unlocked ({@code true}) or re-locked ({@code false}).
     *
     * @return {@code true} for an unlock, {@code false} for a re-lock
     */
    public boolean isUnlocked() {
        return unlocked;
    }

    /**
     * The number of non-empty researches the player had unlocked <b>before</b> this change.
     *
     * @return The previous unlocked count
     */
    public int getPreviousCount() {
        return previousCount;
    }

    /**
     * The number of non-empty researches the player has unlocked <b>after</b> this change.
     *
     * @return The new unlocked count
     */
    public int getNewCount() {
        return newCount;
    }

    /**
     * The total number of non-empty researches that exist in the registry.
     *
     * @return The total research count
     */
    public int getTotalResearches() {
        return totalResearches;
    }

    /**
     * How many non-empty researches this single change added (positive) or removed (negative).
     * Always {@code 1} for a regular unlock and {@code -1} for a regular re-lock, but may be
     * {@code 0} when the affected {@link Research} carries no enabled items.
     *
     * @return The change in unlocked count
     */
    public int getDelta() {
        return newCount - previousCount;
    }

    /**
     * The fraction of non-empty researches the player has now unlocked, in the range
     * {@code [0.0, 1.0]}. Returns {@code 0.0} when there are no non-empty researches at all.
     *
     * @return The unlocked fraction, between {@code 0.0} and {@code 1.0}
     */
    public float getProgressFraction() {
        if (totalResearches == 0) {
            return 0.0F;
        }

        return (float) newCount / totalResearches;
    }

    /**
     * Whether this change unlocked the very last non-empty research, i.e. the player now has
     * every research unlocked. Convenient for firing an "all researches complete" cue.
     *
     * @return {@code true} if the player has now unlocked all non-empty researches
     */
    public boolean isFullyUnlocked() {
        return totalResearches > 0 && newCount == totalResearches;
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

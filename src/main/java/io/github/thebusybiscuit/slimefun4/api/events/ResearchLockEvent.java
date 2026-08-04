package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;

/**
 * This {@link Event} is called whenever a {@link Research} is about to be removed
 * (re-locked) from a {@link PlayerProfile}, for example through an admin command
 * resetting a player's research progress.
 *
 * <p>
 * Unlike {@link ResearchUnlockEvent}, this event is fired from within
 * {@link PlayerProfile#setResearched(Research, boolean)} and is therefore raised for
 * <b>any</b> removal path (interactive or programmatic). Cancelling it prevents the
 * {@link Research} from being removed, allowing add-ons to protect specific unlocks.
 * </p>
 *
 * <p>
 * Note: the associated {@link Player} may be offline (e.g. an admin resetting an offline
 * player). Use {@link #getUUID()} for stable identification and treat
 * {@link #getPlayer()} as nullable.
 * </p>
 *
 * @author Zurker
 *
 * @see Research
 * @see ResearchUnlockEvent
 * @see PlayerProfile#setResearched(Research, boolean)
 *
 */
public class ResearchLockEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerProfile profile;
    private final Research research;
    private boolean cancelled;

    public ResearchLockEvent(@Nonnull PlayerProfile profile, @Nonnull Research research) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(profile, "The PlayerProfile cannot be null");
        Validate.notNull(research, "Research cannot be null");

        this.profile = profile;
        this.research = research;
    }

    /**
     * The {@link PlayerProfile} the {@link Research} is being removed from.
     *
     * @return The affected {@link PlayerProfile}
     */
    @Nonnull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * The {@link Research} that is being re-locked.
     *
     * @return The affected {@link Research}
     */
    @Nonnull
    public Research getResearch() {
        return research;
    }

    /**
     * The {@link UUID} of the player whose {@link Research} is being removed.
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

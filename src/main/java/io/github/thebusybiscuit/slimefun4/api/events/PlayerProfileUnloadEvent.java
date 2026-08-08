package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;

/**
 * This {@link Event} is fired when a {@link PlayerProfile} is about to be removed from
 * memory during the auto-save cycle, after it has been saved (if dirty) and the owning
 * player has left the server.
 * <p>
 * The event is informational only (not cancellable): the removal is about to happen and
 * cannot be prevented. It fires synchronously from the auto-save thread. This is the
 * "unload" companion to {@link AsyncProfileLoadEvent} (the "load" event): a profile is
 * loaded when the player interacts with Slimefun and unloaded when the player has left
 * and the next save cycle confirms they are still gone.
 * <p>
 * Add-ons can use this to clean up profile-related caches, log session duration, or save
 * custom data before the profile reference is gone.
 *
 * @author Zurker
 *
 * @see AsyncProfileLoadEvent
 * @see SlimefunAutoSaveEvent
 * @see PlayerProfile
 */
public class PlayerProfileUnloadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerProfile profile;
    private final UUID uuid;

    public PlayerProfileUnloadEvent(@Nonnull PlayerProfile profile) {
        Validate.notNull(profile, "The PlayerProfile must not be null");

        this.profile = profile;
        this.uuid = profile.getUUID();
    }

    /**
     * This returns the {@link PlayerProfile} that is about to be removed from memory.
     * The profile is still fully accessible at this point.
     *
     * @return The {@link PlayerProfile}
     */
    @Nonnull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * This returns the {@link UUID} of the player whose profile is being unloaded.
     *
     * @return The player {@link UUID}
     */
    @Nonnull
    public UUID getUUID() {
        return uuid;
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

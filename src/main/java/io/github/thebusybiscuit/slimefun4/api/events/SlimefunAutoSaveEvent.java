package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This {@link Event} is fired after the {@link io.github.thebusybiscuit.slimefun4.core.services.AutoSavingService}
 * has finished saving all dirty player profiles and removing profiles that were marked
 * for deletion.
 * <p>
 * The event is informational only (not cancellable): the save has already completed.
 * It fires on the auto-save thread (asynchronous in production), using adaptive
 * asynchronicity. Add-ons can use it to synchronise their own data with the Slimefun
 * save cycle, trigger backups, or monitor save performance.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.core.services.AutoSavingService
 */
public class SlimefunAutoSaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final int profilesSaved;

    public SlimefunAutoSaveEvent(int profilesSaved) {
        super(!Bukkit.isPrimaryThread());
        Validate.isTrue(profilesSaved >= 0, "The saved profile count must not be negative");

        this.profilesSaved = profilesSaved;
    }

    /**
     * This returns how many player profiles were saved during this auto-save cycle.
     * Profiles that were not dirty (no changes since the last save) are not counted.
     *
     * @return The number of profiles saved
     */
    public int getProfilesSaved() {
        return profilesSaved;
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

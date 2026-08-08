package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This {@link Event} is fired after the {@link io.github.thebusybiscuit.slimefun4.core.services.AutoSavingService}
 * has finished saving all dirty block data across every world and flushed pending chunk
 * writes.
 * <p>
 * The event is informational only (not cancellable): the save has already completed.
 * It fires on the auto-save thread (asynchronous in production), using adaptive
 * asynchronicity. This is the block-data companion to {@link SlimefunAutoSaveEvent}
 * (which covers player profiles): add-ons can use it to synchronise backups, trigger
 * data-export pipelines, or monitor save performance.
 *
 * @author Zurker
 *
 * @see SlimefunAutoSaveEvent
 * @see io.github.thebusybiscuit.slimefun4.core.services.AutoSavingService
 */
public class SlimefunBlockDataSaveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final int worldsSaved;

    public SlimefunBlockDataSaveEvent(int worldsSaved) {
        super(!Bukkit.isPrimaryThread());
        Validate.isTrue(worldsSaved >= 0, "The saved world count must not be negative");

        this.worldsSaved = worldsSaved;
    }

    /**
     * This returns how many worlds had dirty block data that was saved during this cycle.
     * Worlds without changes are not counted.
     *
     * @return The number of worlds saved
     */
    public int getWorldsSaved() {
        return worldsSaved;
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

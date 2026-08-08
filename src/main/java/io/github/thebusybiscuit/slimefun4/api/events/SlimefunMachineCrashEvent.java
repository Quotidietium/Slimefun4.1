package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link Event} is fired when a Slimefun machine has thrown an exception in its
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.BlockTicker} for the fourth
 * consecutive tick and is about to be terminated (its block destroyed, its inventory
 * dropped and its BlockStorage data wiped).
 * <p>
 * Cancelling this event spares the machine: it is not destroyed. The error counter stays
 * at four, so subsequent ticks increment it past the threshold without re-triggering
 * termination - the machine remains in its broken state until the underlying issue is
 * resolved or it is removed manually.
 * <p>
 * The event may fire on either the asynchronous ticker thread or the main thread
 * (synchronized ticks run on the main thread), so it uses adaptive asynchronicity.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask
 */
public class SlimefunMachineCrashEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Location location;
    private final SlimefunItem item;

    private boolean cancelled;

    public SlimefunMachineCrashEvent(@Nonnull Location location, @Nonnull SlimefunItem item) {
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(item, "The SlimefunItem must not be null");

        this.location = location;
        this.item = item;
    }

    /**
     * This returns the {@link Location} of the machine that is crashing.
     *
     * @return The {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the {@link SlimefunItem} whose ticker has thrown four consecutive errors.
     *
     * @return The crashing {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getItem() {
        return item;
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

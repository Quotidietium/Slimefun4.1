package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.api.geo.ResourceManager;
import io.github.thebusybiscuit.slimefun4.implementation.items.geo.GEOScanner;
import io.github.thebusybiscuit.slimefun4.implementation.items.geo.PortableGEOScanner;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} starts a geo-scan of a chunk
 * (via a {@link GEOScanner} or a {@link PortableGEOScanner}), after the GPS network
 * complexity check has passed and before the scan results are displayed.
 * <p>
 * Cancelling this event skips the display entirely: no results menu is opened, allowing an
 * addon to veto the scan or to present the results through its own interface. The displayed
 * page may be changed via {@link #setPage(int)}.
 *
 * @author Zurker
 *
 * @see ResourceManager
 * @see GEOScanner
 * @see PortableGEOScanner
 */
public class GEOScanEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block block;
    private int page;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public GEOScanEvent(Player player, Block block, int page) {
        super(player);
        Validate.notNull(block, "The scanned Block must not be null");
        Validate.isTrue(page >= 0, "The page must not be negative");

        this.block = block;
        this.page = page;
    }

    /**
     * This returns the {@link Block} the scan was started at. Note that scans always cover
     * the whole chunk this {@link Block} is in.
     *
     * @return The scanned {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the results page that is about to be displayed.
     *
     * @return The page to display
     */
    public int getPage() {
        return page;
    }

    /**
     * This sets the results page that will be displayed.
     *
     * @param page
     *            The page to display, must not be negative
     */
    public void setPage(int page) {
        Validate.isTrue(page >= 0, "The page must not be negative");
        this.page = page;
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

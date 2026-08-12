package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.implementation.items.geo.GEOMiner;
import io.github.thebusybiscuit.slimefun4.implementation.operations.GEOMiningOperation;

/**
 * This {@link Event} is fired whenever a {@link GEOMiner} has found a
 * {@link GEOResource} with remaining supplies in its {@link org.bukkit.Chunk}
 * and is about to start a {@link GEOMiningOperation}, consuming one unit of the
 * chunk's supplies.
 * <p>
 * Cancelling this event vetoes the extraction: no operation is started, the
 * supplies stay untouched and the miner idles for this tick (it will retry on
 * the next one).
 * <p>
 * By default one extraction consumes exactly one unit of the chunk's supplies.
 * Addons may adjust that via {@link #setConsumedSupplies(int)} - e.g. {@code 0}
 * for a free "lucky" extraction or a higher value for a rich-vein bonus pull.
 * The amount is capped at the available {@link #getSupplies() supplies}; if the
 * miner is broken mid-operation, the consumed units are returned to the chunk.
 * <p>
 * The event does not fire when the chunk was never scanned, when no supplies
 * remain or when the output slots cannot hold the result: the miner already
 * idles in those cases and no supplies would be consumed. Note that breaking
 * the miner mid-operation returns the unit to the chunk via
 * {@link GEOMiningOperation#onCancel}, which is not covered by this event.
 * <p>
 * This complements {@link GEOResourceGenerationEvent}, which fires when the
 * supplies of a chunk are generated, and {@link GEOScanEvent}, which fires when
 * a player displays them.
 *
 * @author Zurker
 *
 * @see GEOResourceGenerationEvent
 * @see GEOScanEvent
 * @see MachineRecipeStartEvent
 */
public class GEOMiningStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final GEOMiner miner;
    private final Location location;
    private final GEOResource resource;
    private final int supplies;

    private int consumedSupplies = 1;
    private boolean cancelled;

    public GEOMiningStartEvent(@Nonnull GEOMiner miner, @Nonnull Location location, @Nonnull GEOResource resource, int supplies) {
        Validate.notNull(miner, "The GEOMiner must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(resource, "The GEOResource must not be null");
        Validate.isTrue(supplies > 0, "The supplies must be positive");

        this.miner = miner;
        this.location = location;
        this.resource = resource;
        this.supplies = supplies;
    }

    /**
     * This returns the {@link GEOMiner} that is about to start mining.
     *
     * @return The {@link GEOMiner}
     */
    @Nonnull
    public GEOMiner getMiner() {
        return miner;
    }

    /**
     * This returns the {@link Location} of the {@link GEOMiner}.
     *
     * @return The {@link Location} of the {@link GEOMiner}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the {@link GEOResource} that is about to be mined.
     *
     * @return The {@link GEOResource}
     */
    @Nonnull
    public GEOResource getResource() {
        return resource;
    }

    /**
     * This returns the remaining supplies of the {@link GEOResource} in the
     * {@link org.bukkit.Chunk} before this extraction, so after a non-cancelled
     * extraction exactly {@code supplies - 1} units remain.
     *
     * @return The remaining supplies before this extraction
     */
    public int getSupplies() {
        return supplies;
    }

    /**
     * This returns how many units of the chunk's supplies this extraction will consume.
     * Defaults to {@code 1}. If the miner is broken mid-operation, this many units are
     * returned to the chunk.
     *
     * @return The supplies this extraction consumes
     */
    public int getConsumedSupplies() {
        return consumedSupplies;
    }

    /**
     * This sets how many units of the chunk's supplies this extraction will consume.
     * A value of {@code 0} makes the extraction free of charge.
     *
     * @param consumedSupplies
     *            The supplies to consume, between {@code 0} and {@link #getSupplies()}
     */
    public void setConsumedSupplies(int consumedSupplies) {
        Validate.isTrue(consumedSupplies >= 0, "The consumed supplies must not be negative");
        Validate.isTrue(consumedSupplies <= supplies, "The consumed supplies must not exceed the available supplies");

        this.consumedSupplies = consumedSupplies;
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

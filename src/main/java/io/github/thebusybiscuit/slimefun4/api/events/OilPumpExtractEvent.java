package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.implementation.items.geo.OilPump;

/**
 * This {@link Event} is fired whenever an {@link OilPump} has found an empty
 * bucket and remaining oil supplies in its {@link org.bukkit.Chunk} and is about
 * to extract one unit: the bucket is about to be consumed and the supplies about
 * to be decremented.
 * <p>
 * Cancelling this event vetoes the extraction: the bucket stays in its input
 * slot, the supplies stay untouched and no operation is started. The pump
 * re-scans on its next tick, so a persistent veto behaves like a jammed output.
 * <p>
 * Machines with dynamic recipes (an empty recipe list, like the {@link OilPump})
 * bypass the generic recipe scan, so {@link MachineRecipeStartEvent} never fires
 * for them and this event is the only veto point before the bucket and the
 * supplies are consumed. {@link AsyncMachineOperationStartEvent} still fires
 * afterwards, once the operation exists and both are already gone. For the
 * solid-resource counterpart see {@link GEOMiningStartEvent}.
 * <p>
 * Addons may also adjust how many supply units the extraction consumes via
 * {@link #setSuppliesCost(int)}, e.g. to make an upgraded pump extract for free
 * (a cost of zero) or to drain the chunk faster. The cost defaults to one and
 * must not exceed the remaining {@link #getSupplies() supplies}.
 *
 * @author Zurker
 *
 * @see MachineRecipeStartEvent
 * @see GEOMiningStartEvent
 * @see OilPump
 */
public class OilPumpExtractEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final OilPump pump;
    private final Location location;
    private final GEOResource resource;
    private final int slot;
    private final int supplies;

    private int suppliesCost = 1;
    private boolean cancelled;

    public OilPumpExtractEvent(@Nonnull OilPump pump, @Nonnull Location location, @Nonnull GEOResource resource, int slot, int supplies) {
        Validate.notNull(pump, "The OilPump must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(resource, "The GEOResource must not be null");
        Validate.isTrue(slot >= 0, "The slot must not be negative");
        Validate.isTrue(supplies > 0, "The supplies must be positive");

        this.pump = pump;
        this.location = location;
        this.resource = resource;
        this.slot = slot;
        this.supplies = supplies;
    }

    /**
     * This returns the {@link OilPump} that is about to extract.
     *
     * @return The {@link OilPump}
     */
    @Nonnull
    public OilPump getPump() {
        return pump;
    }

    /**
     * This returns the {@link Location} of the {@link OilPump}.
     *
     * @return The {@link Location} of the {@link OilPump}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the {@link GEOResource} being extracted (oil).
     *
     * @return The {@link GEOResource}
     */
    @Nonnull
    public GEOResource getResource() {
        return resource;
    }

    /**
     * This returns the input slot holding the empty bucket that is about to
     * be consumed.
     *
     * @return The input slot
     */
    public int getSlot() {
        return slot;
    }

    /**
     * This returns the remaining supplies in the {@link org.bukkit.Chunk}
     * before this extraction, so after a non-cancelled extraction exactly
     * {@code supplies - getSuppliesCost()} units remain.
     *
     * @return The remaining supplies before this extraction
     */
    public int getSupplies() {
        return supplies;
    }

    /**
     * This returns how many supply units this extraction will consume. It defaults
     * to one unit per filled bucket.
     *
     * @return The supplies cost of this extraction
     * @see #setSuppliesCost(int)
     */
    public int getSuppliesCost() {
        return suppliesCost;
    }

    /**
     * This sets how many supply units this extraction will consume. A cost of zero
     * makes the pump extract without draining the {@link org.bukkit.Chunk} (e.g. for
     * an efficiency upgrade); a higher cost drains it faster.
     *
     * @param suppliesCost
     *            The supplies cost, between 0 and the remaining {@link #getSupplies()}
     */
    public void setSuppliesCost(int suppliesCost) {
        Validate.isTrue(suppliesCost >= 0, "The supplies cost must not be negative");
        Validate.isTrue(suppliesCost <= supplies, "The supplies cost must not exceed the remaining supplies");

        this.suppliesCost = suppliesCost;
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

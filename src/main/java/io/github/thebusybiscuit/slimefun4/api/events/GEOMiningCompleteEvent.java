package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.geo.GEOMiner;

/**
 * This {@link Event} is fired when a {@link GEOMiner} finishes mining a resource and is
 * about to push the output to its slots.
 * <p>
 * Because {@link GEOMiner} uses its own tick method (not {@code AContainer}),
 * {@link AsyncMachineOperationFinishEvent} does not fire for it — this event is the only
 * observation and veto point for the completed output.
 * <p>
 * Cancelling this event voids the output: it is not pushed and the operation is ended.
 * The output can be replaced via {@link #setResult(ItemStack)} before it is pushed.
 *
 * @author Zurker
 *
 * @see GEOMiningStartEvent
 * @see AsyncMachineOperationFinishEvent
 * @see GEOMiner
 */
public class GEOMiningCompleteEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final GEOMiner miner;
    private final Location location;
    private ItemStack result;

    private boolean cancelled;

    public GEOMiningCompleteEvent(@Nonnull GEOMiner miner, @Nonnull Location location, @Nonnull ItemStack result) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(miner, "The GEOMiner must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(result, "The result must not be null");

        this.miner = miner;
        this.location = location;
        this.result = result;
    }

    /**
     * This returns the {@link GEOMiner} that has finished mining.
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
     * @return The {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the output item that will be pushed to the output slots.
     *
     * @return The output {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This sets the output item that will be pushed.
     *
     * @param result
     *            The replacement output, must not be null
     */
    public void setResult(@Nonnull ItemStack result) {
        Validate.notNull(result, "The result must not be null");
        this.result = result;
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

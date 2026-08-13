package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.InfusedHopper;

/**
 * This {@link Event} is fired whenever an {@link InfusedHopper} is about to collect
 * a dropped {@link Item} by teleporting it onto itself.
 * <p>
 * The event fires once per item in range. Cancelling it skips that item for this
 * tick: it stays where it is and will be re-evaluated on the next tick.
 * <p>
 * Addons may also redirect the collection via {@link #setDestination(Location)}, e.g.
 * to route valuables into a secure vault instead of onto the hopper. The destination
 * defaults to the collection point above the hopper; the teleport sound still plays.
 *
 * @author Zurker
 *
 * @see InfusedHopper
 * @see ItemMagnetPullEvent
 */
public class InfusedHopperCollectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final InfusedHopper hopper;
    private final Block block;
    private final Item item;

    private Location destination;
    private boolean cancelled;

    /**
     * This creates a new {@link InfusedHopperCollectEvent}. The collection destination
     * defaults to the point above the hopper, computed from the {@link Block}'s
     * coordinates.
     *
     * @param hopper
     *            The {@link InfusedHopper} that is collecting
     * @param block
     *            The {@link Block} of the {@link InfusedHopper}
     * @param item
     *            The dropped {@link Item} about to be collected
     */
    public InfusedHopperCollectEvent(@Nonnull InfusedHopper hopper, @Nonnull Block block, @Nonnull Item item) {
        this(hopper, block, item, defaultDestination(block));
    }

    /**
     * This creates a new {@link InfusedHopperCollectEvent} with an explicit default
     * collection destination. {@link InfusedHopper} itself uses this to hand in the
     * exact collection point it computed for this tick.
     *
     * @param hopper
     *            The {@link InfusedHopper} that is collecting
     * @param block
     *            The {@link Block} of the {@link InfusedHopper}
     * @param item
     *            The dropped {@link Item} about to be collected
     * @param destination
     *            The default collection destination
     */
    public InfusedHopperCollectEvent(@Nonnull InfusedHopper hopper, @Nonnull Block block, @Nonnull Item item, @Nonnull Location destination) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(hopper, "The InfusedHopper must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(item, "The Item must not be null");
        Validate.notNull(destination, "The destination must not be null");

        this.hopper = hopper;
        this.block = block;
        this.item = item;
        this.destination = destination;
    }

    /**
     * Computes the default collection destination: the point above the hopper,
     * derived from the {@link Block}'s coordinates. Validates the {@link Block}
     * upfront so the delegating constructor keeps rejecting null blocks with an
     * {@link IllegalArgumentException}.
     */
    @Nonnull
    private static Location defaultDestination(@Nonnull Block block) {
        Validate.notNull(block, "The Block must not be null");

        return new Location(block.getWorld(), block.getX() + 0.5, block.getY() + 1.2, block.getZ() + 0.5);
    }

    /**
     * This returns the {@link InfusedHopper} that is collecting the item.
     *
     * @return The {@link InfusedHopper}
     */
    @Nonnull
    public InfusedHopper getHopper() {
        return hopper;
    }

    /**
     * This returns the {@link Block} of the {@link InfusedHopper}.
     *
     * @return The {@link InfusedHopper} {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the dropped {@link Item} that is about to be teleported onto
     * the {@link InfusedHopper}.
     *
     * @return The dropped {@link Item}
     */
    @Nonnull
    public Item getItem() {
        return item;
    }

    /**
     * This returns the {@link Location} the {@link Item} will be teleported to. It
     * defaults to the collection point above the {@link InfusedHopper}.
     *
     * @return The collection destination
     * @see #setDestination(Location)
     */
    @Nonnull
    public Location getDestination() {
        return destination;
    }

    /**
     * This redirects the collection to a different {@link Location}: the {@link Item}
     * is teleported there instead of onto the {@link InfusedHopper}.
     *
     * @param destination
     *            The collection destination, must not be null
     */
    public void setDestination(@Nonnull Location destination) {
        Validate.notNull(destination, "The destination must not be null");

        this.destination = destination;
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

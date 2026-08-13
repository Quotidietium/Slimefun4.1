package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;

/**
 * This {@link Event} is fired whenever a {@link Reactor} reaches a cooling checkpoint of its
 * fuel operation and has found a matching coolant cell in one of its coolant slots: the cell
 * is about to be consumed to keep the {@link Reactor} cooled.
 * <p>
 * Cancelling this event vetoes the cooling: the coolant cell is not consumed and the
 * {@link Reactor} is treated as uncooled, so it will begin its explosion countdown just as
 * if no coolant had been present (see {@link ReactorExplodeEvent} for the aftermath).
 *
 * @author Zurker
 *
 * @see Reactor
 * @see ReactorExplodeEvent
 * @see ReactorProduceByproductEvent
 */
public class ReactorCoolantConsumeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Reactor reactor;
    private final Location location;
    private final ItemStack coolantItem;
    private final int slot;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public ReactorCoolantConsumeEvent(Reactor reactor, Location location, ItemStack coolantItem, int slot) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(reactor, "The Reactor must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(coolantItem, "The coolant item must not be null");
        Validate.isTrue(slot >= 0, "The slot must not be negative");

        this.reactor = reactor;
        this.location = location;
        this.coolantItem = coolantItem;
        this.slot = slot;
    }

    /**
     * This returns the {@link Reactor} that is about to consume a coolant cell.
     *
     * @return The {@link Reactor}
     */
    @Nonnull
    public Reactor getReactor() {
        return reactor;
    }

    /**
     * This returns the {@link Location} of the {@link Reactor}.
     *
     * @return The {@link Location} of the reactor
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the coolant {@link ItemStack} sitting in the coolant slot, the stack
     * that is about to lose one unit.
     *
     * @return The coolant {@link ItemStack}
     */
    @Nonnull
    public ItemStack getCoolantItem() {
        return coolantItem;
    }

    /**
     * This returns the coolant slot the cell will be consumed from.
     *
     * @return The coolant slot index
     */
    public int getSlot() {
        return slot;
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

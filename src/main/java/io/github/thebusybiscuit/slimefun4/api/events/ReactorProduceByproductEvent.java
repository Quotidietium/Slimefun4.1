package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;

/**
 * This {@link Event} is fired whenever a {@link Reactor} has finished burning a fuel that
 * yields a byproduct and is about to push that byproduct into the reactor's output slot:
 * the {@link ItemStack} is about to be produced.
 * <p>
 * The byproduct {@link ItemStack} is modifiable via {@link #setResult(ItemStack)} - an addon
 * may swap it for a custom item. Cancelling this event produces no byproduct at all: the fuel
 * is still consumed (the operation ended) but nothing is pushed to the output.
 *
 * @author Zurker
 *
 * @see Reactor
 */
public class ReactorProduceByproductEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Reactor reactor;
    private final Location location;
    private ItemStack result;

    private boolean cancelled;

    public ReactorProduceByproductEvent(@Nonnull Reactor reactor, @Nonnull Location location, @Nonnull ItemStack result) {
        Validate.notNull(reactor, "The Reactor must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(result, "The result must not be null");

        this.reactor = reactor;
        this.location = location;
        this.result = result;
    }

    /**
     * This returns the {@link Reactor} producing the byproduct.
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
     * @return The {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the byproduct {@link ItemStack} that is about to be produced. Use
     * {@link #setResult(ItemStack)} to replace it with a custom item.
     *
     * @return The byproduct {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This replaces the byproduct {@link ItemStack} that will be produced.
     *
     * @param result
     *            The new byproduct, not {@code null}
     */
    public void setResult(@Nullable ItemStack result) {
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

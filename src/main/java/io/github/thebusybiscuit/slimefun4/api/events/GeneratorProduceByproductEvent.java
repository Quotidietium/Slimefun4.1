package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;

/**
 * This {@link Event} is fired whenever an {@link AGenerator} (e.g. a Coal or Combustion Generator)
 * has finished burning a bucketed fuel and is about to push the emptied container (by default an
 * empty bucket) into the generator's output slot.
 * <p>
 * The byproduct {@link ItemStack} is modifiable via {@link #setResult(ItemStack)} - an addon may
 * replace the empty container with a custom item. Cancelling this event produces no byproduct at
 * all: the fuel is still consumed (the operation ended) but nothing is pushed to the output.
 *
 * @author Zurker
 *
 * @see AGenerator
 * @see ReactorProduceByproductEvent
 */
public class GeneratorProduceByproductEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AGenerator generator;
    private final Location location;
    private ItemStack result;

    private boolean cancelled;

    public GeneratorProduceByproductEvent(@Nonnull AGenerator generator, @Nonnull Location location, @Nonnull ItemStack result) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(generator, "The AGenerator must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(result, "The result must not be null");

        this.generator = generator;
        this.location = location;
        this.result = result;
    }

    /**
     * This returns the {@link AGenerator} producing the byproduct.
     *
     * @return The {@link AGenerator}
     */
    @Nonnull
    public AGenerator getGenerator() {
        return generator;
    }

    /**
     * This returns the {@link Location} of the {@link AGenerator}.
     *
     * @return The {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the byproduct {@link ItemStack} (by default an empty bucket) that is about to be
     * produced. Use {@link #setResult(ItemStack)} to replace it.
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

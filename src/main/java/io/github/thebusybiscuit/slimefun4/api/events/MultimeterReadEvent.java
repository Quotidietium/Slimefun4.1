package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.Multimeter;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a chargeable
 * {@link EnergyNetComponent} block with a {@link Multimeter}: the stored charge and the
 * capacity have been measured and are about to be displayed to the {@link Player}.
 * <p>
 * Cancelling this event skips the display entirely: no message is sent, allowing an addon
 * to present the readings through its own channel or to suppress the measurement.
 *
 * @author Zurker
 *
 * @see Multimeter
 * @see EnergyNetComponent
 */
public class MultimeterReadEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Multimeter multimeter;
    private final Location location;
    private final EnergyNetComponent component;
    private final int stored;
    private final int capacity;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public MultimeterReadEvent(Player player, Multimeter multimeter, Location location, EnergyNetComponent component, int stored, int capacity) {
        super(player);
        Validate.notNull(multimeter, "The Multimeter must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(component, "The EnergyNetComponent must not be null");
        Validate.isTrue(stored >= 0, "The stored charge must not be negative");
        Validate.isTrue(capacity > 0, "The capacity must be positive");

        this.multimeter = multimeter;
        this.location = location;
        this.component = component;
        this.stored = stored;
        this.capacity = capacity;
    }

    /**
     * This returns the {@link Multimeter} that was used.
     *
     * @return The {@link Multimeter}
     */
    @Nonnull
    public Multimeter getMultimeter() {
        return multimeter;
    }

    /**
     * This returns the {@link Location} of the measured {@link EnergyNetComponent} block.
     *
     * @return The measured {@link Location}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the measured {@link EnergyNetComponent}.
     *
     * @return The measured {@link EnergyNetComponent}
     */
    @Nonnull
    public EnergyNetComponent getComponent() {
        return component;
    }

    /**
     * This returns the charge currently stored at the measured {@link Location}.
     *
     * @return The stored charge
     */
    public int getStored() {
        return stored;
    }

    /**
     * This returns the capacity of the measured {@link EnergyNetComponent}.
     *
     * @return The capacity
     */
    public int getCapacity() {
        return capacity;
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

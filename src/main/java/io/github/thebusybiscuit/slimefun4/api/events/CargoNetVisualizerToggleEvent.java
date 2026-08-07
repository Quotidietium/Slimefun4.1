package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoManager;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a
 * {@link CargoManager} to toggle the cargo network visualizer and the new state is about
 * to be stored.
 * <p>
 * Cancelling this event vetoes the toggle: the stored visualizer state stays as it is and
 * no message is sent. The new state can also be forced via {@link #setEnabled(boolean)}.
 * <p>
 * The visualizer, when enabled, draws the cargo network connections as particles. The
 * event is fired before anything is written, synchronously, since the right-click happens
 * on the main thread.
 *
 * @author Zurker
 *
 * @see NetworkCreateEvent
 * @see CargoManager
 */
public class CargoNetVisualizerToggleEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final CargoManager cargoManager;
    private final Block block;
    private final boolean previousEnabled;
    private boolean enabled;

    private boolean cancelled;

    public CargoNetVisualizerToggleEvent(@Nonnull Player player, @Nonnull CargoManager cargoManager, @Nonnull Block block, boolean previousEnabled, boolean enabled) {
        super(player);
        Validate.notNull(cargoManager, "The CargoManager must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.cargoManager = cargoManager;
        this.block = block;
        this.previousEnabled = previousEnabled;
        this.enabled = enabled;
    }

    /**
     * This returns the {@link CargoManager} whose visualizer is being toggled.
     *
     * @return The {@link CargoManager}
     */
    @Nonnull
    public CargoManager getCargoManager() {
        return cargoManager;
    }

    /**
     * This returns the {@link Block} of the {@link CargoManager}.
     *
     * @return The {@link CargoManager} {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns whether the visualizer was enabled before this toggle.
     *
     * @return The previous visualizer state
     */
    public boolean wasPreviouslyEnabled() {
        return previousEnabled;
    }

    /**
     * This returns whether the visualizer will be enabled after this toggle.
     *
     * @return The new visualizer state
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * This sets whether the visualizer will be enabled, overriding the toggled state.
     *
     * @param enabled
     *            The new visualizer state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

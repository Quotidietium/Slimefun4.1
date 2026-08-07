package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoNode;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} clicks a distribution-mode
 * toggle of a {@link io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoInputNode}
 * and the node is about to store the changed mode.
 * <p>
 * Cancelling this event vetoes the change: the stored mode stays as it is and the menu
 * is not refreshed, as if the click had never happened. Which mode was toggled is told
 * by {@link #getReason()}, and the boolean value is whether that mode is enabled.
 * <p>
 * The two modes govern how items leaving the node are distributed across the channel:
 * {@link Reason#ROUND_ROBIN} spreads them evenly, {@link Reason#SMART_FILL} tries to keep
 * a constant amount in each inventory. The event is fired before anything is written,
 * synchronously, since menu clicks happen on the main thread.
 *
 * @author Zurker
 *
 * @see CargoNodeChannelChangeEvent
 * @see CargoNodeFilterChangeEvent
 * @see CargoNode
 */
public class CargoNodeDistributionModeEvent extends PlayerEvent implements Cancellable {

    /**
     * This enum describes which distribution mode was toggled.
     */
    public enum Reason {

        /**
         * The round-robin toggle. A value of {@code true} means items are distributed evenly.
         */
        ROUND_ROBIN,

        /**
         * The "smart-filling" toggle. A value of {@code true} means the node tries to keep a
         * constant amount of items in each inventory.
         */
        SMART_FILL
    }

    private static final HandlerList handlers = new HandlerList();

    private final CargoNode cargoNode;
    private final Block block;
    private final Reason reason;
    private final boolean previousValue;
    private boolean newValue;

    private boolean cancelled;

    public CargoNodeDistributionModeEvent(@Nonnull Player player, @Nonnull CargoNode cargoNode, @Nonnull Block block, @Nonnull Reason reason, boolean previousValue, boolean newValue) {
        super(player);
        Validate.notNull(cargoNode, "The CargoNode must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(reason, "The reason must not be null");

        this.cargoNode = cargoNode;
        this.block = block;
        this.reason = reason;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    /**
     * This returns the {@link CargoNode} whose mode is being changed.
     *
     * @return The {@link CargoNode}
     */
    @Nonnull
    public CargoNode getCargoNode() {
        return cargoNode;
    }

    /**
     * This returns the {@link Block} of the {@link CargoNode}.
     *
     * @return The node {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns which distribution mode was toggled.
     *
     * @return The {@link Reason}
     */
    @Nonnull
    public Reason getReason() {
        return reason;
    }

    /**
     * This returns whether the mode was enabled before this change.
     *
     * @return The previous mode value
     */
    public boolean getPreviousValue() {
        return previousValue;
    }

    /**
     * This returns whether the mode will be enabled after this change.
     *
     * @return The new mode value
     */
    public boolean getNewValue() {
        return newValue;
    }

    /**
     * This sets whether the mode will be enabled, overriding the toggled value.
     *
     * @param newValue
     *            The new mode value
     */
    public void setNewValue(boolean newValue) {
        this.newValue = newValue;
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

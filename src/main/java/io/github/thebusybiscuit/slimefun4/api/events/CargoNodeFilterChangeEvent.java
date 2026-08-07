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
 * This {@link PlayerEvent} is fired whenever a {@link Player} clicks the filter settings of
 * a filter-bearing {@link CargoNode} (the input and advanced output nodes) and the node is
 * about to store the changed setting.
 * <p>
 * Cancelling this event vetoes the change: the stored setting stays as it is and the menu
 * is not refreshed, as if the click had never happened. Which setting was toggled is told
 * by {@link #getReason()}, and the boolean meaning depends on it: for
 * {@link Reason#FILTER_TYPE} the value is whether the node is in whitelist mode, for
 * {@link Reason#LORE_MATCHING} it is whether the item lore must match.
 * <p>
 * The event is fired before anything is written, synchronously, since menu clicks happen
 * on the main thread.
 *
 * @author Zurker
 *
 * @see CargoNodeChannelChangeEvent
 * @see CargoNode
 */
public class CargoNodeFilterChangeEvent extends PlayerEvent implements Cancellable {

    /**
     * This enum describes which filter setting was toggled.
     */
    public enum Reason {

        /**
         * The whitelist/blacklist toggle. A value of {@code true} means whitelist mode.
         */
        FILTER_TYPE,

        /**
         * The "include lore" toggle. A value of {@code true} means the item lore must match.
         */
        LORE_MATCHING
    }

    private static final HandlerList handlers = new HandlerList();

    private final CargoNode cargoNode;
    private final Block block;
    private final Reason reason;
    private final boolean previousValue;
    private boolean newValue;

    private boolean cancelled;

    public CargoNodeFilterChangeEvent(@Nonnull Player player, @Nonnull CargoNode cargoNode, @Nonnull Block block, @Nonnull Reason reason, boolean previousValue, boolean newValue) {
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
     * This returns the {@link CargoNode} whose setting is being changed.
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
     * This returns which filter setting was toggled.
     *
     * @return The {@link Reason}
     */
    @Nonnull
    public Reason getReason() {
        return reason;
    }

    /**
     * This returns the setting value before this change. Its boolean meaning depends on
     * {@link #getReason()}.
     *
     * @return The previous setting value
     */
    public boolean getPreviousValue() {
        return previousValue;
    }

    /**
     * This returns the setting value that will be stored after this change. Its boolean
     * meaning depends on {@link #getReason()}.
     *
     * @return The new setting value
     */
    public boolean getNewValue() {
        return newValue;
    }

    /**
     * This sets the setting value that will be stored, overriding the toggled value.
     *
     * @param newValue
     *            The new setting value
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

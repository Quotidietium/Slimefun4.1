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
 * This {@link PlayerEvent} is fired whenever a {@link Player} clicks the channel
 * selector of a channel-bearing {@link CargoNode} (the input, output and advanced
 * output nodes) and the node is about to store the newly selected channel.
 * <p>
 * Cancelling this event vetoes the change: the stored frequency stays untouched and
 * the menu is not refreshed, as if the click had never happened.
 * <p>
 * Addons may also redirect the change via {@link #setNewChannel(int)}, e.g. to skip
 * channels that are locked for this {@link Player}; the redirected channel is what
 * gets stored.
 * <p>
 * Channels are zero-based here (the menu displays them one-based) and wrap around:
 * decreasing channel 0 selects 15, increasing channel 15 selects 0. The previous
 * channel may be 16, the special "chest terminal" display state. The event is fired
 * before anything is written, synchronously, since menu clicks happen on the main
 * thread. The connector node has no channel selector and never fires this event.
 *
 * @author Zurker
 *
 * @see CargoNodeRestrictionEvent
 * @see CargoItemInsertEvent
 * @see CargoItemWithdrawEvent
 * @see CargoNode
 */
public class CargoNodeChannelChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final CargoNode cargoNode;
    private final Block block;
    private final int previousChannel;

    private int newChannel;
    private boolean cancelled;

    public CargoNodeChannelChangeEvent(@Nonnull Player player, @Nonnull CargoNode cargoNode, @Nonnull Block block, int previousChannel, int newChannel) {
        super(player);
        Validate.notNull(cargoNode, "The CargoNode must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.isTrue(previousChannel >= 0 && previousChannel <= 16, "The previous channel must be between 0 and 16");
        Validate.isTrue(newChannel >= 0 && newChannel <= 15, "The new channel must be between 0 and 15");

        this.cargoNode = cargoNode;
        this.block = block;
        this.previousChannel = previousChannel;
        this.newChannel = newChannel;
    }

    /**
     * This returns the {@link CargoNode} whose channel is being changed.
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
     * This returns the channel that was selected before this change, zero-based.
     *
     * @return The previously selected channel
     */
    public int getPreviousChannel() {
        return previousChannel;
    }

    /**
     * This returns the channel that will be stored after this change, zero-based.
     *
     * @return The newly selected channel
     */
    public int getNewChannel() {
        return newChannel;
    }

    /**
     * This sets the channel that will be stored after this change, zero-based,
     * overriding the channel the {@link Player} picked. The wrap-around arithmetic
     * of the click handler is not re-applied: the value given here is stored as-is.
     *
     * @param newChannel
     *            The channel to store, between 0 and 15
     */
    public void setNewChannel(int newChannel) {
        Validate.isTrue(newChannel >= 0 && newChannel <= 15, "The new channel must be between 0 and 15");

        this.newChannel = newChannel;
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

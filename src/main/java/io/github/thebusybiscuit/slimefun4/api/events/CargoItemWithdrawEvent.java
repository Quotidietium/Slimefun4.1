package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;

/**
 * This {@link Event} is fired on the main thread whenever a {@link CargoNet} input node
 * has withdrawn an {@link ItemStack} from its attached container and is about to
 * distribute it to the output nodes.
 * <p>
 * Cancelling this event returns the {@link ItemStack} to its source container
 * (or drops it above the container if the source slot was occupied in the meantime,
 * mirroring the regular "could not distribute" fallback) and skips the distribution
 * for this tick.
 * <p>
 * The withdrawn item can be replaced via {@link #setItem(ItemStack)} before distribution
 * begins, allowing addons to transform items in transit (e.g., rename, enchant, or
 * replace entirely).
 *
 * @author Zurker
 *
 * @see CargoItemInsertEvent
 */
public class CargoItemWithdrawEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final CargoNet network;
    private final Location inputNode;
    private final Block inputTarget;
    private final ItemStack item;
    private ItemStack modifiedItem;
    private final int previousSlot;

    private boolean cancelled;

    public CargoItemWithdrawEvent(@Nonnull CargoNet network, @Nonnull Location inputNode, @Nonnull Block inputTarget, @Nonnull ItemStack item, int previousSlot) {
        super(!Bukkit.isPrimaryThread());

        this.network = network;
        this.inputNode = inputNode;
        this.inputTarget = inputTarget;
        this.item = item;
        this.modifiedItem = item;
        this.previousSlot = previousSlot;
    }

    /**
     * This returns the {@link CargoNet} performing the transfer.
     *
     * @return The {@link CargoNet}
     */
    @Nonnull
    public CargoNet getNetwork() {
        return network;
    }

    /**
     * This returns the {@link Location} of the input node that withdrew the item.
     *
     * @return The {@link Location} of the input node
     */
    @Nonnull
    public Location getInputNode() {
        return inputNode;
    }

    /**
     * This returns the container {@link Block} the item was withdrawn from.
     *
     * @return The source container {@link Block}
     */
    @Nonnull
    public Block getInputTarget() {
        return inputTarget;
    }

    /**
     * This returns the withdrawn {@link ItemStack} that is about to be distributed.
     * If {@link #setItem(ItemStack)} was called, returns the replacement.
     *
     * @return The {@link ItemStack} that will be distributed
     */
    @Nonnull
    public ItemStack getItem() {
        return modifiedItem;
    }

    /**
     * This returns the original {@link ItemStack} as it was withdrawn from the source
     * container, before any listener modification.
     *
     * @return The original withdrawn {@link ItemStack}
     */
    @Nonnull
    public ItemStack getOriginalItem() {
        return item;
    }

    /**
     * This sets the {@link ItemStack} that will be distributed to the output nodes,
     * overriding the originally withdrawn item. The replacement is distributed as-is;
     * the source container's slot was already emptied.
     *
     * @param item
     *            The replacement {@link ItemStack}, must not be null
     */
    public void setItem(@Nonnull ItemStack item) {
        java.util.Objects.requireNonNull(item, "The item must not be null");
        this.modifiedItem = item;
    }

    /**
     * This returns the slot of the source container the item was withdrawn from.
     *
     * @return The source slot index
     */
    public int getPreviousSlot() {
        return previousSlot;
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

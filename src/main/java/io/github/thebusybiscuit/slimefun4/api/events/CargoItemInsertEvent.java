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
 * This {@link Event} is fired on the main thread whenever a {@link CargoNet} is about
 * to insert an {@link ItemStack} into the container attached to an output node.
 * <p>
 * Cancelling this event skips this output node; the {@link ItemStack} stays in transit
 * and the {@link CargoNet} tries the next output node (the same semantics as when a
 * protection plugin denies access to the destination container).
 *
 * @author Zurker
 *
 * @see CargoItemWithdrawEvent
 */
public class CargoItemInsertEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final CargoNet network;
    private final Location inputNode;
    private final Location outputNode;
    private final Block outputTarget;
    private final ItemStack item;

    private boolean cancelled;

    public CargoItemInsertEvent(@Nonnull CargoNet network, @Nonnull Location inputNode, @Nonnull Location outputNode, @Nonnull Block outputTarget, @Nonnull ItemStack item) {
        super(!Bukkit.isPrimaryThread());

        this.network = network;
        this.inputNode = inputNode;
        this.outputNode = outputNode;
        this.outputTarget = outputTarget;
        this.item = item;
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
     * This returns the {@link Location} of the input node the item originated from.
     *
     * @return The {@link Location} of the input node
     */
    @Nonnull
    public Location getInputNode() {
        return inputNode;
    }

    /**
     * This returns the {@link Location} of the output node the item is about to be inserted through.
     *
     * @return The {@link Location} of the output node
     */
    @Nonnull
    public Location getOutputNode() {
        return outputNode;
    }

    /**
     * This returns the container {@link Block} the item is about to be inserted into.
     *
     * @return The destination container {@link Block}
     */
    @Nonnull
    public Block getOutputTarget() {
        return outputTarget;
    }

    /**
     * This returns the {@link ItemStack} that is about to be inserted.
     *
     * @return The {@link ItemStack} in transit
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
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

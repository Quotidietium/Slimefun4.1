package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks an {@link AncientPedestal}
 * with an item in hand and the pedestal is about to accept it as a ritual ingredient: the hand item
 * is about to be consumed and a display copy placed on top of the pedestal.
 * <p>
 * Cancelling this event skips the placement entirely: the item stays in the player's hand and
 * nothing is placed on the pedestal.
 * <p>
 * Addons may also change which {@link ItemStack} is displayed on the pedestal via
 * {@link #setItem(ItemStack)}, e.g. to show a ritual ingredient in an activated form.
 * The hand item of the {@link Player} is consumed regardless; only the displayed copy
 * (and its nametag) follows the changed item.
 *
 * @author Zurker
 *
 * @see AncientPedestal
 */
public class PedestalItemPlaceEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AncientPedestal pedestal;
    private final Block block;

    private ItemStack item;
    private boolean cancelled;

    public PedestalItemPlaceEvent(@Nonnull Player player, @Nonnull AncientPedestal pedestal, @Nonnull Block block, @Nonnull ItemStack item) {
        super(player);
        Validate.notNull(pedestal, "The AncientPedestal must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(item, "The item must not be null");

        this.pedestal = pedestal;
        this.block = block;
        this.item = item;
    }

    /**
     * This returns the {@link AncientPedestal} the item is being placed on.
     *
     * @return The {@link AncientPedestal}
     */
    @Nonnull
    public AncientPedestal getPedestal() {
        return pedestal;
    }

    /**
     * This returns the {@link Block} of the {@link AncientPedestal}.
     *
     * @return The {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link ItemStack} that is about to be placed.
     *
     * @return The {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This sets the {@link ItemStack} that will be displayed on the pedestal. The hand
     * item of the {@link Player} is still consumed; only the displayed copy and its
     * nametag follow the given item.
     * <p>
     * Setting an air or otherwise empty {@link ItemStack} destroys the item (consistent
     * with the cargo withdraw/insert events): the hand item is consumed and nothing is
     * displayed on the pedestal.
     *
     * @param item
     *            The {@link ItemStack} to display instead
     */
    public void setItem(@Nonnull ItemStack item) {
        Validate.notNull(item, "The item must not be null");

        this.item = item;
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

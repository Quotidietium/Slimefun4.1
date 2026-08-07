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
 * that holds a ritual ingredient: the display item is about to be removed from the pedestal and
 * returned to the {@link Player}'s inventory (or dropped, should it be full).
 * <p>
 * Cancelling this event skips the retrieval entirely: the item stays on the pedestal and nothing
 * is returned to the {@link Player}.
 *
 * @author Zurker
 *
 * @see AncientPedestal
 * @see PedestalItemPlaceEvent
 */
public class PedestalItemTakeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AncientPedestal pedestal;
    private final Block block;
    private final ItemStack item;

    private boolean cancelled;

    public PedestalItemTakeEvent(@Nonnull Player player, @Nonnull AncientPedestal pedestal, @Nonnull Block block, @Nonnull ItemStack item) {
        super(player);
        Validate.notNull(pedestal, "The AncientPedestal must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(item, "The item must not be null");

        this.pedestal = pedestal;
        this.block = block;
        this.item = item;
    }

    /**
     * This returns the {@link AncientPedestal} the item is being taken from.
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
     * This returns the {@link ItemStack} that is about to be returned to the {@link Player}.
     *
     * @return The {@link ItemStack}
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

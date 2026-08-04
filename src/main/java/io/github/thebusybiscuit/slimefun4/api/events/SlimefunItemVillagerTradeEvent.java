package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link PlayerEvent} is fired whenever a {@link SlimefunItem} is about to be
 * blocked from being traded with a villager, either by clicking it into the merchant
 * inventory or by dragging it across the merchant slots.
 * <p>
 * By default the interaction is denied: the item cannot be offered for a trade.
 * Cancelling this event allows the {@link SlimefunItem} to be traded instead.
 *
 * @author Zurker
 *
 * @see SlimefunItem
 */
public class SlimefunItemVillagerTradeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final ItemStack itemStack;
    private final InventoryEvent inventoryEvent;

    private boolean cancelled;

    public SlimefunItemVillagerTradeEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack itemStack, @Nonnull InventoryEvent inventoryEvent) {
        super(player);
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(itemStack, "The ItemStack must not be null");
        Validate.notNull(inventoryEvent, "The InventoryEvent must not be null");

        this.slimefunItem = slimefunItem;
        this.itemStack = itemStack;
        this.inventoryEvent = inventoryEvent;
    }

    /**
     * This returns the {@link SlimefunItem} that is about to be blocked from trading.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the actual {@link ItemStack} the {@link Player} tried to trade with.
     *
     * @return The traded {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * This returns the underlying {@link InventoryEvent}, either an
     * {@link org.bukkit.event.inventory.InventoryClickEvent} or an
     * {@link org.bukkit.event.inventory.InventoryDragEvent}.
     *
     * @return The underlying {@link InventoryEvent}
     */
    @Nonnull
    public InventoryEvent getInventoryEvent() {
        return inventoryEvent;
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

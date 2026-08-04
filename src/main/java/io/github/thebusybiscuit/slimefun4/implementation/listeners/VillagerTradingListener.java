package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemVillagerTradeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.VanillaItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.misc.SyntheticEmerald;

/**
 * This {@link Listener} prevents any {@link SlimefunItem} from being used to trade with
 * Villagers, with one exception being {@link SyntheticEmerald}.
 * 
 * @author TheBusyBiscuit
 *
 */
public class VillagerTradingListener implements Listener {

    public VillagerTradingListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreTrade(InventoryClickEvent e) {
        Inventory clickedInventory = e.getClickedInventory();
        Inventory topInventory = e.getView().getTopInventory();

        if (clickedInventory != null && topInventory.getType() == InventoryType.MERCHANT) {
            if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
                e.setCancelled(true);
                return;
            }

            SlimefunItem sfItem;
            ItemStack itemStack;

            if (clickedInventory.getType() == InventoryType.MERCHANT) {
                sfItem = SlimefunItem.getByItem(e.getCursor());
                itemStack = e.getCursor();
            } else {
                sfItem = SlimefunItem.getByItem(e.getCurrentItem());
                itemStack = e.getCurrentItem();
            }

            if (isUnallowed(sfItem)) {
                SlimefunItemVillagerTradeEvent event = new SlimefunItemVillagerTradeEvent((Player) e.getWhoClicked(), sfItem, itemStack, e);
                Bukkit.getPluginManager().callEvent(event);

                // A cancelled event allows the item to be traded instead
                e.setCancelled(!event.isCancelled());
            } else {
                e.setCancelled(false);
            }

            if (e.getResult() == Result.DENY) {
                Slimefun.getLocalization().sendMessage((Player) e.getWhoClicked(), "villagers.no-trading", true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory topInventory = e.getView().getTopInventory();

        if (topInventory.getType() == InventoryType.MERCHANT) {
            int topInventorySize = topInventory.getSize();
            SlimefunItem sfItem = SlimefunItem.getByItem(e.getOldCursor());

            if (isUnallowed(sfItem)) {
                for (int rawSlot : e.getRawSlots()) {
                    if (rawSlot < topInventorySize) {
                        SlimefunItemVillagerTradeEvent event = new SlimefunItemVillagerTradeEvent((Player) e.getWhoClicked(), sfItem, e.getOldCursor(), e);
                        Bukkit.getPluginManager().callEvent(event);

                        if (!event.isCancelled()) {
                            // Dragging is not an InventoryClickEvent, validate the dragged item separately
                            e.setCancelled(true);
                            Slimefun.getLocalization().sendMessage((Player) e.getWhoClicked(), "villagers.no-trading", true);
                        }

                        return;
                    }
                }
            }
        }
    }

    private boolean isUnallowed(@Nullable SlimefunItem item) {
        return item != null && !(item instanceof VanillaItem) && !(item instanceof SyntheticEmerald) && !item.isDisabled();
    }
}

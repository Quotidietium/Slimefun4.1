package io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemWorkstationEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This {@link Listener} prevents any {@link SlimefunItem} from being used in a
 * brewing stand.
 * 
 * @author VoidAngel
 * @author SoSeDiK
 * @author CURVX
 *
 */
public class BrewingStandListener implements SlimefunCraftingListener {

    public BrewingStandListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreBrew(InventoryClickEvent e) {
        Inventory clickedInventory = e.getClickedInventory();
        Inventory topInventory = e.getView().getTopInventory();

        if (clickedInventory != null && topInventory.getType() == InventoryType.BREWING && topInventory.getHolder() instanceof BrewingStand) {
            if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
                e.setCancelled(true);
                return;
            }

            if (clickedInventory.getType() == InventoryType.BREWING) {
                SlimefunItem sfItem = SlimefunItem.getByItem(e.getCursor());

                if (isUnallowed(sfItem)) {
                    SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent((Player) e.getWhoClicked(), sfItem, e.getCursor(), SlimefunItemWorkstationEvent.Workstation.BREWING_STAND);
                    Bukkit.getPluginManager().callEvent(event);
                    e.setCancelled(!event.isCancelled());
                } else {
                    e.setCancelled(false);
                }
            } else {
                SlimefunItem sfItem = SlimefunItem.getByItem(e.getCurrentItem());

                if (isUnallowed(sfItem)) {
                    SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent((Player) e.getWhoClicked(), sfItem, e.getCurrentItem(), SlimefunItemWorkstationEvent.Workstation.BREWING_STAND);
                    Bukkit.getPluginManager().callEvent(event);
                    e.setCancelled(!event.isCancelled());
                } else {
                    e.setCancelled(false);
                }
            }

            if (e.getResult() == Result.DENY) {
                Slimefun.getLocalization().sendMessage((Player) e.getWhoClicked(), "brewing_stand.not-working", true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        Inventory topInventory = e.getView().getTopInventory();

        if (topInventory.getType() == InventoryType.BREWING && topInventory.getHolder() instanceof BrewingStand) {
            int topInventorySize = topInventory.getSize();
            SlimefunItem sfItem = SlimefunItem.getByItem(e.getOldCursor());

            if (isUnallowed(sfItem)) {
                for (int rawSlot : e.getRawSlots()) {
                    if (rawSlot < topInventorySize) {
                        SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent((Player) e.getWhoClicked(), sfItem, e.getOldCursor(), SlimefunItemWorkstationEvent.Workstation.BREWING_STAND);
                        Bukkit.getPluginManager().callEvent(event);

                        if (!event.isCancelled()) {
                            // Dragging is not an InventoryClickEvent, validate the dragged item separately
                            e.setCancelled(true);
                            Slimefun.getLocalization().sendMessage((Player) e.getWhoClicked(), "brewing_stand.not-working", true);
                        }

                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void hopperOnBrew(InventoryMoveItemEvent e) {
        if (e.getDestination().getType() == InventoryType.BREWING && isUnallowed(e.getItem())) {
            e.setCancelled(true);
        }
    }
}

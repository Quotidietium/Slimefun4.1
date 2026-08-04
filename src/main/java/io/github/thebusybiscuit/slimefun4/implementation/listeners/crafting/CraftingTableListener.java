package io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemWorkstationEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This {@link Listener} prevents any {@link SlimefunItem} from being used in a
 * crafting table.
 * 
 * @author TheBusyBiscuit
 *
 */
public class CraftingTableListener implements SlimefunCraftingListener {

    public CraftingTableListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        for (ItemStack item : e.getInventory().getContents()) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem != null && !sfItem.isUseableInWorkbench()) {
                SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent((Player) e.getWhoClicked(), sfItem, item, SlimefunItemWorkstationEvent.Workstation.CRAFTING_TABLE);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    // Allowed by an addon, keep scanning the remaining grid slots
                    continue;
                }

                e.setResult(Result.DENY);
                Slimefun.getLocalization().sendMessage((Player) e.getWhoClicked(), "workbench.not-enhanced", true);
                break;
            }
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (e.getInventory().getResult() != null) {
            for (ItemStack item : e.getInventory().getContents()) {
                SlimefunItem sfItem = SlimefunItem.getByItem(item);

                if (sfItem != null && !sfItem.isUseableInWorkbench()) {
                    SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent((Player) e.getView().getPlayer(), sfItem, item, SlimefunItemWorkstationEvent.Workstation.CRAFTING_TABLE);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        continue;
                    }

                    e.getInventory().setResult(null);
                    break;
                }
            }
        }
    }

}

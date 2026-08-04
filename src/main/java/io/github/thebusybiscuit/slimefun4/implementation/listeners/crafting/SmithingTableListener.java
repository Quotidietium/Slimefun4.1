package io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemWorkstationEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This {@link Listener} prevents any {@link SlimefunItem} from being used in a
 * smithing table.
 * 
 * @author Sefiraat
 * @author iTwins
 */
public class SmithingTableListener implements SlimefunCraftingListener {

    public SmithingTableListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmith(SmithItemEvent e) {
        ItemStack material = e.getInventory().getContents()[materialSlot()];
        SlimefunItem sfItem = SlimefunItem.getByItem(material);
        if (sfItem != null && !sfItem.isUseableInWorkbench()) {
            SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent((Player) e.getWhoClicked(), sfItem, material, SlimefunItemWorkstationEvent.Workstation.SMITHING_TABLE);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            e.setResult(Result.DENY);
            Slimefun.getLocalization().sendMessage(e.getWhoClicked(), "smithing_table.not-working", true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareSmith(PrepareSmithingEvent e) {
        if (e.getInventory().getResult() != null) {
            ItemStack material = e.getInventory().getContents()[materialSlot()];
            SlimefunItem sfItem = SlimefunItem.getByItem(material);
            if (sfItem != null && !sfItem.isUseableInWorkbench()) {
                SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent((Player) e.getView().getPlayer(), sfItem, material, SlimefunItemWorkstationEvent.Workstation.SMITHING_TABLE);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                e.setResult(null);
            }
        }
    }

    private int materialSlot() {
        if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_20)) {
            return 2;
        }
        return 1;
    }

}

package io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemCrafterPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This {@link Listener} prevents any {@link SlimefunItem} from being used as an
 * ingredient in a Crafter. The Crafter crafts on its own without any Player
 * interaction, so the click-based protection of the other crafting listeners
 * cannot apply here.
 * <p>
 * The {@link CrafterCraftEvent} only exists on Minecraft 1.21 and above, this
 * {@link Listener} must only be registered there.
 *
 * @author The Slimefun 4.1 Team
 */
public class CrafterListener implements SlimefunCraftingListener {

    public CrafterListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent e) {
        if (e.getBlock().getState() instanceof Crafter crafter) {
            for (ItemStack item : crafter.getInventory().getContents()) {
                if (isUnallowed(item)) {
                    if (SlimefunItemCrafterPreventEvent.getHandlerList().getRegisteredListeners().length > 0) {
                        SlimefunItemCrafterPreventEvent event = new SlimefunItemCrafterPreventEvent(SlimefunItem.getByItem(item), item, e.getBlock());
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            // An addon vetoed the protection; the craft proceeds with the ingredient.
                            return;
                        }
                    }

                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

}

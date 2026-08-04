package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.events.ItemPickupPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.events.ItemPickupPreventEvent.PreventReason;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * Listens to the ItemPickup events to prevent it if the item has the "no_pickup" metadata or is an ALTAR_PROBE.
 *
 * @author TheBusyBiscuit
 */
public class ItemPickupListener implements Listener {

    public ItemPickupListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent e) {
        handlePickup(e, e.getItem());
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent e) {
        handlePickup(e, e.getItem());
    }

    @ParametersAreNonnullByDefault
    private void handlePickup(Cancellable e, Item item) {
        if (SlimefunUtils.hasNoPickupFlag(item)) {
            if (!isPreventionVetoed(item, PreventReason.NO_PICKUP_FLAG)) {
                e.setCancelled(true);
            }
        } else if (item.getItemStack().hasItemMeta()) {
            ItemMeta meta = item.getItemStack().getItemMeta();

            if (meta.hasDisplayName() && meta.getDisplayName().startsWith(AncientPedestal.ITEM_PREFIX)) {
                if (!isPreventionVetoed(item, PreventReason.ALTAR_PROBE)) {
                    e.setCancelled(true);
                    item.remove();
                }
            }
        }
    }

    /**
     * Fires an {@link ItemPickupPreventEvent} if any listener is registered and returns
     * whether the prevention was vetoed. Without listeners this costs nothing and the
     * old behavior is preserved.
     */
    @ParametersAreNonnullByDefault
    private boolean isPreventionVetoed(Item item, PreventReason reason) {
        if (ItemPickupPreventEvent.getHandlerList().getRegisteredListeners().length > 0) {
            ItemPickupPreventEvent event = new ItemPickupPreventEvent(item, reason);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }
}

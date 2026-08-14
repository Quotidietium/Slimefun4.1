package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

import io.github.thebusybiscuit.slimefun4.api.events.HopperTransferPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotHopperable;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * This {@link Listener} prevents item from being transferred to
 * and from {@link AContainer} using a hopper.
 *
 * @author CURVX
 *
 * @see NotHopperable
 * @see HopperTransferPreventEvent
 *
 */
public class HopperListener implements Listener {

    public HopperListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onHopperInsert(InventoryMoveItemEvent e) {
        Location loc = e.getDestination().getLocation();

        // Null-check BEFORE BlockStorage.check(loc): Inventory#getLocation() is @Nullable
        // (other plugins can also fire synthetic InventoryMoveItemEvents with virtual
        // inventories), and getLocationInfo would NPE on a null Location. The previous
        // ordering called check() first, making the null-check dead code.
        if (loc == null) {
            return;
        }

        SlimefunItem item = BlockStorage.check(loc);

        if (e.getSource().getType() == InventoryType.HOPPER && item instanceof NotHopperable) {
            if (isPreventionVetoed(item, e)) {
                return;
            }

            e.setCancelled(true);
        }
    }

    /**
     * Fires a {@link HopperTransferPreventEvent} if any listener is registered and returns
     * whether the hopper prevention was vetoed. Without listeners this costs nothing and
     * the old behavior is preserved.
     */
    private boolean isPreventionVetoed(@Nonnull SlimefunItem slimefunItem, @Nonnull InventoryMoveItemEvent e) {
        if (HopperTransferPreventEvent.getHandlerList().getRegisteredListeners().length > 0) {
            HopperTransferPreventEvent event = new HopperTransferPreventEvent(slimefunItem, e.getSource(), e.getDestination(), e.getItem(), e);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeRestrictionEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoNode;

/**
 * This {@link Listener} is solely responsible for preventing Cargo Nodes from being placed
 * on the top or bottom of a block.
 *
 * @author TheBusyBiscuit
 *
 * @see CargoNode
 * @see CargoNodeRestrictionEvent
 *
 */
public class CargoNodeListener implements Listener {

    public CargoNodeListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCargoNodePlace(BlockPlaceEvent e) {
        Block b = e.getBlock();
        SlimefunItem sfItem = SlimefunItem.getByItem(e.getItemInHand());

        if ((b.getY() != e.getBlockAgainst().getY() || !e.getBlockReplacedState().getType().isAir()) && sfItem instanceof CargoNode cargoNode) {
            if (isRestrictionVetoed(e, cargoNode, b)) {
                return;
            }

            Slimefun.getLocalization().sendMessage(e.getPlayer(), "machines.CARGO_NODES.must-be-placed", true);
            e.setCancelled(true);
        }
    }

    /**
     * Fires a {@link CargoNodeRestrictionEvent} if any listener is registered and returns
     * whether the placement restriction was vetoed. Without listeners this costs nothing
     * and the old behavior is preserved.
     */
    private boolean isRestrictionVetoed(@Nonnull BlockPlaceEvent e, @Nonnull CargoNode cargoNode, @Nonnull Block block) {
        if (CargoNodeRestrictionEvent.getHandlerList().getRegisteredListeners().length > 0) {
            CargoNodeRestrictionEvent event = new CargoNodeRestrictionEvent(e.getPlayer(), cargoNode, block, e.getBlockAgainst(), e);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }
}

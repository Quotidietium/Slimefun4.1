package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.SoulboundItemsKeepEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SoulboundItemsReturnEvent;
import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * This {@link Listener} is responsible for handling any {@link Soulbound} items.
 * A {@link Soulbound} {@link ItemStack} will not drop upon a {@link Player Player's} death.
 * Instead the {@link ItemStack} is saved and given back to the {@link Player} when they respawn.
 * 
 * @author TheBusyBiscuit
 *
 */
public class SoulboundListener implements Listener {

    /**
     * Pseudo-slot used to store the item the {@link Player} held on their cursor
     * when they died. The cursor is not part of the regular inventory slots.
     */
    private static final int CURSOR_SLOT = Integer.MIN_VALUE;

    private final Map<UUID, Map<Integer, ItemStack>> soulbound = new HashMap<>();

    public SoulboundListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDamage(PlayerDeathEvent e) {
        if (e.getKeepInventory()) {
            /*
             * The inventory is kept on death, so nothing must be stored away and
             * returned on respawn - doing so would duplicate the items (most
             * notably the cursor item, which is re-added with addItem()).
             */
            return;
        }

        Map<Integer, ItemStack> items = new HashMap<>();
        Player p = e.getEntity();

        /*
         * Fire a SoulboundItemsKeepEvent before anything is stored or stripped.
         * Cancellation disables the soulbound behavior for this death entirely:
         * every item drops normally and nothing is returned on respawn.
         */
        SoulboundItemsKeepEvent keepEvent = new SoulboundItemsKeepEvent(p, e);
        Bukkit.getPluginManager().callEvent(keepEvent);

        if (keepEvent.isCancelled()) {
            return;
        }

        for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
            ItemStack item = p.getInventory().getItem(slot);

            // Store soulbound items for later retrieval
            if (SlimefunUtils.isSoulbound(item, p.getWorld())) {
                items.put(slot, item);
            }
        }

        /*
         * The item on the cursor is part of the death drops but not part of the
         * inventory slots iterated above. Without this, a soulbound item held on
         * the cursor would be removed from the drops below and simply vanish.
         */
        ItemStack cursor = p.getItemOnCursor();

        if (SlimefunUtils.isSoulbound(cursor, p.getWorld())) {
            items.put(CURSOR_SLOT, cursor.clone());
        }

        // There shouldn't even be any items in there, but let's be extra safe!
        Map<Integer, ItemStack> existingItems = soulbound.get(p.getUniqueId());

        if (existingItems == null) {
            soulbound.put(p.getUniqueId(), items);
        } else {
            existingItems.putAll(items);
        }

        // Remove soulbound items from our drops
        e.getDrops().removeIf(itemStack -> SlimefunUtils.isSoulbound(itemStack, p.getWorld()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        returnSoulboundItems(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        /*
         * If the player died and disconnected before respawning, PlayerRespawnEvent never fired.
         * Return the soulbound items to their inventory now so they are saved with the player's
         * data instead of being stuck in memory (and lost if the entry is never drained). The
         * map is drained by remove(), so a later respawn after reconnecting is a no-op and
         * cannot duplicate anything.
         */
        returnSoulboundItems(e.getPlayer());
    }

    private void returnSoulboundItems(@Nonnull Player p) {
        Map<Integer, ItemStack> items = soulbound.remove(p.getUniqueId());

        if (items != null) {
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                if (entry.getKey() == CURSOR_SLOT) {
                    // The cursor slot does not exist on respawn, add it to the inventory
                    p.getInventory().addItem(entry.getValue());
                } else {
                    p.getInventory().setItem(entry.getKey(), entry.getValue());
                }
            }

            // Notify addons that the stored soulbound items were returned
            Bukkit.getPluginManager().callEvent(new SoulboundItemsReturnEvent(p, items));
        }
    }
}

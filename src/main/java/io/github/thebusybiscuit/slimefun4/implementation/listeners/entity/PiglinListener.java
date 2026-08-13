package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.PiglinBarterDropEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PiglinBarterPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.PiglinBarterDrop;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This {@link Listener} prevents a {@link Piglin} from bartering with a
 * {@link SlimefunItem}.
 * It also listens to the {@link EntityDropItemEvent} to
 * inject a {@link PiglinBarterDrop} if the chance check passes.
 *
 * @author poma123
 * @author dNiym
 * 
 */
public class PiglinListener implements Listener {

    /**
     * Tracks which misconfigured barter items have already been warned about, so a piglin
     * barter farm (which fires {@link EntityDropItemEvent} frequently) does not flood the log
     * with the same "chance must be between 1-99%" warning on every single barter.
     */
    private static final Set<String> warnedInvalidBarterChances = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public PiglinListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntityType() == EntityType.PIGLIN) {
            ItemStack item = e.getItem().getItemStack();
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            // Don't let Piglins pick up gold from Slimefun
            if (sfItem != null && !isPreventionVetoed(sfItem, (Piglin) e.getEntity(), item, null, PiglinBarterPreventEvent.Reason.PICKUP)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!e.getRightClicked().isValid() || e.getRightClicked().getType() != EntityType.PIGLIN) {
            return;
        }

        Player p = e.getPlayer();
        ItemStack item;

        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            item = p.getInventory().getItemInOffHand();
        } else {
            item = p.getInventory().getItemInMainHand();
        }

        // We only care about Gold since it's the actual "Bartering" we wanna prevent
        if (item.getType() == Material.GOLD_INGOT) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem != null && !isPreventionVetoed(sfItem, (Piglin) e.getRightClicked(), item, p, PiglinBarterPreventEvent.Reason.BARTER)) {
                Slimefun.getLocalization().sendMessage(p, "messages.piglin-barter", true);
                e.setCancelled(true);
            }
        }
    }

    /**
     * Fires a {@link PiglinBarterPreventEvent} if any listener is registered and returns
     * whether the prevention was vetoed. Without listeners this costs nothing and the
     * old behavior is preserved.
     */
    @ParametersAreNonnullByDefault
    private boolean isPreventionVetoed(SlimefunItem slimefunItem, Piglin piglin, ItemStack item, Player player, PiglinBarterPreventEvent.Reason reason) {
        if (PiglinBarterPreventEvent.getHandlerList().getRegisteredListeners().length > 0) {
            PiglinBarterPreventEvent event = new PiglinBarterPreventEvent(slimefunItem, piglin, item, player, reason);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }

    @EventHandler
    public void onPiglinDropItem(EntityDropItemEvent e) {
        if (e.getEntity() instanceof Piglin) {
            Set<ItemStack> drops = Slimefun.getRegistry().getBarteringDrops();

            /*
             * NOTE: Getting a new random number each iteration because multiple items could have the same
             * % chance to drop, and if one fails all items with that number will fail.
             * Getting a new random number will allow multiple items with the same % chance to drop.
             */

            for (ItemStack is : drops) {
                SlimefunItem sfi = SlimefunItem.getByItem(is);
                // Check the getBarteringLootChance and compare against a random number 0-100,
                // if the random number is greater then replace the item.
                if (sfi instanceof PiglinBarterDrop piglinBarterDrop) {
                    int chance = piglinBarterDrop.getBarteringLootChance();

                    if (chance < 1 || chance >= 100) {
                        // Warn at most once per misconfigured item: barter farms fire this event
                        // frequently, so re-warning on every barter would flood the log.
                        if (warnedInvalidBarterChances.add(sfi.getId())) {
                            sfi.warn("The Piglin Bartering chance must be between 1-99% on item: " + sfi.getId());
                        }
                    } else if (chance > ThreadLocalRandom.current().nextInt(100)) {
                        PiglinBarterDropEvent event = new PiglinBarterDropEvent((Piglin) e.getEntity(), e.getItemDrop(), sfi, chance);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            // The Piglin keeps its vanilla drop, no further items are tried
                            return;
                        }

                        // An addon may have replaced the barter drop itself
                        e.getItemDrop().setItemStack(event.getDrop());
                        return;
                    }
                }
            }
        }
    }
}

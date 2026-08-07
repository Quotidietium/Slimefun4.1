package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;

import io.github.thebusybiscuit.slimefun4.api.events.EnderBootsPearlProtectEvent;
import io.github.thebusybiscuit.slimefun4.api.events.FarmerShoesTramplePreventEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBootsFallEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.EnderBoots;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.FarmerShoes;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.LongFallBoots;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.StomperBoots;

/**
 * This {@link Listener} is responsible for handling all boots provided by
 * Slimefun, such as the {@link StomperBoots} or any {@link SlimefunArmorPiece} that
 * is a pair of boots and needs to listen to an {@link EntityDamageEvent}.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 *
 */
public class SlimefunBootsListener implements Listener {

    public SlimefunBootsListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == DamageCause.FALL) {
            onFallDamage(e);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnderPearlDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof EnderPearl && e.getEntity() instanceof Player p) {
            SlimefunItem boots = SlimefunItem.getByItem(p.getInventory().getBoots());

            if (boots instanceof EnderBoots && boots.canUse(p, true)) {
                if (EnderBootsPearlProtectEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    EnderBootsPearlProtectEvent event = new EnderBootsPearlProtectEvent(p, (EnderBoots) boots, p.getInventory().getBoots(), (EnderPearl) e.getDamager());
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        // An addon vetoed the protection; the pearl damage applies normally.
                        return;
                    }
                }

                e.setCancelled(true);
            }
        }
    }

    private void onFallDamage(@Nonnull EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            SlimefunItem boots = SlimefunItem.getByItem(p.getInventory().getBoots());

            if (boots != null) {
                // Check if the boots were researched
                if (!boots.canUse(p, true)) {
                    return;
                }

                if (boots instanceof StomperBoots || boots instanceof LongFallBoots) {
                    SlimefunBootsFallEvent event = new SlimefunBootsFallEvent(p, boots, e);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        // The protection is skipped: the Player takes the fall damage normally
                        return;
                    }
                }

                if (boots instanceof StomperBoots stomperBoots) {
                    e.setCancelled(true);
                    stomperBoots.stomp(e);
                } else if (boots instanceof LongFallBoots longFallBoots) {
                    e.setCancelled(true);
                    longFallBoots.getSoundEffect().playAt(p.getLocation(), SoundCategory.PLAYERS);
                }
            }
        }
    }

    @EventHandler
    public void onTrample(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) {
            Block b = e.getClickedBlock();

            if (b != null && b.getType() == Material.FARMLAND) {
                Player p = e.getPlayer();
                SlimefunItem boots = SlimefunItem.getByItem(p.getInventory().getBoots());

                if (boots instanceof FarmerShoes && boots.canUse(p, true)) {
                    if (FarmerShoesTramplePreventEvent.getHandlerList().getRegisteredListeners().length > 0) {
                        FarmerShoesTramplePreventEvent event = new FarmerShoesTramplePreventEvent(p, (FarmerShoes) boots, p.getInventory().getBoots(), b);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            // An addon vetoed the protection; the farmland is trampled as vanilla.
                            return;
                        }
                    }

                    e.setCancelled(true);
                }
            }
        }
    }
}

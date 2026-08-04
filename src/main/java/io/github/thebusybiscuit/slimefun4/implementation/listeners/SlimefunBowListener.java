package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBowShootEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SlimefunBow;

/**
 * This {@link Listener} is responsible for tracking {@link Arrow Arrows} fired from a
 * {@link SlimefunBow}.
 * 
 * @author TheBusyBiscuit
 * 
 * @see SlimefunBow
 *
 */
public class SlimefunBowListener implements Listener {

    private final Map<UUID, SlimefunBow> projectiles = new HashMap<>();

    public void register(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Periodically evict entries for arrows that vanished without a ProjectileHitEvent
        // (they despawned, flew into an unloaded chunk, were /kill-ed, ...). Otherwise the map
        // grows without bound on a busy archery server.
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::evictDeadProjectiles, 600L, 600L);
    }

    private void evictDeadProjectiles() {
        projectiles.entrySet().removeIf(entry -> Bukkit.getEntity(entry.getKey()) == null);
    }

    /**
     * This returns a {@link HashMap} holding the {@link UUID} of a {@link Arrow} and the
     * associated {@link SlimefunBow} with which this {@link Arrow} was fired from.
     * 
     * @return A {@link HashMap} with all actively tracked {@link Arrow Arrows}
     */
    @Nonnull
    public Map<UUID, SlimefunBow> getProjectileData() {
        return projectiles;
    }

    @EventHandler
    public void onBowUse(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player && e.getProjectile() instanceof Arrow) {
            SlimefunItem bow = SlimefunItem.getByItem(e.getBow());

            if (bow instanceof SlimefunBow slimefunBow) {
                SlimefunBowShootEvent shootEvent = new SlimefunBowShootEvent((Player) e.getEntity(), slimefunBow, e.getBow(), (Arrow) e.getProjectile(), e);
                Bukkit.getPluginManager().callEvent(shootEvent);

                // Only track the shot (and thus invoke BowShootHandler on hit) when not suppressed.
                // The arrow still flies regardless - we never touch the underlying Bukkit event.
                if (!shootEvent.isCancelled()) {
                    projectiles.put(e.getProjectile().getUniqueId(), slimefunBow);
                }
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent e) {
        Slimefun.runSync(() -> {
            if (e.getEntity().isValid() && e.getEntity() instanceof Arrow) {
                projectiles.remove(e.getEntity().getUniqueId());
            }
        }, 4L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArrowSuccessfulHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Arrow && e.getEntity() instanceof LivingEntity && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            SlimefunBow bow = projectiles.remove(e.getDamager().getUniqueId());

            if (!e.isCancelled() && bow != null) {
                bow.callItemHandler(BowShootHandler.class, handler -> handler.onHit(e, (LivingEntity) e.getEntity()));
            }
        }
    }

}

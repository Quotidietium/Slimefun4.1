package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import io.github.thebusybiscuit.slimefun4.api.events.RadiationResetEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.RadiationTask;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * {@link RadioactivityListener} handles radioactivity level resets
 * on death
 *
 * @author Semisol
 */
public class RadioactivityListener implements Listener {

    public RadioactivityListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(@Nonnull PlayerDeathEvent e) {
        if (isResetVetoed(e.getEntity(), e)) {
            // An addon vetoed the reset; the exposure (and no grace period) carries through the death.
            return;
        }

        RadiationUtils.clearExposure(e.getEntity());
        RadiationTask.addGracePeriod(e.getEntity());
    }

    /**
     * Fires a {@link RadiationResetEvent} if the {@link Player} died with any exposure
     * and a listener is registered, and returns whether the reset was vetoed. Without
     * listeners (or nothing to reset) this costs nothing and the old behavior is kept.
     */
    private boolean isResetVetoed(@Nonnull Player p, @Nonnull PlayerDeathEvent e) {
        int exposureBefore = RadiationUtils.getExposure(p);

        if (exposureBefore > 0 && RadiationResetEvent.getHandlerList().getRegisteredListeners().length > 0) {
            RadiationResetEvent event = new RadiationResetEvent(p, exposureBefore, e);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        }

        return false;
    }
}

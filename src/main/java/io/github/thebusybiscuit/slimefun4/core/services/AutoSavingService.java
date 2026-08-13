package io.github.thebusybiscuit.slimefun4.core.services;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerProfileUnloadEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunAutoSaveEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockDataSaveEvent;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.debug.Debug;
import io.github.thebusybiscuit.slimefun4.core.debug.TestCase;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * This Service is responsible for automatically saving {@link Player} and {@link Block}
 * data.
 * 
 * @author TheBusyBiscuit
 *
 */
public class AutoSavingService {

    private int interval;

    /**
     * This method starts the {@link AutoSavingService} with the given interval.
     * 
     * @param plugin
     *            The current instance of Slimefun
     * @param interval
     *            The interval in which to run this task
     */
    public void start(@Nonnull Slimefun plugin, int interval) {
        if (interval <= 0) {
            /*
             * A missing or corrupted config value (Config#getInt returns 0) or a negative
             * delay would schedule these tasks with a nonsensical period: running every
             * tick (disk-I/O and log storm) or never repeating (auto-save silently
             * disabled). Fall back to the documented default instead.
             */
            Slimefun.logger().log(Level.WARNING, "The auto-save delay is configured as {0} minute(s), which is invalid. Falling back to the default of 10 minutes", interval);
            interval = 10;
        }

        this.interval = interval;

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllPlayers, 2000L, interval * 60L * 20L);
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllBlocks, 2000L, interval * 60L * 20L);
    }

    /**
     * This method saves every {@link PlayerProfile} in memory and removes profiles
     * that were marked for deletion.
     */
    private void saveAllPlayers() {
        Iterator<PlayerProfile> iterator = PlayerProfile.iterator();
        int players = 0;

        Debug.log(TestCase.PLAYER_PROFILE_DATA, "Saving all players data");

        while (iterator.hasNext()) {
            PlayerProfile profile = iterator.next();
            boolean saved = true;

            if (profile.isDirty()) {
                try {
                    profile.save();
                    players++;

                    Debug.log(TestCase.PLAYER_PROFILE_DATA, "Saved data for {} ({})",
                        profile.getPlayer() != null ? profile.getPlayer().getName() : "Unknown", profile.getUUID()
                    );
                } catch (Exception | LinkageError x) {
                    /*
                     * One broken profile must not abort the whole auto-save run (an
                     * uncaught exception would even cancel this repeating task).
                     * The profile stays dirty, so the next cycle retries it.
                     */
                    saved = false;
                    Slimefun.logger().log(Level.WARNING, x, () -> "Could not auto-save the PlayerProfile for " + profile.getUUID() + ", will retry on the next cycle");
                }
            }

            // Remove the PlayerProfile from memory if the player has left the server (marked from removal)
            // and they're still not on the server
            // At this point, we've already saved their profile so we can safely remove it
            // without worry for having a data sync issue (e.g. data is changed but then we try to re-load older data)
            if (saved && profile.isMarkedForDeletion() && profile.getPlayer() == null) {
                if (PlayerProfileUnloadEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    Bukkit.getPluginManager().callEvent(new PlayerProfileUnloadEvent(profile));
                }

                iterator.remove();

                Debug.log(TestCase.PLAYER_PROFILE_DATA, "Removed data from memory for {}",
                    profile.getUUID()
                );
            }
        }

        if (players > 0) {
            Slimefun.logger().log(Level.INFO, "Auto-saved all player data for {0} player(s)!", players);
        }

        if (SlimefunAutoSaveEvent.getHandlerList().getRegisteredListeners().length > 0) {
            Bukkit.getPluginManager().callEvent(new SlimefunAutoSaveEvent(players));
        }
    }

    /**
     * This method saves the data of every {@link Block} marked dirty by {@link BlockStorage}.
     */
    private void saveAllBlocks() {
        Set<BlockStorage> worlds = new HashSet<>();

        for (World world : Bukkit.getWorlds()) {
            try {
                BlockStorage storage = BlockStorage.getStorage(world);

                if (storage != null) {
                    storage.computeChanges();

                    if (storage.getChanges() > 0) {
                        worlds.add(storage);
                    }
                }
            } catch (Exception | LinkageError x) {
                /*
                 * One broken world must not abort the whole auto-save run (an uncaught
                 * exception would even cancel this repeating task, silently disabling
                 * block auto-saves until a restart).
                 */
                Slimefun.logger().log(Level.WARNING, x, () -> "Could not compute block-data changes for world " + world.getName() + ", will retry on the next cycle");
            }
        }

        int savedWorlds = 0;

        if (!worlds.isEmpty()) {
            Slimefun.logger().log(Level.INFO, "Auto-saving block data... (Next auto-save: {0}m)", interval);

            for (BlockStorage storage : worlds) {
                try {
                    storage.save();
                    savedWorlds++;
                } catch (Exception | LinkageError x) {
                    /*
                     * One broken world must not abort the whole auto-save run (see above).
                     * BlockStorage re-queues failed changes itself, so the next cycle
                     * retries them.
                     */
                    Slimefun.logger().log(Level.WARNING, x, () -> "Could not auto-save block data for a world, will retry on the next cycle");
                }
            }
        }

        try {
            BlockStorage.saveChunks();
        } catch (Exception | LinkageError x) {
            // The chunk counter was already drained; the next cycle re-attempts a save
            Slimefun.logger().log(Level.WARNING, x, () -> "Could not auto-save chunk data, will retry on the next cycle");
        }

        if (SlimefunBlockDataSaveEvent.getHandlerList().getRegisteredListeners().length > 0) {
            Bukkit.getPluginManager().callEvent(new SlimefunBlockDataSaveEvent(savedWorlds));
        }
    }

}

package io.github.thebusybiscuit.slimefun4.storage.backend.legacy;

import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.gps.Waypoint;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.Storage;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.google.common.annotations.Beta;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

@Beta
public class LegacyStorage implements Storage {

    @Override
    public PlayerData loadPlayerData(@Nonnull UUID uuid) {
        Config playerFile = new Config("data-storage/Slimefun/Players/" + uuid + ".yml");
        // Not too sure why this is its own file
        Config waypointsFile = new Config("data-storage/Slimefun/waypoints/" + uuid + ".yml");

        // Load research
        Set<Research> researches = new HashSet<>();
        for (Research research : Slimefun.getRegistry().getResearches()) {
            if (playerFile.contains("researches." + research.getID())) {
                researches.add(research);
            }
        }

        // Load backpacks
        HashMap<Integer, PlayerBackpack> backpacks = new HashMap<>();
        for (String key : playerFile.getKeys("backpacks")) {
            try {
                int id = Integer.parseInt(key);
                int size = playerFile.getInt("backpacks." + key + ".size");

                if (size <= 0) {
                    /*
                     * The stored size is missing or corrupted (getInt returns 0 for
                     * non-numbers). Loading an empty backpack here would permanently
                     * wipe the stored contents on the next save, so infer the size
                     * from the highest content slot instead.
                     */
                    size = inferBackpackSize(playerFile, "backpacks." + key + ".contents");
                }

                HashMap<Integer, ItemStack> items = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    items.put(i, playerFile.getItem("backpacks." + key + ".contents." + i));
                }

                PlayerBackpack backpack = PlayerBackpack.load(uuid, id, size, items);

                backpacks.put(id, backpack);
            } catch (Exception x) {
                Slimefun.logger().log(Level.WARNING, x, () -> "Could not load Backpack \"" + key + "\" for Player \"" + uuid + '"');
            }
        }

        // Load waypoints
        Set<Waypoint> waypoints = new HashSet<>();
        Map<String, Map<String, Object>> unresolvedWaypoints = new HashMap<>();

        for (String key : waypointsFile.getKeys()) {
            try {
                if (waypointsFile.contains(key + ".world") && Bukkit.getWorld(waypointsFile.getString(key + ".world")) != null) {
                    String waypointName = waypointsFile.getString(key + ".name");
                    Location loc = waypointsFile.getLocation(key);
                    waypoints.add(new Waypoint(uuid, key, loc, waypointName));
                } else {
                    /*
                     * The waypoint's world is not loaded (e.g. a multi-world plugin loading
                     * after Slimefun, or a temporarily renamed/unloaded world). Dropping the
                     * entry here would make the next save wipe it from the file permanently
                     * (waypointsFile.clear()), so preserve the raw values and write them
                     * back verbatim on save instead.
                     */
                    unresolvedWaypoints.put(key, captureRawWaypoint(waypointsFile, key));
                }
            } catch (Exception x) {
                Slimefun.logger().log(Level.WARNING, x, () -> "Could not load Waypoint \"" + key + "\" for Player \"" + uuid + '"');
            }
        }

        PlayerData playerData = new PlayerData(researches, backpacks, waypoints);
        playerData.getUnresolvedWaypoints().putAll(unresolvedWaypoints);
        return playerData;
    }

    /**
     * Captures the raw config entries of a waypoint that could not be resolved, keyed
     * by their path suffix relative to the waypoint id (e.g. "x", "world", "name").
     */
    @ParametersAreNonnullByDefault
    private static Map<String, Object> captureRawWaypoint(Config waypointsFile, String key) {
        Map<String, Object> raw = new HashMap<>();

        for (String sub : waypointsFile.getKeys(key)) {
            raw.put(sub, waypointsFile.getValue(key + "." + sub));
        }

        return raw;
    }

    /**
     * Infers a backpack's size from its highest content slot, rounded up to the
     * next multiple of 9 and capped at 54. Used when the stored size is missing
     * or corrupted so the contents survive the load instead of being wiped by
     * the next save.
     *
     * @param playerFile
     *            The player's data file
     * @param contentsPath
     *            The config path of the backpack's contents section
     *
     * @return The inferred backpack size
     */
    private static int inferBackpackSize(@Nonnull Config playerFile, @Nonnull String contentsPath) {
        int maxSlot = -1;

        for (String contentKey : playerFile.getKeys(contentsPath)) {
            try {
                maxSlot = Math.max(maxSlot, Integer.parseInt(contentKey));
            } catch (NumberFormatException ignored) {
                // Not a slot key, skip it
            }
        }

        if (maxSlot < 0) {
            throw new IllegalStateException("The backpack size is corrupted and there are no contents to infer it from");
        }

        return Math.min(54, (maxSlot / 9 + 1) * 9);
    }

    // The current design of saving all at once isn't great, this will be refined.
    @Override
    public void savePlayerData(@Nonnull UUID uuid, @Nonnull PlayerData data) {
        Config playerFile = new Config("data-storage/Slimefun/Players/" + uuid + ".yml");
        // Not too sure why this is its own file
        Config waypointsFile = new Config("data-storage/Slimefun/waypoints/" + uuid + ".yml");

        // Save research
        playerFile.setValue("researches", null);
        for (Research research : Slimefun.getRegistry().getResearches()) {
            // Save the research if it's researched
            if (data.getResearches().contains(research)) {
                playerFile.setValue("researches." + research.getID(), true);

            // Remove the research if it's no longer researched
            // ----
            // We have a duplicate ID (173) used for both Coal Gen and Bio Reactor
            // If you researched the Goal Gen we would remove it on save if you didn't also have the Bio Reactor
            // Due to the fact we would set it as researched (true in the branch above) on Coal Gen
            // but then go into this branch and remove it if you didn't have Bio Reactor
            // Sooooo we're gonna hack this for now while we move away from the Legacy Storage
            // Let's make sure the user doesn't have _any_ research with this ID and _then_ remove it
            } else if (
                playerFile.contains("researches." + research.getID())
                && !data.getResearches().stream().anyMatch((r) -> r.getID() == research.getID())
            ) {
                playerFile.setValue("researches." + research.getID(), null);
            }
        }

        // Save backpacks
        for (PlayerBackpack backpack : data.getBackpacks().values()) {
            playerFile.setValue("backpacks." + backpack.getId() + ".size", backpack.getSize());

            for (int i = 0; i < backpack.getSize(); i++) {
                ItemStack item = backpack.getInventory().getItem(i);
                if (item != null) {
                    playerFile.setValue("backpacks." + backpack.getId() + ".contents." + i, item);

                // Remove the item if it's no longer in the inventory
                } else if (playerFile.contains("backpacks." + backpack.getId() + ".contents." + i)) {
                    playerFile.setValue("backpacks." + backpack.getId() + ".contents." + i, null);
                }
            }
        }

        // Save waypoints
        waypointsFile.clear();

        for (Waypoint waypoint : data.getWaypoints()) {
            // Legacy data uses IDs
            waypointsFile.setValue(waypoint.getId(), waypoint.getLocation());
            waypointsFile.setValue(waypoint.getId() + ".name", waypoint.getName());
        }

        /*
         * Write back the entries that could not be resolved on load (their world was
         * not loaded), unless a resolvable waypoint now claims the same id - clear()
         * above must not become a silent delete for waypoints we simply cannot see
         * right now.
         */
        Set<String> resolvedIds = new HashSet<>();

        for (Waypoint waypoint : data.getWaypoints()) {
            resolvedIds.add(waypoint.getId());
        }

        for (Map.Entry<String, Map<String, Object>> entry : data.getUnresolvedWaypoints().entrySet()) {
            if (!resolvedIds.contains(entry.getKey())) {
                for (Map.Entry<String, Object> value : entry.getValue().entrySet()) {
                    waypointsFile.setValue(entry.getKey() + "." + value.getKey(), value.getValue());
                }
            }
        }

        // Save files (atomically - a crash mid-write must not corrupt the previous state)
        if (!saveAtomically(playerFile) | !saveAtomically(waypointsFile)) {
            // | on purpose: attempt BOTH files, then report
            throw new UncheckedIOException(new IOException("Could not save the player data for " + uuid + " (disk full?), the profile stays dirty and will be retried"));
        }
    }

    /**
     * Writes a {@link Config} to disk via a temporary file and an atomic move
     * (with a plain replace as fallback for filesystems that do not support
     * atomic moves), the same strategy {@code BlockStorage} uses for .sfb files.
     *
     * @param config
     *            The {@link Config} to write
     *
     * @return Whether the file was successfully written
     */
    private boolean saveAtomically(@Nonnull Config config) {
        File target = config.getFile();
        File tmpFile = new File(target.getParentFile(), target.getName() + ".tmp");

        config.save(tmpFile);

        if (!tmpFile.exists()) {
            // Config.save() swallowed an IOException (e.g. disk full) - nothing to move
            Slimefun.logger().log(Level.SEVERE, "Could not write a temporary file for \"{0}\" (disk full?), will retry on the next save cycle", target.getName());
            return false;
        }

        try {
            Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException x) {
            try {
                // Some filesystems do not support atomic moves - fall back to a plain replace
                Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException x2) {
                Slimefun.logger().log(Level.SEVERE, x2, () -> "An Error occurred while saving player data to \"" + target.getName() + "\", will retry on the next save cycle");
                return false;
            }
        }
    }
}

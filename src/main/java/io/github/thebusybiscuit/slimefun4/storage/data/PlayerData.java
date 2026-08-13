package io.github.thebusybiscuit.slimefun4.storage.data;

import com.google.common.annotations.Beta;

import io.github.thebusybiscuit.slimefun4.api.gps.Waypoint;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;

/**
 * The data which backs {@link io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile}
 *
 * <b>This API is still experimental, it may change without notice.</b>
 */
// TODO: Should we keep this in PlayerProfile?
@Beta
public class PlayerData {

    // These collections are iterated by the async auto-save thread (LegacyStorage) while the
    // main thread mutates them (unlocking researches, adding waypoints, ...). Plain HashSet/Map
    // would throw ConcurrentModificationException under load, so we use concurrent backings.
    private final Set<Research> researches = ConcurrentHashMap.newKeySet();
    private final Map<Integer, PlayerBackpack> backpacks = new ConcurrentHashMap<>();
    private final Set<Waypoint> waypoints = ConcurrentHashMap.newKeySet();

    /*
     * Raw waypoint entries that could not be resolved when the profile was read (their
     * world was not loaded). They are not part of #waypoints, but must be written back
     * verbatim on save - otherwise the next save would wipe them from the file
     * permanently (data loss for servers with late-loading or temporarily unloaded
     * worlds). Populated by the storage backend during load.
     */
    private final Map<String, Map<String, Object>> unresolvedWaypoints = new ConcurrentHashMap<>();

    public PlayerData(Set<Research> researches, Map<Integer, PlayerBackpack> backpacks, Set<Waypoint> waypoints) {
        this.researches.addAll(researches);
        this.backpacks.putAll(backpacks);
        this.waypoints.addAll(waypoints);
    }

    public Set<Research> getResearches() {
        return researches;
    }

    public void addResearch(@Nonnull Research research) {
        Validate.notNull(research, "Cannot add a 'null' research!");
        researches.add(research);
    }

    public void removeResearch(@Nonnull Research research) {
        Validate.notNull(research, "Cannot remove a 'null' research!");
        researches.remove(research);
    }

    @Nonnull
    public Map<Integer, PlayerBackpack> getBackpacks() {
        return backpacks;
    }

    @Nonnull
    public PlayerBackpack getBackpack(int id) {
        return backpacks.get(id);
    }

    public void addBackpack(@Nonnull PlayerBackpack backpack) {
        Validate.notNull(backpack, "Cannot add a 'null' backpack!");
        backpacks.put(backpack.getId(), backpack);
    }

    public void removeBackpack(@Nonnull PlayerBackpack backpack) {
        Validate.notNull(backpack, "Cannot remove a 'null' backpack!");
        backpacks.remove(backpack.getId());
    }

    public Set<Waypoint> getWaypoints() {
        return waypoints;
    }

    /**
     * Returns the raw waypoint entries that could not be resolved when this profile was
     * read (e.g. their {@link org.bukkit.World} was not loaded). These are not visible
     * as {@link Waypoint Waypoints} and cannot be interacted with, but the storage
     * backend writes them back verbatim on save so they survive until their world is
     * available again.
     *
     * @return A mutable map of waypoint id to its raw config entries (path suffix to value)
     */
    @Nonnull
    public Map<String, Map<String, Object>> getUnresolvedWaypoints() {
        return unresolvedWaypoints;
    }

    public void addWaypoint(@Nonnull Waypoint waypoint) {
        Validate.notNull(waypoint, "Cannot add a 'null' waypoint!");

        for (Waypoint wp : waypoints) {
            if (wp.getId().equals(waypoint.getId())) {
                throw new IllegalArgumentException("A Waypoint with that id already exists for this Player");
            }
        }

        // Limited to 21 due to limited UI space and no pagination
        if (waypoints.size() >= 21) {
            return; // not sure why this doesn't throw but the one above does...
        }

        waypoints.add(waypoint);
    }

    public void removeWaypoint(@Nonnull Waypoint waypoint) {
        Validate.notNull(waypoint, "Cannot remove a 'null' waypoint!");
        waypoints.remove(waypoint);
    }
}

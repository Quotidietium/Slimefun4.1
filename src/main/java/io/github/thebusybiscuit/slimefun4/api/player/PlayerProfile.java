package io.github.thebusybiscuit.slimefun4.api.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.common.CommonPatterns;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.events.AsyncProfileLoadEvent;
import io.github.thebusybiscuit.slimefun4.api.gps.Waypoint;
import io.github.thebusybiscuit.slimefun4.api.items.HashedArmorpiece;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectiveArmor;
import io.github.thebusybiscuit.slimefun4.core.debug.Debug;
import io.github.thebusybiscuit.slimefun4.core.debug.TestCase;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;

/**
 * A class that can store a Player's {@link Research} progress for caching purposes.
 * It also holds the backpacks of a {@link Player}.
 * 
 * @author TheBusyBiscuit
 * 
 * @see Research
 * @see Waypoint
 * @see PlayerBackpack
 * @see HashedArmorpiece
 *
 */
public class PlayerProfile {

    private static final Map<UUID, List<QueuedProfileCallback>> loading = new ConcurrentHashMap<>();

    /**
     * How often loading a {@link PlayerProfile} is attempted before giving up
     * and invoking the failure handlers instead of the callbacks.
     */
    private static final int MAX_LOAD_ATTEMPTS = 3;

    /**
     * A queued {@link PlayerProfile} callback together with an optional failure
     * handler which is run when the Profile could not be loaded at all.
     */
    private static final class QueuedProfileCallback {

        private final Consumer<PlayerProfile> callback;
        private final Runnable failureHandler;

        QueuedProfileCallback(@Nonnull Consumer<PlayerProfile> callback, @Nullable Runnable failureHandler) {
            this.callback = callback;
            this.failureHandler = failureHandler;
        }
    }

    private final UUID ownerId;
    private final String name;

    private final Config configFile;

    // Volatile: these are written on the main thread (and the async profile-load thread) and read
    // by the async auto-save thread and onDisable. Without volatile the auto-save thread may never
    // observe a just-written dirty=true and skip saving the profile for a whole cycle.
    private volatile boolean dirty = false;
    private volatile boolean markedForDeletion = false;

    /**
     * Incremented on every {@link #markDirty()} call. {@link #save()} only clears the
     * {@link #dirty} flag when this value is unchanged across the persist, so a mutation that
     * lands while we are writing to disk is never silently dropped (it keeps the profile dirty
     * and forces another save cycle).
     */
    private final AtomicLong modificationEpoch = new AtomicLong();

    private final GuideHistory guideHistory = new GuideHistory(this);

    private final HashedArmorpiece[] armor = { new HashedArmorpiece(), new HashedArmorpiece(), new HashedArmorpiece(), new HashedArmorpiece() };

    private final PlayerData data;

    protected PlayerProfile(@Nonnull OfflinePlayer p, PlayerData data) {
        this.ownerId = p.getUniqueId();
        this.name = p.getName();
        this.data = data;

        configFile = new Config("data-storage/Slimefun/Players/" + ownerId.toString() + ".yml");
    }

    /**
     * This method provides a fast way to access the armor of a {@link Player}.
     * It returns a cached version, represented by {@link HashedArmorpiece}.
     * 
     * @return The cached armor for this {@link Player}
     */
    public @Nonnull HashedArmorpiece[] getArmor() {
        return armor;
    }

    /**
     * This returns the {@link Config} which is used to store the data.
     * Only intended for internal usage.
     * 
     * @return The {@link Config} associated with this {@link PlayerProfile}
     *
     * @deprecated Look at {@link PlayerProfile#getPlayerData()} instead for reading data.
     */
    @Deprecated
    public @Nonnull Config getConfig() {
        return configFile;
    }

    /**
     * This returns the {@link UUID} this {@link PlayerProfile} is linked to.
     * 
     * @return The {@link UUID} of our {@link PlayerProfile}
     */
    public @Nonnull UUID getUUID() {
        return ownerId;
    }

    /**
     * This method returns whether the {@link Player} has logged off.
     * If this is true, then the Profile can be removed from RAM.
     * 
     * @return Whether the Profile is marked for deletion
     */
    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    /**
     * This method returns whether the Profile has unsaved changes
     * 
     * @return Whether there are unsaved changes
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * This method will save the Player's Researches and Backpacks to the hard drive
     */
    public void save() {
        // Capture the epoch before writing. If any mutation (which always bumps the epoch via
        // markDirty() AFTER changing the data) lands while we are writing, the epoch will have
        // moved and we leave the profile dirty so the change is saved on the next cycle instead
        // of being silently dropped.
        long epochBefore = modificationEpoch.get();

        // Throws UncheckedIOException on failure, which leaves dirty untouched so the profile is
        // retried on the next save cycle.
        Slimefun.getPlayerStorage().savePlayerData(this.ownerId, this.data);

        if (modificationEpoch.get() == epochBefore) {
            dirty = false;
        }
    }

    /**
     * This method sets the Player's "researched" status for this Research.
     * Use the boolean to unlock or lock the {@link Research}
     * 
     * @param research
     *            The {@link Research} that should be unlocked or locked
     * @param unlock
     *            Whether the {@link Research} should be unlocked or locked
     */
    public void setResearched(@Nonnull Research research, boolean unlock) {
        Validate.notNull(research, "Research must not be null!");

        // Mutate first, then mark dirty. markDirty() bumps the modification epoch which save()
        // uses to detect concurrent mutations - the epoch must always reflect the state of the
        // data, so the bookkeeping has to run AFTER the change, never before.
        if (unlock) {
            data.addResearch(research);
        } else {
            data.removeResearch(research);
        }

        markDirty();
    }

    /**
     * This method returns whether the {@link Player} has unlocked the given {@link Research}
     * 
     * @param research
     *            The {@link Research} that is being queried
     * 
     * @return Whether this {@link Research} has been unlocked
     */
    public boolean hasUnlocked(@Nullable Research research) {
        if (research == null) {
            // No Research, no restriction
            return true;
        }

        return !research.isEnabled() || data.getResearches().contains(research);
    }

    /**
     * This method returns whether this {@link Player} has unlocked all {@link Research Researches}.
     * 
     * @return Whether they unlocked every {@link Research}
     */
    public boolean hasUnlockedEverything() {
        for (Research research : Slimefun.getRegistry().getResearches()) {
            // If there is a single Research not unlocked: They haven't unlocked everything.
            if (!hasUnlocked(research)) {
                return false;
            }
        }

        // Player has everything unlocked - Hooray!
        return true;
    }

    /**
     * This Method will return all Researches that this {@link Player} has unlocked
     * 
     * @return A {@code Hashset<Research>} of all Researches this {@link Player} has unlocked
     */
    public @Nonnull Set<Research> getResearches() {
        return ImmutableSet.copyOf(this.data.getResearches());
    }

    /**
     * This returns a {@link List} of all {@link Waypoint Waypoints} belonging to this
     * {@link PlayerProfile}.
     * 
     * @return A {@link List} containing every {@link Waypoint}
     */
    public @Nonnull List<Waypoint> getWaypoints() {
        return ImmutableList.copyOf(this.data.getWaypoints());
    }

    /**
     * This adds the given {@link Waypoint} to the {@link List} of {@link Waypoint Waypoints}
     * of this {@link PlayerProfile}.
     * 
     * @param waypoint
     *            The {@link Waypoint} to add
     */
    public void addWaypoint(@Nonnull Waypoint waypoint) {
        this.data.addWaypoint(waypoint);
        markDirty();
    }

    /**
     * This removes the given {@link Waypoint} from the {@link List} of {@link Waypoint Waypoints}
     * of this {@link PlayerProfile}.
     * 
     * @param waypoint
     *            The {@link Waypoint} to remove
     */
    public void removeWaypoint(@Nonnull Waypoint waypoint) {
        this.data.removeWaypoint(waypoint);
        markDirty();
    }

    /**
     * Call this method if the Player has left.
     * The profile can then be removed from RAM.
     */
    public final void markForDeletion() {
        Debug.log(TestCase.PLAYER_PROFILE_DATA, "Marking {} ({}) profile for deletion", name, ownerId);
        markedForDeletion = true;
    }

    /**
     * Call this method if this Profile has unsaved changes.
     */
    public final void markDirty() {
        Debug.log(TestCase.PLAYER_PROFILE_DATA, "Marking {} ({}) profile as dirty", name, ownerId);
        dirty = true;
        modificationEpoch.incrementAndGet();
    }

    public @Nonnull PlayerBackpack createBackpack(int size) {
        /*
         * The next free id is max-key + 1, NOT the map size: once a backpack in
         * the middle was removed (e.g. via an addon), size() would collide with
         * an existing id and silently overwrite that backpack's contents.
         */
        int nextId = 0;

        for (int id : this.data.getBackpacks().keySet()) {
            nextId = Math.max(nextId, id + 1);
        }

        PlayerBackpack backpack = PlayerBackpack.newBackpack(this.ownerId, nextId, size);
        this.data.addBackpack(backpack);

        markDirty();

        return backpack;
    }

    public @Nonnull Optional<PlayerBackpack> getBackpack(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("Backpacks cannot have negative ids!");
        }

        PlayerBackpack backpack = data.getBackpack(id);

        if (backpack != null) {
            markDirty();
            return Optional.of(backpack);
        }

        return Optional.empty();
    }

    private int countNonEmptyResearches(@Nonnull Collection<Research> researches) {
        int count = 0;
        for (Research research : researches) {
            if (research.hasEnabledItems()) {
                count++;
            }
        }
        return count;
    }

    /**
     * This method gets the research title, as defined in {@code config.yml},
     * of this {@link PlayerProfile} based on the fraction
     * of unlocked {@link Research}es of this player.
     *
     * @return The research title of this {@link PlayerProfile}
     */
    public @Nonnull String getTitle() {
        List<String> titles = Slimefun.getRegistry().getResearchRanks();

        int allResearches = countNonEmptyResearches(Slimefun.getRegistry().getResearches());
        float fraction = (float) countNonEmptyResearches(getResearches()) / allResearches;
        int index = (int) (fraction * (titles.size() - 1));

        return titles.get(index);
    }

    /**
     * This sends the statistics for the specified {@link CommandSender}
     * to the {@link CommandSender}. This includes research title, research progress
     * and total xp spent.
     *
     * @param sender The {@link CommandSender} for which to get the statistics and send them to.
     */
    public void sendStats(@Nonnull CommandSender sender) {
        int unlockedResearches = countNonEmptyResearches(getResearches());
        int levels = getResearches().stream().mapToInt(Research::getCost).sum();
        int allResearches = countNonEmptyResearches(Slimefun.getRegistry().getResearches());

        float progress = Math.round(((unlockedResearches * 100.0F) / allResearches) * 100.0F) / 100.0F;

        sender.sendMessage("");
        sender.sendMessage(ChatColors.color("&7Statistics for Player: &b" + name));
        sender.sendMessage("");
        sender.sendMessage(ChatColors.color("&7Title: " + ChatColor.AQUA + getTitle()));
        sender.sendMessage(ChatColors.color("&7Research Progress: " + NumberUtils.getColorFromPercentage(progress) + progress + " &r% " + ChatColor.YELLOW + '(' + unlockedResearches + " / " + allResearches + ')'));
        sender.sendMessage(ChatColors.color("&7Total XP Levels spent: " + ChatColor.AQUA + levels));
    }

    /**
     * This returns the {@link Player} who this {@link PlayerProfile} belongs to.
     * If the {@link Player} is offline, null will be returned.
     * 
     * @return The {@link Player} of this {@link PlayerProfile} or null
     */
    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(getUUID());
    }

    /**
     * This returns the {@link GuideHistory} of this {@link Player}.
     * It is basically that player's browsing history.
     * 
     * @return The {@link GuideHistory} of this {@link Player}
     */
    public @Nonnull GuideHistory getGuideHistory() {
        return guideHistory;
    }

    public static boolean fromUUID(@Nonnull UUID uuid, @Nonnull Consumer<PlayerProfile> callback) {
        return get(Bukkit.getOfflinePlayer(uuid), callback);
    }

    /**
     * Get the {@link PlayerProfile} for a {@link OfflinePlayer} asynchronously.
     *
     * @param p
     *            The {@link OfflinePlayer} who's {@link PlayerProfile} to retrieve
     * @param callback
     *            The callback with the {@link PlayerProfile}
     *
     * @return If the {@link OfflinePlayer} was cached or not.
     */
    public static boolean get(@Nonnull OfflinePlayer p, @Nonnull Consumer<PlayerProfile> callback) {
        return get(p, callback, null);
    }

    /**
     * Get the {@link PlayerProfile} for a {@link OfflinePlayer} asynchronously.
     * <p>
     * If the Profile could not be loaded even after retrying, the given
     * {@link Runnable} is run on the main Thread instead of the callback, so
     * callers can compensate (e.g. refund a cost they already took).
     *
     * @param p
     *            The {@link OfflinePlayer} who's {@link PlayerProfile} to retrieve
     * @param callback
     *            The callback with the {@link PlayerProfile}
     * @param failureHandler
     *            A {@link Runnable} run on the main Thread when loading failed, or null
     *
     * @return If the {@link OfflinePlayer} was cached or not.
     */
    public static boolean get(@Nonnull OfflinePlayer p, @Nonnull Consumer<PlayerProfile> callback, @Nullable Runnable failureHandler) {
        Validate.notNull(p, "Cannot get a PlayerProfile for: null!");
        UUID uuid = p.getUniqueId();

        Debug.log(TestCase.PLAYER_PROFILE_DATA, "Getting PlayerProfile for {}", uuid);

        PlayerProfile profile = Slimefun.getRegistry().getPlayerProfiles().get(uuid);

        if (profile != null) {
            Debug.log(TestCase.PLAYER_PROFILE_DATA, "PlayerProfile for {} was already loaded", uuid);
            callback.accept(profile);
            return true;
        }

        // If we're already loading, we don't want to spin up a whole new thread and load the profile again/more
        // This can very easily cause CPU, memory and thread exhaustion if the profile is large
        // See #4011, #4116
        List<QueuedProfileCallback> newQueue = new ArrayList<>();
        List<QueuedProfileCallback> queue = loading.putIfAbsent(uuid, newQueue);

        if (queue != null) {
            Debug.log(TestCase.PLAYER_PROFILE_DATA, "Attempted to get PlayerProfile ({}) while loading", uuid);

            /*
             * Instead of dropping the callback (which could silently void actions
             * that already consumed resources, e.g. a research that took its XP
             * cost), we queue it and run it once the profile has finished loading.
             * The monitor on the queue is shared with the loading thread, which
             * removes the loading flag and flushes the queue atomically.
             */
            synchronized (queue) {
                PlayerProfile loaded = Slimefun.getRegistry().getPlayerProfiles().get(uuid);

                if (loaded != null) {
                    // Loading finished in the meantime, no need to queue
                    callback.accept(loaded);
                } else {
                    queue.add(new QueuedProfileCallback(callback, failureHandler));
                }
            }

            return false;
        }

        Slimefun.getThreadService().newThread(Slimefun.instance(), "PlayerProfile#get(" + uuid + ")", () -> {
            PlayerProfile loadedProfile = loadProfile(p, uuid);

            /*
             * Make sure we remove the loading flag after we put the PlayerProfile
             * into the registry (loadProfile does that). Otherwise, we end up with
             * a race condition where the profile is not in the map just _yet_ but
             * the loading flag is gone and we can end up loading it a second time
             * (and thus can dupe items).
             * Fixes https://github.com/Slimefun/Slimefun4/issues/4130
             *
             * The flag removal and the queue flush happen under the queue's
             * monitor so no callback can slip in between and get lost. The flag is
             * also cleared when loading failed, otherwise the profile could never
             * be loaded again (soft-lock).
             */
            synchronized (newQueue) {
                loading.remove(uuid, newQueue);

                for (QueuedProfileCallback queuedCallback : newQueue) {
                    deliver(queuedCallback, loadedProfile, uuid);
                }

                newQueue.clear();
            }

            deliver(new QueuedProfileCallback(callback, failureHandler), loadedProfile, uuid);
        });

        return false;
    }

    /**
     * Loads the {@link PlayerProfile} for the given {@link UUID}, retrying up to
     * {@value #MAX_LOAD_ATTEMPTS} times before giving up. A transient I/O hiccup
     * (e.g. a briefly locked file) must not void the callbacks of actions that
     * already consumed resources.
     *
     * @param p
     *            The {@link OfflinePlayer} to load the Profile for
     * @param uuid
     *            The {@link UUID} of that Player
     *
     * @return The loaded {@link PlayerProfile}, or null if all attempts failed
     */
    @Nullable
    private static PlayerProfile loadProfile(@Nonnull OfflinePlayer p, @Nonnull UUID uuid) {
        for (int attempt = 1; attempt <= MAX_LOAD_ATTEMPTS; attempt++) {
            try {
                PlayerData data = Slimefun.getPlayerStorage().loadPlayerData(uuid);

                AsyncProfileLoadEvent event = new AsyncProfileLoadEvent(new PlayerProfile(p, data));
                Bukkit.getPluginManager().callEvent(event);

                PlayerProfile profile = event.getProfile();
                Slimefun.getRegistry().getPlayerProfiles().put(uuid, profile);
                markForDeletionIfOffline(uuid, profile);
                return profile;
            } catch (Exception | LinkageError x) {
                if (attempt < MAX_LOAD_ATTEMPTS) {
                    int currentAttempt = attempt;
                    Slimefun.logger().log(Level.WARNING, x, () -> "Failed to load the PlayerProfile for " + uuid + " (attempt " + currentAttempt + '/' + MAX_LOAD_ATTEMPTS + "), retrying...");

                    try {
                        // Give the transient cause (file lock, I/O hiccup) a moment to pass
                        Thread.sleep(100L * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                } else {
                    Slimefun.logger().log(Level.SEVERE, x, () -> "Failed to load the PlayerProfile for " + uuid + " after " + MAX_LOAD_ATTEMPTS + " attempts");
                }
            }
        }

        return null;
    }

    /**
     * Runs a queued callback once loading finished - or its failure handler when
     * the {@link PlayerProfile} could not be loaded at all. Any failure is
     * isolated so the remaining callbacks are still served.
     *
     * @param queuedCallback
     *            The {@link QueuedProfileCallback} to run
     * @param profile
     *            The loaded {@link PlayerProfile}, or null when loading failed
     * @param uuid
     *            The {@link UUID} of the Player (for logging)
     */
    private static void deliver(@Nonnull QueuedProfileCallback queuedCallback, @Nullable PlayerProfile profile, @Nonnull UUID uuid) {
        try {
            if (profile != null) {
                queuedCallback.callback.accept(profile);
            } else if (queuedCallback.failureHandler != null) {
                // Compensation (e.g. refunds) must happen on the main Thread
                Slimefun.runSync(queuedCallback.failureHandler);
            }
        } catch (Exception | LinkageError x) {
            Slimefun.logger().log(Level.SEVERE, x, () -> "A PlayerProfile callback for " + uuid + " threw an exception");
        }
    }

    /**
     * If the Player quit while their profile was still loading asynchronously,
     * the quit listener never saw this profile and could not mark it for
     * deletion - it would stay in memory forever. Check on the main Thread and
     * mark it for deletion if the Player is no longer online.
     *
     * @param uuid
     *            The {@link UUID} of the Player
     * @param profile
     *            The freshly loaded {@link PlayerProfile}
     */
    private static void markForDeletionIfOffline(@Nonnull UUID uuid, @Nonnull PlayerProfile profile) {
        Slimefun.runSync(() -> {
            if (Bukkit.getPlayer(uuid) == null) {
                profile.markForDeletion();
            }
        });
    }

    /**
     * This requests an instance of {@link PlayerProfile} to be loaded for the given {@link OfflinePlayer}.
     * This method will return true if the {@link PlayerProfile} was already found.
     * 
     * @param p
     *            The {@link OfflinePlayer} to request the {@link PlayerProfile} for.
     * 
     * @return Whether the {@link PlayerProfile} was already loaded
     */
    public static boolean request(@Nonnull OfflinePlayer p) {
        Validate.notNull(p, "Cannot request a Profile for null");
        Debug.log(TestCase.PLAYER_PROFILE_DATA, "Requesting PlayerProfile for {}", p.getName());

        UUID uuid = p.getUniqueId();

        if (!Slimefun.getRegistry().getPlayerProfiles().containsKey(uuid)) {
            // If we're already loading, we don't want to spin up a whole new thread and load the profile again/more
            // This can very easily cause CPU, memory and thread exhaustion if the profile is large
            // See #4011, #4116
            List<QueuedProfileCallback> newQueue = new ArrayList<>();

            if (loading.putIfAbsent(uuid, newQueue) != null) {
                Debug.log(TestCase.PLAYER_PROFILE_DATA, "Attempted to request PlayerProfile ({}) while loading", uuid);
                return false;
            }

            Slimefun.getThreadService().newThread(Slimefun.instance(), "PlayerProfile#request(" + uuid + ")", () -> {
                try {
                    PlayerData data = Slimefun.getPlayerStorage().loadPlayerData(uuid);

                    PlayerProfile pp = new PlayerProfile(p, data);
                    Slimefun.getRegistry().getPlayerProfiles().put(uuid, pp);
                    markForDeletionIfOffline(uuid, pp);
                } catch (Exception | LinkageError x) {
                    Slimefun.logger().log(Level.SEVERE, x, () -> "Failed to load the PlayerProfile for " + uuid);
                } finally {
                    // Make sure we remove the loading flag after we put the PlayerProfile into the registry.
                    // Otherwise, we end up with a race condition where the profile is not in the map just _yet_
                    // but the loading flag is gone and we can end up loading it a second time (and thus can dupe items)
                    // Fixes https://github.com/Slimefun/Slimefun4/issues/4130
                    //
                    // The finally block also guarantees the flag is cleared even if
                    // loading failed, otherwise the profile could never be loaded again.
                    loading.remove(uuid, newQueue);
                }
            });

            return false;
        }

        return true;
    }

    /**
     * This method tries to search for a {@link PlayerProfile} of the given {@link OfflinePlayer}.
     * The result of this method is an {@link Optional}, if no {@link PlayerProfile} was found, an empty
     * {@link Optional} will be returned.
     * 
     * @param p
     *            The {@link OfflinePlayer} to get the {@link PlayerProfile} for
     * 
     * @return An {@link Optional} describing the result
     */
    public static @Nonnull Optional<PlayerProfile> find(@Nonnull OfflinePlayer p) {
        return Optional.ofNullable(Slimefun.getRegistry().getPlayerProfiles().get(p.getUniqueId()));
    }

    public static @Nonnull Iterator<PlayerProfile> iterator() {
        return Slimefun.getRegistry().getPlayerProfiles().values().iterator();
    }

    public static void getBackpack(@Nullable ItemStack item, @Nonnull Consumer<PlayerBackpack> callback) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return;
        }

        OptionalInt id = OptionalInt.empty();
        String uuid = "";

        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith(ChatColors.color("&7ID: ")) && line.indexOf('#') != -1) {
                String[] splitLine = CommonPatterns.HASH.split(line);

                // String#split drops trailing empty strings, so a line ending in
                // '#' would leave us with a single element - guard against that
                if (splitLine.length == 2 && CommonPatterns.NUMERIC.matcher(splitLine[1]).matches()) {
                    id = OptionalInt.of(Integer.parseInt(splitLine[1]));
                    uuid = splitLine[0].replace(ChatColors.color("&7ID: "), "");
                }
            }
        }

        if (id.isPresent()) {
            int number = id.getAsInt();
            UUID ownerUuid;

            try {
                ownerUuid = UUID.fromString(uuid);
            } catch (IllegalArgumentException x) {
                // Forged or malformed lore line. Silently ignore.
                return;
            }

            fromUUID(ownerUuid, profile -> {
                Optional<PlayerBackpack> backpack = profile.getBackpack(number);
                backpack.ifPresent(callback);
            });
        }
    }

    public boolean hasFullProtectionAgainst(@Nonnull ProtectionType type) {
        Validate.notNull(type, "ProtectionType must not be null.");

        int armorCount = 0;
        NamespacedKey setId = null;

        for (HashedArmorpiece armorpiece : armor) {
            Optional<SlimefunArmorPiece> armorPiece = armorpiece.getItem();
            if (armorPiece.isPresent() && armorPiece.get() instanceof ProtectiveArmor protectiveArmor) {
                for (ProtectionType protectionType : protectiveArmor.getProtectionTypes()) {
                    if (protectionType == type) {
                        if (!protectiveArmor.isFullSetRequired()) {
                            return true;
                        } else if (setId == null || setId.equals(protectiveArmor.getArmorSetId())) {
                            armorCount++;
                            setId = protectiveArmor.getArmorSetId();
                        }
                    }
                }
            }
        }

        return armorCount == 4;
    }

    public PlayerData getPlayerData() {
        return this.data;
    }

    @Override
    public int hashCode() {
        return ownerId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayerProfile profile && ownerId.equals(profile.ownerId);
    }

    @Override
    public String toString() {
        return "PlayerProfile {" + ownerId + "}";
    }

}

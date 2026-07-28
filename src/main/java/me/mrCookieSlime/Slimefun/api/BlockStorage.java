package me.mrCookieSlime.Slimefun.api;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.bakedlibs.dough.common.CommonPatterns;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalBlockMenu;

// This class really needs a VERY big overhaul
public class BlockStorage {

    private static final String PATH_BLOCKS = "data-storage/Slimefun/stored-blocks/";
    private static final String PATH_CHUNKS = "data-storage/Slimefun/stored-chunks/";
    private static final String PATH_INVENTORIES = "data-storage/Slimefun/stored-inventories/";

    private static final EmptyBlockData emptyBlockData = new EmptyBlockData();

    private final World world;
    private final Map<Location, Config> storage = new ConcurrentHashMap<>();
    private final Map<Location, BlockMenu> inventories = new ConcurrentHashMap<>();

    /*
     * Deferred persistence: block data writes are pure in-memory operations.
     * Locations with modified data are marked dirty and any file-key removals
     * (deleted blocks / id changes) are recorded, so the JSON serialization
     * and the per-id .sfb file update only happen once per save() cycle
     * instead of on every single value change.
     */
    private final Set<Location> dirtyBlocks = ConcurrentHashMap.newKeySet();
    private final Map<Location, Set<String>> pendingFileDeletions = new ConcurrentHashMap<>();
    private final Object persistenceLock = new Object();

    /**
     * Serializes whole {@link #save()} / {@link #saveAndRemove()} invocations against
     * each other (e.g. the asynchronous auto-save vs. a world unload on the main
     * Thread). Writers never take this lock, so holding it across file IO does not
     * stall gameplay code.
     */
    private final Object saveLock = new Object();

    /*
     * A persistent in-memory view of the per-id .sfb files, shared across
     * save cycles. Keeping the parsed file Config around avoids re-parsing
     * every touched .sfb file from disk on every save; the file itself is
     * still only written during save().
     */
    private final Map<String, Config> blockFiles = new ConcurrentHashMap<>();

    private static final AtomicInteger chunkChanges = new AtomicInteger();

    /**
     * Serialises concurrent {@link #saveChunks()} calls: it is invoked both from
     * the asynchronous auto-save Thread and (per unloading World) from the main
     * Thread, and all callers share the same temporary file.
     */
    private static final Object chunkSaveLock = new Object();
    private static boolean universalInventoriesLoaded = false;

    private int changes = 0;
    private AtomicBoolean isMarkedForRemoval = new AtomicBoolean(false);

    @Nullable
    public static BlockStorage getStorage(@Nonnull World world) {
        return Slimefun.getRegistry().getWorlds().get(world.getName());
    }

    @Nonnull
    public static BlockStorage getOrCreate(@Nonnull World world) {
        BlockStorage storage = Slimefun.getRegistry().getWorlds().get(world.getName());

        if (storage != null && storage.isMarkedForRemoval()) {
            /*
             * The World was unloaded and its BlockStorage is only waiting for the
             * next TickerTask cycle to be dropped from the registry. If the World
             * gets loaded again before that happens, reusing this instance would
             * leave the World with a "dead" storage: the ticker drops it on the
             * next run and every write afterwards silently goes nowhere.
             * Evict it (atomically, so a racing removal is fine) and load fresh.
             */
            if (Slimefun.getRegistry().getWorlds().remove(world.getName(), storage)) {
                storage = null;
            } else {
                storage = Slimefun.getRegistry().getWorlds().get(world.getName());
            }
        }

        if (storage == null) {
            BlockStorage fresh = new BlockStorage(world);

            /*
             * The constructor skips its own registration when another Thread
             * registered an instance for this World in the meantime. Always
             * return the actually registered instance, never a half-initialised
             * one that no code path will ever save or load.
             */
            BlockStorage registered = Slimefun.getRegistry().getWorlds().get(world.getName());
            return registered != null ? registered : fresh;
        } else {
            return storage;
        }
    }

    private static String serializeLocation(Location l) {
        return l.getWorld().getName() + ';' + l.getBlockX() + ';' + l.getBlockY() + ';' + l.getBlockZ();
    }

    private static String serializeChunk(World world, int x, int z) {
        return world.getName() + ";Chunk;" + x + ';' + z;
    }

    private static Location deserializeLocation(String l) {
        try {
            String[] components = CommonPatterns.SEMICOLON.split(l);
            if (components.length != 4) {
                return null;
            }

            World w = Bukkit.getWorld(components[0]);

            if (w != null) {
                return new Location(w, Integer.parseInt(components[1]), Integer.parseInt(components[2]), Integer.parseInt(components[3]));
            }
        } catch (NumberFormatException x) {
            Slimefun.logger().log(Level.WARNING, "Could not parse Number", x);
        }
        return null;
    }

    public BlockStorage(World w) {
        this.world = w;

        if (world.getName().indexOf('.') != -1) {
            throw new IllegalArgumentException("Slimefun cannot deal with World names that contain a dot: " + w.getName());
        }

        if (Slimefun.getRegistry().getWorlds().containsKey(w.getName())) {
            // Cancel the loading process if the world was already loaded
            return;
        }

        Slimefun.logger().log(Level.INFO, "Loading Blocks for World \"{0}\"", w.getName());
        Slimefun.logger().log(Level.INFO, "This may take a long time...");

        File dir = new File(PATH_BLOCKS + w.getName());

        if (dir.exists()) {
            loadBlocks(dir);
        } else {
            dir.mkdirs();
        }

        loadChunks();

        // TODO: properly support loading inventories within unit tests
        if (!Slimefun.instance().isUnitTest()) {
            loadInventories();
        }
        Slimefun.getRegistry().getWorlds().put(world.getName(), this);
    }

    private void loadBlocks(File directory) {
        long total = directory.listFiles().length;
        long start = System.currentTimeMillis();
        long done = 0;
        long timestamp = System.currentTimeMillis();
        long totalBlocks = 0;
        int delay = Slimefun.getCfg().getInt("URID.info-delay");

        try {
            for (File file : directory.listFiles()) {
                if (file.getName().equals("null.sfb")) {
                    Slimefun.logger().log(Level.WARNING, "File with corrupted blocks detected!");
                    Slimefun.logger().log(Level.WARNING, "Slimefun will simply skip this File, you should look inside though!");
                    Slimefun.logger().log(Level.WARNING, file.getPath());
                } else if (file.getName().endsWith(".sfb")) {
                    if (timestamp + delay < System.currentTimeMillis()) {
                        int progress = Math.round((((done * 100.0F) / total) * 100.0F) / 100.0F);
                        Slimefun.logger().log(Level.INFO, "Loading Blocks... {0}% done (\"{1}\")", new Object[] { progress, world.getName() });
                        timestamp = System.currentTimeMillis();
                    }

                    FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                    for (String key : cfg.getKeys(false)) {
                        loadBlock(file, cfg, key);
                        totalBlocks++;
                    }

                    done++;
                }
            }
        } finally {
            long time = (System.currentTimeMillis() - start);
            Slimefun.logger().log(Level.INFO, "Loading Blocks... 100% (FINISHED - {0}ms)", time);
            Slimefun.logger().log(Level.INFO, "Loaded a total of {0} Blocks for World \"{1}\"", new Object[] { totalBlocks, world.getName() });

            if (totalBlocks > 0) {
                Slimefun.logger().log(Level.INFO, "Avg: {0}ms/Block", NumberUtils.roundDecimalNumber((double) time / (double) totalBlocks));
            }
        }
    }

    private void loadBlock(File file, FileConfiguration cfg, String key) {
        Location l = deserializeLocation(key);

        if (l == null) {
            // That location was malformed, we will skip this one
            return;
        }

        try {
            String json = cfg.getString(key);
            Config blockInfo = parseBlockInfo(l, json);

            if (blockInfo != null && blockInfo.contains("id")) {
                if (storage.putIfAbsent(l, blockInfo) != null) {
                    /*
                     * It should not be possible to have two blocks on the same location.
                     * Ignore the new entry if a block is already present and print an
                     * error to the console (if enabled).
                     */
                    if (Slimefun.getRegistry().logDuplicateBlockEntries()) {
                        Slimefun.logger().log(Level.INFO, "Ignoring duplicate block @ %d, %d, %d (%s -> %s)".formatted(l.getBlockX(), l.getBlockY(), l.getBlockZ(), blockInfo.getString("id"), storage.get(l).getString("id")));
                    }

                    return;
                }

                String fileName = file.getName().replace(".sfb", "");

                if (Slimefun.getRegistry().getTickerBlocks().contains(fileName)) {
                    Slimefun.getTickerTask().enableTicker(l);
                }
            }
        } catch (Exception x) {
            Slimefun.logger().log(Level.WARNING, x, () -> "Failed to load " + file.getName() + '(' + key + ") for Slimefun " + Slimefun.getVersion());
        }
    }

    private void loadChunks() {
        File chunks = new File(PATH_CHUNKS + "chunks.sfc");

        if (chunks.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(chunks);

            for (String key : cfg.getKeys(false)) {
                try {
                    if (world.getName().equals(CommonPatterns.SEMICOLON.split(key)[0])) {
                        BlockInfoConfig data = new BlockInfoConfig(parseJSON(cfg.getString(key)));
                        Slimefun.getRegistry().getChunks().put(key, data);
                    }
                } catch (Exception x) {
                    Slimefun.logger().log(Level.WARNING, x, () -> "Failed to load " + chunks.getName() + " in World " + world.getName() + '(' + key + ") for Slimefun " + Slimefun.getVersion());
                }
            }
        }
    }

    private void loadInventories() {
        for (File file : new File("data-storage/Slimefun/stored-inventories").listFiles()) {
            if (file.getName().startsWith(world.getName()) && file.getName().endsWith(".sfi")) {
                try {
                    Location l = deserializeLocation(file.getName().replace(".sfi", ""));

                    // We only want to only load this world's menus
                    if (world != l.getWorld()) {
                        continue;
                    }

                    io.github.bakedlibs.dough.config.Config cfg = new io.github.bakedlibs.dough.config.Config(file);
                    BlockMenuPreset preset = BlockMenuPreset.getPreset(cfg.getString("preset"));

                    if (preset == null) {
                        preset = BlockMenuPreset.getPreset(checkID(l));
                    }

                    if (preset != null) {
                        inventories.put(l, new BlockMenu(preset, l, cfg));
                    }
                } catch (Exception x) {
                    Slimefun.logger().log(Level.SEVERE, x, () -> "An Error occurred while loading this Block Inventory: " + file.getName());
                }
            }
        }

        if (universalInventoriesLoaded) {
            return;
        }

        universalInventoriesLoaded = true;

        for (File file : new File("data-storage/Slimefun/universal-inventories").listFiles()) {
            if (file.getName().endsWith(".sfi")) {
                try {
                    io.github.bakedlibs.dough.config.Config cfg = new io.github.bakedlibs.dough.config.Config(file);
                    BlockMenuPreset preset = BlockMenuPreset.getPreset(cfg.getString("preset"));

                    if (preset != null) {
                        Slimefun.getRegistry().getUniversalInventories().put(preset.getID(), new UniversalBlockMenu(preset, cfg));
                    }
                } catch (Exception x) {
                    Slimefun.logger().log(Level.SEVERE, x, () -> "An Error occurred while loading this universal Inventory: " + file.getName());
                }
            }
        }
    }

    public void computeChanges() {
        synchronized (persistenceLock) {
            changes = dirtyBlocks.size() + pendingFileDeletions.size();
        }

        Map<Location, BlockMenu> inventories2 = new HashMap<>(inventories);
        for (Map.Entry<Location, BlockMenu> entry : inventories2.entrySet()) {
            changes += entry.getValue().getUnsavedChanges();
        }

        Map<String, UniversalBlockMenu> universalInventories2 = new HashMap<>(Slimefun.getRegistry().getUniversalInventories());
        for (Map.Entry<String, UniversalBlockMenu> entry : universalInventories2.entrySet()) {
            changes += entry.getValue().getUnsavedChanges();
        }
    }

    public int getChanges() {
        return changes;
    }

    public void save() {
        synchronized (saveLock) {
            computeChanges();

            if (changes == 0) {
                return;
            }

            Slimefun.logger().log(Level.INFO, "Saving block data for world \"{0}\" ({1} change(s) queued)", new Object[] { world.getName(), changes });

            /*
             * Drain the pending writes under a lock so no deletion can be lost to a
             * concurrent write, then serialize everything outside the lock.
             * Writers that come in after the drain simply land in the next save cycle.
             */
            Map<Location, Set<String>> deletions;
            Set<Location> dirty;

            synchronized (persistenceLock) {
                deletions = new HashMap<>(pendingFileDeletions);
                pendingFileDeletions.clear();
                dirty = new HashSet<>(dirtyBlocks);
                dirtyBlocks.clear();
            }

            // The per-id .sfb file views are cached persistently (see blockFiles);
            // only the ids touched in this cycle are written back to disk.
            Set<String> touchedIds = new HashSet<>();

            // 1. File-key removals first (a deleted or re-ided block may be re-written below)
            for (Map.Entry<Location, Set<String>> entry : deletions.entrySet()) {
                String serializedLocation = serializeLocation(entry.getKey());

                for (String id : entry.getValue()) {
                    getBlockFile(id).setValue(serializedLocation, null);
                    touchedIds.add(id);
                }
            }

            // 2. Serialize all dirty blocks (the live storage map is the source of truth)
            Map<String, Set<Location>> locationsById = new HashMap<>();

            for (Location l : dirty) {
                Config cfg = storage.get(l);

                if (cfg == null) {
                    // The block was deleted again after being marked dirty,
                    // its removal is handled by the deletions above (if any).
                    continue;
                }

                String id = cfg.getString("id");

                if (id == null) {
                    /*
                     * This Block is no longer valid...
                     * Fixes #1577
                     */
                    continue;
                }

                getBlockFile(id).setValue(serializeLocation(l), serializeBlockInfo(cfg));
                touchedIds.add(id);
                locationsById.computeIfAbsent(id, key -> new HashSet<>()).add(l);
            }

            // 3. Write every touched file (atomically) or delete it if no keys remain
            for (String id : touchedIds) {
                Config cfg = blockFiles.get(id);

                if (cfg == null) {
                    // Cannot happen, but stay defensive.
                    continue;
                }

                if (cfg.getKeys().isEmpty()) {
                    /*
                     * Drop the cached view as well, otherwise a block of this id
                     * added later would resurrect stale keys into a fresh file.
                     */
                    blockFiles.remove(id);

                    File file = cfg.getFile();

                    if (file.exists()) {
                        try {
                            Files.delete(file.toPath());
                        } catch (IOException e) {
                            Slimefun.logger().log(Level.WARNING, e, () -> "Could not delete file \"" + file.getName() + '"');
                        }
                    }
                } else if (!writeBlockFile(id, cfg)) {
                    /*
                     * The write failed (e.g. disk full). Re-queue everything we just
                     * drained for this id, otherwise the in-memory view and the file
                     * on disk would diverge permanently without any retry.
                     */
                    requeueFailedChanges(id, deletions, locationsById.get(id));
                }
            }

            Map<Location, BlockMenu> unsavedInventories = new HashMap<>(inventories);
            for (Map.Entry<Location, BlockMenu> entry : unsavedInventories.entrySet()) {
                /*
                 * Skip menus that were removed after the snapshot was taken (e.g. the
                 * machine was broken in the meantime) - re-saving them would recreate
                 * an orphaned .sfi file whose contents would "resurrect" later.
                 */
                if (inventories.get(entry.getKey()) == entry.getValue()) {
                    entry.getValue().save(entry.getKey());
                }
            }

            Map<String, UniversalBlockMenu> unsavedUniversalInventories = new HashMap<>(Slimefun.getRegistry().getUniversalInventories());
            for (Map.Entry<String, UniversalBlockMenu> entry : unsavedUniversalInventories.entrySet()) {
                entry.getValue().save();
            }

            changes = 0;
        }
    }

    /**
     * Writes the cached .sfb file view for one item id to disk, via a temporary
     * file and an atomic move (with a plain replace as fallback for filesystems
     * that do not support atomic moves).
     *
     * @param id
     *            The item id (for logging)
     * @param cfg
     *            The cached file view
     *
     * @return Whether the file was successfully written
     */
    private boolean writeBlockFile(@Nonnull String id, @Nonnull Config cfg) {
        File target = cfg.getFile();
        File tmpFile = new File(target.getParentFile(), target.getName() + ".tmp");

        cfg.save(tmpFile);

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
                Slimefun.logger().log(Level.SEVERE, x2, () -> "An Error occurred while saving block data for id \"" + id + "\", will retry on the next save cycle");
                return false;
            }
        }
    }

    /**
     * Puts the changes for one item id back into the pending queues after a
     * failed write, so the next save cycle retries them instead of losing them.
     */
    @ParametersAreNonnullByDefault
    private void requeueFailedChanges(String id, Map<Location, Set<String>> deletions, Set<Location> dirtyLocations) {
        synchronized (persistenceLock) {
            if (dirtyLocations != null) {
                dirtyBlocks.addAll(dirtyLocations);
            }

            for (Map.Entry<Location, Set<String>> entry : deletions.entrySet()) {
                if (entry.getValue().contains(id)) {
                    pendingFileDeletions.computeIfAbsent(entry.getKey(), key -> ConcurrentHashMap.newKeySet()).add(id);
                }
            }
        }
    }

    /**
     * This returns the in-memory view of the per-id .sfb file for the given item id,
     * loading it from disk (and creating the parent directory) on first access.
     * The view is cached persistently in {@link #blockFiles}, so later save
     * cycles do not have to re-parse the file from disk.
     *
     * @param id
     *            The {@link SlimefunItem} id whose .sfb file to load
     *
     * @return The file {@link Config} for that id
     */
    @Nonnull
    private Config getBlockFile(@Nonnull String id) {
        return blockFiles.computeIfAbsent(id, key -> {
            File dir = new File(PATH_BLOCKS + world.getName());
            dir.mkdirs();
            return new Config(PATH_BLOCKS + world.getName() + '/' + key + ".sfb");
        });
    }

    public void saveAndRemove() {
        synchronized (saveLock) {
            save();
            blockFiles.clear();
            saveChunks();
            isMarkedForRemoval.set(true);
        }
    }

    public boolean isMarkedForRemoval() {
        return isMarkedForRemoval.get();
    }

    public static void saveChunks() {
        synchronized (chunkSaveLock) {
            /*
             * Reset the counter up front: writes that happen while we are
             * saving must count towards the NEXT cycle, not be swallowed
             * by this one.
             */
            if (chunkChanges.getAndSet(0) <= 0) {
                return;
            }

            File chunks = new File(PATH_CHUNKS + "chunks.sfc");
            File tmpFile = new File(PATH_CHUNKS + "chunks.temp");
            Config cfg = new Config(tmpFile);

            for (Map.Entry<String, BlockInfoConfig> entry : Slimefun.getRegistry().getChunks().entrySet()) {
                // Saving empty chunk data is pointless
                if (!entry.getValue().getKeys().isEmpty()) {
                    cfg.setValue(entry.getKey(), entry.getValue().toJSON());
                }
            }

            // Write to a temporary file first, then move it into place
            cfg.save();

            try {
                Files.move(tmpFile.toPath(), chunks.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException x) {
                try {
                    Files.move(tmpFile.toPath(), chunks.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException x2) {
                    Slimefun.logger().log(Level.SEVERE, x2, () -> "An Error occurred while saving chunk data for Slimefun " + Slimefun.getVersion());
                }
            }
        }
    }

    /**
     * This will return an {@link ImmutableMap} of the underline {@code Map<String, Config>} of
     * this worlds {@link BlockStorage}.
     *
     * @return An {@link ImmutableMap} of the raw data.
     */
    @Nonnull
    public Map<Location, Config> getRawStorage() {
        return ImmutableMap.copyOf(this.storage);
    }

    /**
     * This will return an {@link ImmutableMap} of the underline {@code Map<String, Config>} of
     * this worlds {@link BlockStorage}. If there is no registered world then this will return null.
     *
     * @param world
     *            The world of which to fetch the data from.
     * @return An {@link ImmutableMap} of the raw data or null if the world isn't registered.
     */
    @Nullable
    public static Map<Location, Config> getRawStorage(@Nonnull World world) {
        Validate.notNull(world, "World cannot be null!");

        BlockStorage storage = getStorage(world);
        if (storage != null) {
            return storage.getRawStorage();
        } else {
            return null;
        }
    }

    public static void store(Block block, ItemStack item) {
        SlimefunItem sfitem = SlimefunItem.getByItem(item);

        if (sfitem != null) {
            addBlockInfo(block, "id", sfitem.getId(), true);
        }
    }

    public static void store(Block block, String item) {
        addBlockInfo(block, "id", item, true);
    }

    /**
     * Retrieves the SlimefunItem's ItemStack from the specified Block.
     * If the specified Block is registered in BlockStorage,
     * its data will be erased from it, regardless of the returned value.
     *
     * @param block
     *            the block to retrieve the ItemStack from
     * 
     * @return the SlimefunItem's ItemStack corresponding to the block if it has one, otherwise null
     */
    @Nullable
    public static ItemStack retrieve(@Nonnull Block block) {
        SlimefunItem item = check(block);

        if (item == null) {
            return null;
        } else {
            clearBlockInfo(block);
            return item.getItem();
        }
    }

    @Nonnull
    public static Config getLocationInfo(Location l) {
        BlockStorage storage = getStorage(l.getWorld());

        if (storage == null) {
            return emptyBlockData;
        }

        Config cfg = storage.storage.get(l);
        return cfg == null ? emptyBlockData : cfg;
    }

    @Nonnull
    private static Map<String, String> parseJSON(String json) {
        Map<String, String> map = new HashMap<>();

        if (json != null && json.length() > 2) {
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(json).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        return map;
    }

    private static BlockInfoConfig parseBlockInfo(Location l, String json) {
        try {
            return new BlockInfoConfig(parseJSON(json));
        } catch (Exception x) {
            Logger logger = Slimefun.logger();
            logger.log(Level.WARNING, x.getClass().getName());
            logger.log(Level.WARNING, "Failed to parse BlockInfo for Block @ {0}, {1}, {2}", new Object[] { l.getBlockX(), l.getBlockY(), l.getBlockZ() });
            logger.log(Level.WARNING, json);
            logger.log(Level.WARNING, "");
            logger.log(Level.WARNING, "IGNORE THIS ERROR UNLESS IT IS SPAMMING");
            logger.log(Level.WARNING, "");
            logger.log(Level.SEVERE, x, () -> "An Error occurred while parsing Block Info for Slimefun " + Slimefun.getVersion());
            return null;
        }
    }

    private static String serializeBlockInfo(Config cfg) {
        StringWriter string = new StringWriter();

        try (JsonWriter writer = new JsonWriter(string)) {
            writer.setLenient(true);
            writer.beginObject();

            for (String key : cfg.getKeys()) {
                String value = cfg.getString(key);

                /*
                 * The value may have been removed concurrently between the keySet
                 * snapshot and this read - never write a JSON null, it would fail
                 * to parse back (and take the whole block's data down with it).
                 */
                if (value != null) {
                    writer.name(key).value(value);
                }
            }

            writer.endObject();
            return string.toString();
        } catch (IOException x) {
            Slimefun.logger().log(Level.SEVERE, "An error occurred while serializing BlockInfo", x);
            return null;
        }
    }

    public static String getLocationInfo(Location l, String key) {
        return getLocationInfo(l).getString(key);
    }

    public static String getLocationInfo(BlockPosition l, String key) {
        return getLocationInfo(l.toLocation()).getString(key);
    }

    public static void addBlockInfo(Location l, String key, String value) {
        addBlockInfo(l, key, value, false);
    }

    public static void addBlockInfo(BlockPosition l, String key, String value) {
        addBlockInfo(l.toLocation(), key, value, false);
    }

    public static void addBlockInfo(Block block, String key, String value) {
        addBlockInfo(block.getLocation(), key, value);
    }

    public static void addBlockInfo(Block block, String key, String value, boolean updateTicker) {
        addBlockInfo(block.getLocation(), key, value, updateTicker);
    }

    public static void addBlockInfo(BlockPosition l, String key, String value, boolean updateTicker) {
        addBlockInfo(l.toLocation(), key, value, updateTicker);
    }

    public static void addBlockInfo(Location l, String key, String value, boolean updateTicker) {
        Config cfg = getLocationInfo(l);

        if (cfg == emptyBlockData) {
            cfg = new BlockInfoConfig();
        }

        cfg.setValue(key, value);
        setBlockInfo(l, cfg, updateTicker);
    }

    public static boolean hasBlockInfo(Block block) {
        return hasBlockInfo(block.getLocation());
    }

    public static boolean hasBlockInfo(Location l) {
        BlockStorage storage = getStorage(l.getWorld());

        if (storage != null) {
            Config cfg = storage.storage.get(l);
            return cfg != null && cfg.getString("id") != null;
        } else {
            return false;
        }
    }

    private static void setBlockInfo(Location l, Config cfg, boolean updateTicker) {
        BlockStorage storage = getStorage(l.getWorld());

        if (storage == null) {
            Slimefun.logger().warning("Could not set Block info for non-registered World '" + l.getWorld().getName() + "'. Is some plugin trying to store data in a fake world?");
            return;
        }

        Config previous = storage.storage.put(l, cfg);
        String id = cfg.getString("id");
        boolean idChanged = previous != null && !Objects.equals(previous.getString("id"), id);

        if (previous == null || idChanged) {
            if (idChanged) {
                /*
                 * The block id changed in place (e.g. via #store(...) without a
                 * prior delete): make sure the old id's .sfb file drops this
                 * location, otherwise a stale duplicate entry would survive the
                 * next save and could "revive" the old block on the next load.
                 */
                storage.markForFileDeletion(l, previous.getString("id"));
            }

            // Menu Presets never change at runtime, so the (comparatively costly)
            // preset lookup and inventory setup only needs to happen for newly
            // stored blocks or when the id actually changed.
            BlockMenuPreset preset = BlockMenuPreset.getPreset(id);

            if (preset != null) {
                if (BlockMenuPreset.isUniversalInventory(id)) {
                    Slimefun.getRegistry().getUniversalInventories().computeIfAbsent(id, key -> new UniversalBlockMenu(preset));
                } else if (!storage.hasInventory(l)) {
                    File file = new File(PATH_INVENTORIES + serializeLocation(l) + ".sfi");

                    if (file.exists()) {
                        BlockMenu inventory = new BlockMenu(preset, l, new io.github.bakedlibs.dough.config.Config(file));
                        storage.inventories.put(l, inventory);
                    } else {
                        storage.loadInventory(l, preset);
                    }
                }
            }
        }

        if (updateTicker && id != null) {
            SlimefunItem item = SlimefunItem.getById(id);

            if (item != null
                && l.getWorld() != null
                && item.isTicking()
                && !item.isDisabledIn(l.getWorld())
            ) {
                Slimefun.getTickerTask().enableTicker(l);
            }
        }

        // The expensive JSON serialization + file update is deferred to save()
        storage.dirtyBlocks.add(l);
    }

    public static void setBlockInfo(Block b, String json, boolean updateTicker) {
        setBlockInfo(b.getLocation(), json, updateTicker);
    }

    public static void setBlockInfo(Location l, String json, boolean updateTicker) {
        Config blockInfo = json == null ? new BlockInfoConfig() : parseBlockInfo(l, json);

        if (blockInfo == null) {
            return;
        }

        setBlockInfo(l, blockInfo, updateTicker);
    }

    public static void clearBlockInfo(Block block) {
        clearBlockInfo(block.getLocation());
    }

    public static void clearBlockInfo(Location l) {
        clearBlockInfo(l, true);
    }

    public static void clearBlockInfo(Block b, boolean destroy) {
        clearBlockInfo(b.getLocation(), destroy);
    }

    public static void clearBlockInfo(Location l, boolean destroy) {
        Slimefun.getTickerTask().queueDelete(l, destroy);
    }

    public static void clearAllBlockInfoAtChunk(Chunk chunk, boolean destroy) {
        clearAllBlockInfoAtChunk(chunk.getWorld(), chunk.getX(), chunk.getZ(), destroy);
    }

    public static void clearAllBlockInfoAtChunk(World world, int chunkX, int chunkZ, boolean destroy) {
        BlockStorage blockStorage = getStorage(world);
        if (blockStorage == null) {
            return;
        }
        Map<Location, Boolean> toClear = new HashMap<>();
        // Unsafe: get raw storage for this world
        for (Location location : blockStorage.storage.keySet()) {
            if (location.getBlockX() >> 4 == chunkX && location.getBlockZ() >> 4 == chunkZ) {
                toClear.put(location, destroy);
            }
        }
        Slimefun.getTickerTask().queueDelete(toClear);
    }

    /**
     * <strong>Do not call this method!</strong>.
     * This method is used for internal purposes only.
     * 
     * @param l
     *            The {@link Location}
     * @param destroy
     *            Whether to completely destroy the block data
     */
    public static void deleteLocationInfoUnsafely(Location l, boolean destroy) {
        BlockStorage storage = getStorage(l.getWorld());

        if (storage == null) {
            /*
             * The World was already unloaded (and its BlockStorage removed).
             * There is nothing left to delete - the data on disk was saved
             * during unload. Throwing here would kill the caller's queue
             * processing, so just warn and move on.
             */
            Slimefun.logger().log(Level.WARNING, "Could not delete block data @ {0}: World \"{1}\" is no longer loaded", new Object[] { new BlockPosition(l), l.getWorld().getName() });
            return;
        }

        // Read the block data once and reuse it, instead of looking it up
        // separately via hasBlockInfo(...) and getLocationInfo(...).
        Config cfg = storage.storage.get(l);

        if (cfg != null && cfg.getString("id") != null) {
            /*
             * All three steps under one lock: save() drains the pending
             * deletions under the same monitor, so it can never observe a
             * half-deleted state (file-deletion recorded but data still
             * present) and write the deleted block back to disk as a ghost.
             */
            synchronized (storage.persistenceLock) {
                storage.markForFileDeletion(l, cfg.getString("id"));
                storage.dirtyBlocks.remove(l);
                storage.storage.remove(l);
            }
        }

        if (destroy) {
            if (storage.hasInventory(l)) {
                storage.clearInventory(l);
            }

            UniversalBlockMenu universalInventory = getUniversalInventory(l);

            if (universalInventory != null) {
                universalInventory.close();
                universalInventory.save();
            }

            Slimefun.getTickerTask().disableTicker(l);
        }
    }

    @ParametersAreNonnullByDefault
    public static void moveBlockInfo(Location from, Location to) {
        Slimefun.getTickerTask().queueMove(from, to);
    }

    /**
     * <strong>Do not call this method!</strong>.
     * This method is used for internal purposes only.
     * 
     * @param from
     *            The origin {@link Location}
     * @param to
     *            The destination {@link Location}
     */
    @ParametersAreNonnullByDefault
    public static void moveLocationInfoUnsafely(Location from, Location to) {
        // Read the block data once and reuse it, instead of looking it up
        // separately via hasBlockInfo(...) and getLocationInfo(...).
        BlockStorage storage = getStorage(from.getWorld());
        Config previousData = storage == null ? null : storage.storage.get(from);

        if (previousData == null || previousData.getString("id") == null) {
            return;
        }

        setBlockInfo(to, previousData, true);

        if (storage.inventories.containsKey(from)) {
            BlockMenu menu = storage.inventories.get(from);
            storage.inventories.put(to, menu);
            storage.clearInventory(from);
            menu.move(to);
        }

        // Under one lock: same half-deleted-state protection as in
        // deleteLocationInfoUnsafely(...) above
        synchronized (storage.persistenceLock) {
            storage.markForFileDeletion(from, previousData.getString("id"));
            storage.dirtyBlocks.remove(from);
            storage.storage.remove(from);
        }

        Slimefun.getTickerTask().disableTicker(from);
    }

    /**
     * This records that the given {@link Location} must be removed from the
     * .sfb file of the given item id on the next {@link #save()}.
     * The removal is deferred so that high-frequency writes stay pure
     * in-memory operations.
     *
     * @param l
     *            The {@link Location} whose file entry should be removed
     * @param id
     *            The item id under which the entry was stored, null is ignored
     */
    private void markForFileDeletion(@Nonnull Location l, @Nullable String id) {
        if (id == null) {
            return;
        }

        synchronized (persistenceLock) {
            pendingFileDeletions.computeIfAbsent(l, k -> ConcurrentHashMap.newKeySet()).add(id);
        }
    }

    @Nullable
    public static SlimefunItem check(@Nonnull Block b) {
        String id = checkID(b);
        return id == null ? null : SlimefunItem.getById(id);
    }

    @Nullable
    public static SlimefunItem check(@Nonnull Location l) {
        String id = checkID(l);
        return id == null ? null : SlimefunItem.getById(id);
    }

    public static boolean check(Block block, String slimefunItem) {
        String id = checkID(block);
        return id != null && id.equals(slimefunItem);
    }

    @Nullable
    public static String checkID(@Nonnull Block b) {
        // Only access the BlockState when on the main thread
        if (Bukkit.isPrimaryThread() && Slimefun.getBlockDataService().isTileEntity(b.getType())) {
            Optional<String> blockData = Slimefun.getBlockDataService().getBlockData(b);

            if (blockData.isPresent()) {
                return blockData.get();
            }
        }

        return checkID(b.getLocation());
    }

    @Nullable
    public static String checkID(@Nonnull Location l) {
        return getLocationInfo(l, "id");
    }

    public static boolean check(@Nonnull Location l, @Nullable String slimefunItem) {
        if (slimefunItem == null) {
            return false;
        }

        String id = checkID(l);
        return id != null && id.equals(slimefunItem);
    }

    public static boolean isWorldLoaded(@Nonnull World world) {
        return Slimefun.getRegistry().getWorlds().containsKey(world.getName());
    }

    public BlockMenu loadInventory(Location l, BlockMenuPreset preset) {
        if (preset == null) {
            return null;
        }

        BlockMenu menu = new BlockMenu(preset, l);
        inventories.put(l, menu);
        return menu;
    }

    /**
     * Reload a BlockMenu based on the preset. This method is solely for if you wish to reload
     * based on data from the preset.
     *
     * @param l
     *            The location of the Block.
     */
    public void reloadInventory(Location l) {
        BlockMenu menu = this.inventories.get(l);

        if (menu != null) {
            menu.reload();
        }
    }

    public void clearInventory(Location l) {
        BlockMenu menu = getInventory(l);

        if (menu != null) {
            for (HumanEntity human : new ArrayList<>(menu.toInventory().getViewers())) {
                // Prevents "java.lang.IllegalStateException: Asynchronous entity add!"
                // when closing the inventory while holding an item
                Slimefun.runSync(human::closeInventory);
            }

            inventories.get(l).delete(l);
            inventories.remove(l);
        }
    }

    public boolean hasInventory(Location l) {
        return inventories.containsKey(l);
    }

    public static boolean hasUniversalInventory(String id) {
        return Slimefun.getRegistry().getUniversalInventories().containsKey(id);
    }

    public static UniversalBlockMenu getUniversalInventory(Block block) {
        return getUniversalInventory(block.getLocation());
    }

    public static UniversalBlockMenu getUniversalInventory(Location l) {
        String id = checkID(l);
        return id == null ? null : getUniversalInventory(id);
    }

    public static UniversalBlockMenu getUniversalInventory(String id) {
        return Slimefun.getRegistry().getUniversalInventories().get(id);
    }

    public static BlockMenu getInventory(Block b) {
        return getInventory(b.getLocation());
    }

    public static boolean hasInventory(Block b) {
        BlockStorage storage = getStorage(b.getWorld());

        if (storage == null) {
            return false;
        } else {
            return storage.hasInventory(b.getLocation());
        }
    }

    public static BlockMenu getInventory(Location l) {
        BlockStorage storage = getStorage(l.getWorld());

        if (storage == null) {
            return null;
        }

        BlockMenu menu = storage.inventories.get(l);

        if (menu != null) {
            return menu;
        } else {
            return storage.loadInventory(l, BlockMenuPreset.getPreset(checkID(l)));
        }
    }

    public static Config getChunkInfo(World world, int x, int z) {
        try {
            if (!isWorldLoaded(world)) {
                return emptyBlockData;
            }

            String key = serializeChunk(world, x, z);
            Map<String, BlockInfoConfig> chunks = Slimefun.getRegistry().getChunks();
            BlockInfoConfig cfg = chunks.get(key);

            if (cfg == null) {
                // Atomic: two Threads must never end up writing to two
                // different instances where only one is actually stored
                BlockInfoConfig fresh = new BlockInfoConfig();
                BlockInfoConfig existing = chunks.putIfAbsent(key, fresh);
                cfg = existing != null ? existing : fresh;
            }

            return cfg;
        } catch (Exception e) {
            Slimefun.logger().log(Level.SEVERE, e, () -> "Failed to parse ChunkInfo for Slimefun " + Slimefun.getVersion());
            return emptyBlockData;
        }
    }

    public static void setChunkInfo(World world, int x, int z, String key, String value) {
        String serializedChunk = serializeChunk(world, x, z);
        Map<String, BlockInfoConfig> chunks = Slimefun.getRegistry().getChunks();
        BlockInfoConfig cfg = chunks.get(serializedChunk);

        if (cfg == null) {
            // Atomic: see getChunkInfo(...)
            BlockInfoConfig fresh = new BlockInfoConfig();
            BlockInfoConfig existing = chunks.putIfAbsent(serializedChunk, fresh);
            cfg = existing != null ? existing : fresh;
        }

        cfg.setValue(key, value);

        chunkChanges.incrementAndGet();
    }

    public static boolean hasChunkInfo(World world, int x, int z) {
        String serializedChunk = serializeChunk(world, x, z);
        return Slimefun.getRegistry().getChunks().containsKey(serializedChunk);
    }

    public static String getChunkInfo(World world, int x, int z, String key) {
        return getChunkInfo(world, x, z).getString(key);
    }

    public static String getBlockInfoAsJson(Block block) {
        return getBlockInfoAsJson(block.getLocation());
    }

    public static String getBlockInfoAsJson(Location l) {
        return serializeBlockInfo(getLocationInfo(l));
    }

    public boolean hasUniversalInventory(Block block) {
        return hasUniversalInventory(block.getLocation());
    }

    public boolean hasUniversalInventory(Location l) {
        String id = checkID(l);
        return id != null && hasUniversalInventory(id);
    }
}

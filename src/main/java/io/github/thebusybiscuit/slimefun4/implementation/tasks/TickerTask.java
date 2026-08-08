package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitScheduler;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.bakedlibs.dough.blocks.ChunkPosition;
import io.github.thebusybiscuit.slimefun4.api.ErrorReport;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunMachineCrashEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * The {@link TickerTask} is responsible for ticking every {@link BlockTicker},
 * synchronous or not.
 * 
 * @author TheBusyBiscuit
 * 
 * @see BlockTicker
 *
 */
public class TickerTask implements Runnable {

    /**
     * This Map holds all currently actively ticking locations.
     * The value of this map (Set entries) MUST be thread-safe and mutable.
     */
    private final Map<ChunkPosition, Set<Location>> tickingLocations = new ConcurrentHashMap<>();

    // These are "Queues" of blocks that need to be removed or moved
    private final Map<Location, Location> movingQueue = new ConcurrentHashMap<>();
    private final Map<Location, Boolean> deletionQueue = new ConcurrentHashMap<>();

    /**
     * This Map tracks how many bugs have occurred in a given Location .
     * If too many bugs happen, we delete that Location.
     */
    private final Map<BlockPosition, Integer> bugs = new ConcurrentHashMap<>();

    private int tickRate;
    private volatile boolean halted = false;

    /*
     * Bukkit may overlap executions of an asynchronous timer task whose
     * previous run overran its period. This flag must therefore be flipped
     * atomically (compareAndSet), otherwise two Threads could both pass the
     * check and tick the same blocks concurrently.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * A buffered synchronized tick. Holds everything needed to run the synchronized
     * part of a {@link BlockTicker} on the main Thread later.
     */
    private static final class SynchronizedTick {

        private final Location location;
        private final SlimefunItem item;
        private final Config data;

        SynchronizedTick(@Nonnull Location location, @Nonnull SlimefunItem item, @Nonnull Config data) {
            this.location = location;
            this.item = item;
            this.data = data;
        }
    }

    /**
     * This method starts the {@link TickerTask} on an asynchronous schedule.
     * 
     * @param plugin
     *            The instance of our {@link Slimefun}
     */
    public void start(@Nonnull Slimefun plugin) {
        this.tickRate = Slimefun.getCfg().getInt("URID.custom-ticker-delay");

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, this, 100L, tickRate);
    }

    /**
     * This method resets this {@link TickerTask} to run again.
     */
    private void reset() {
        running.set(false);
    }

    @Override
    public void run() {
        // If this method is actually still running... DON'T
        if (!running.compareAndSet(false, true)) {
            return;
        }

        long tickStart = System.nanoTime();

        try {
            Slimefun.getProfiler().start();
            Set<BlockTicker> tickers = new HashSet<>();
            List<SynchronizedTick> synchronizedTicks = new ArrayList<>();

            // Remove any deleted blocks
            Iterator<Map.Entry<Location, Boolean>> removals = deletionQueue.entrySet().iterator();
            while (removals.hasNext()) {
                Map.Entry<Location, Boolean> entry = removals.next();

                /*
                 * Isolate failures to the single entry: deleteLocationInfoUnsafely()
                 * throws for Locations whose World was already unloaded. Without a
                 * per-entry guard the exception would abort this whole run() - and
                 * since the offending entry is never removed, every following run
                 * would die on the same entry, permanently stalling all ticking.
                 */
                try {
                    BlockStorage.deleteLocationInfoUnsafely(entry.getKey(), entry.getValue());
                } catch (Exception | LinkageError x) {
                    Slimefun.logger().log(Level.WARNING, x, () -> "Could not delete block data @ " + new BlockPosition(entry.getKey()) + ", dropping the queue entry");
                }

                removals.remove();
            }

            // Fixes #2576 - Remove any deleted instances of BlockStorage
            Slimefun.getRegistry().getWorlds().values().removeIf(BlockStorage::isMarkedForRemoval);

            // Run our ticker code
            if (!halted) {
                for (Map.Entry<ChunkPosition, Set<Location>> entry : tickingLocations.entrySet()) {
                    tickChunk(entry.getKey(), tickers, entry.getValue(), synchronizedTicks);
                }
            }

            // Move any moved block data
            Iterator<Map.Entry<Location, Location>> moves = movingQueue.entrySet().iterator();
            while (moves.hasNext()) {
                Map.Entry<Location, Location> entry = moves.next();

                // Same per-entry isolation as the deletion queue above
                try {
                    BlockStorage.moveLocationInfoUnsafely(entry.getKey(), entry.getValue());
                } catch (Exception | LinkageError x) {
                    Slimefun.logger().log(Level.WARNING, x, () -> "Could not move block data @ " + new BlockPosition(entry.getKey()) + ", dropping the queue entry");
                }

                moves.remove();
            }

            /*
             * Run all synchronized ticks in a single scheduler submission instead of
             * one submission per block. The relative order of blocks is preserved,
             * each block still gets its own timestamp and its own try/catch in tickBlock().
             */
            if (!synchronizedTicks.isEmpty()) {
                Slimefun.runSync(() -> {
                    for (SynchronizedTick tick : synchronizedTicks) {
                        /**
                         * We are inserting a new timestamp because synchronized actions
                         * are always ran with a 50ms delay (1 game tick)
                         */
                        Block b = tick.location.getBlock();
                        tickBlock(tick.location, b, tick.item, tick.data, System.nanoTime());
                    }
                });
            }

            // Start a new tick cycle for every BlockTicker
            for (BlockTicker ticker : tickers) {
                ticker.startNewTick();
            }

            reset();
        } catch (Exception | LinkageError x) {
            Slimefun.logger().log(Level.SEVERE, x, () -> "An Exception was caught while ticking the Block Tickers Task for Slimefun v" + Slimefun.getVersion());
            reset();
        } finally {
            // Records the tick's total elapsed time every tick (keeps the timings
            // placeholder current) and resolves any pending /sf timings summary.
            // Placed in finally so an exception mid-tick still records timing and
            // clears the per-block collection state.
            Slimefun.getProfiler().endTick(System.nanoTime() - tickStart);
        }
    }

    @ParametersAreNonnullByDefault
    private void tickChunk(ChunkPosition chunk, Set<BlockTicker> tickers, Set<Location> locations, List<SynchronizedTick> synchronizedTicks) {
        try {
            // Only continue if the Chunk is actually loaded
            if (chunk.isLoaded()) {
                // Resolve the world's BlockStorage once per chunk instead of on every block.
                BlockStorage storage = BlockStorage.getStorage(chunk.getWorld());

                for (Location l : locations) {
                    tickLocation(tickers, l, synchronizedTicks, storage);
                }
            }
        } catch (IllegalStateException x) {
            /*
             * The ChunkPosition's WeakReference to its World was garbage collected.
             * The entry is dead weight (its Locations can never tick again), so
             * drop it instead of letting it kill every following tick cycle.
             */
            if (tickingLocations.remove(chunk, locations)) {
                Slimefun.logger().log(Level.WARNING, x, () -> "Removed a ticking chunk whose World is no longer available: " + chunk);
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException x) {
            Slimefun.logger().log(Level.SEVERE, x, () -> "An Exception has occurred while trying to resolve Chunk: " + chunk);
        }
    }

    private void tickLocation(@Nonnull Set<BlockTicker> tickers, @Nonnull Location l, @Nonnull List<SynchronizedTick> synchronizedTicks, @Nullable BlockStorage storage) {
        // Reuse the per-chunk-resolved BlockStorage when available to skip the world lookup
        // that the single-argument getLocationInfo performs on every block.
        Config data = storage != null ? BlockStorage.getLocationInfo(l, storage) : BlockStorage.getLocationInfo(l);
        SlimefunItem item = SlimefunItem.getById(data.getString("id"));

        if (item == null) {
            return;
        }

        BlockTicker blockTicker = item.getBlockTicker();

        if (blockTicker != null) {
            try {
                if (blockTicker.isSynchronized()) {
                    Slimefun.getProfiler().scheduleEntries(1);
                    blockTicker.update();

                    // Buffered: all synchronized blocks are ticked in a single scheduler
                    // submission at the end of this run (see run()).
                    synchronizedTicks.add(new SynchronizedTick(l, item, data));
                } else {
                    long timestamp = Slimefun.getProfiler().newEntry();
                    blockTicker.update();
                    Block b = l.getBlock();
                    tickBlock(l, b, item, data, timestamp);
                }

                tickers.add(blockTicker);
            } catch (Exception x) {
                reportErrors(l, item, x);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void tickBlock(Location l, Block b, SlimefunItem item, Config data, long timestamp) {
        try {
            item.getBlockTicker().tick(b, item, data);
        } catch (Exception | LinkageError x) {
            reportErrors(l, item, x);
        } finally {
            Slimefun.getProfiler().closeEntry(l, item, timestamp);
        }
    }

    @ParametersAreNonnullByDefault
    private void reportErrors(Location l, SlimefunItem item, Throwable x) {
        BlockPosition position = new BlockPosition(l);

        // Atomically increment: reportErrors runs on BOTH the async ticker thread and the main
        // thread (synchronized ticks), so a non-atomic getOrDefault+put could drop updates (a
        // machine never reaching the 4-error termination threshold) or generate duplicate
        // ErrorReports for error #1.
        int errors = bugs.merge(position, 1, Integer::sum);

        if (errors == 1) {
            // Generate a new Error-Report
            new ErrorReport<>(x, l, item);
        } else if (errors == 4) {
            if (SlimefunMachineCrashEvent.getHandlerList().getRegisteredListeners().length > 0) {
                SlimefunMachineCrashEvent crashEvent = new SlimefunMachineCrashEvent(l, item);
                Bukkit.getPluginManager().callEvent(crashEvent);

                if (crashEvent.isCancelled()) {
                    // An addon chose to spare this machine; it stays broken but is not destroyed.
                    return;
                }
            }

            Slimefun.logger().log(Level.SEVERE, "X: {0} Y: {1} Z: {2} ({3})", new Object[] { l.getBlockX(), l.getBlockY(), l.getBlockZ(), item.getId() });
            Slimefun.logger().log(Level.SEVERE, "has thrown 4 error messages in the last 4 Ticks, the Block has been terminated.");
            Slimefun.logger().log(Level.SEVERE, "Check your /plugins/Slimefun/error-reports/ folder for details.");
            Slimefun.logger().log(Level.SEVERE, " ");
            bugs.remove(position);

            /*
             * Terminate the machine properly, mirroring its BlockBreakHandler:
             * end any ongoing operation so it cannot be "resumed" by a new
             * machine placed at this location (its ingredients were consumed
             * long ago - resuming would produce free outputs), and clear any
             * cached machine state (e.g. AContainer's negative recipe scans)
             * so the next machine at this spot starts with a clean slate.
             */
            if (item instanceof MachineProcessHolder<?> processHolder) {
                processHolder.getMachineProcessor().endOperation(l);
            }

            if (item instanceof AContainer container) {
                container.clearRecipeCache(l);
            }

            BlockMenu menu = BlockStorage.getInventory(l);

            /*
             * Notify any networks claiming this location, mirroring what the
             * NetworkListener does for a normal break. A terminated regulator's
             * Network would otherwise leak: never ticked again (the ticker is
             * gone) yet still registered, with markDirty events piling up in
             * its queue forever.
             */
            Slimefun.getNetworkManager().updateAllNetworks(l);

            Bukkit.getScheduler().scheduleSyncDelayedTask(Slimefun.instance(), () -> {
                if (menu != null) {
                    // Drop the machine's contents like a normal block break would
                    int[] inventorySlots = menu.getPreset().getInventorySlots().stream().mapToInt(Integer::intValue).toArray();
                    menu.dropItems(l, inventorySlots);
                }

                l.getBlock().setType(Material.AIR);
            });

            BlockStorage.deleteLocationInfoUnsafely(l, true);
        }
    }

    public boolean isHalted() {
        return halted;
    }

    public void halt() {
        halted = true;
    }

    /**
     * Waits (with a timeout) until no asynchronous {@link #run()} is in flight.
     * {@link org.bukkit.Bukkit#getScheduler()} cancellation does not interrupt an
     * already running task, so without this a shutdown {@link #run()} call would
     * return immediately (because {@link #running} is true) and leave the
     * deletion/move queues un-drained before the final save - resurrecting
     * block data for blocks that were just broken.
     */
    public void awaitIdle() {
        long deadline = System.currentTimeMillis() + 5_000;

        while (running.get() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @ParametersAreNonnullByDefault
    public void queueMove(Location from, Location to) {
        Validate.notNull(from, "Source Location cannot be null!");
        Validate.notNull(to, "Target Location cannot be null!");

        movingQueue.put(from, to);
    }

    @ParametersAreNonnullByDefault
    public void queueDelete(Location l, boolean destroy) {
        Validate.notNull(l, "Location must not be null!");

        deletionQueue.put(l, destroy);
    }


    @ParametersAreNonnullByDefault
    public void queueDelete(Collection<Location> locations, boolean destroy) {
        Validate.notNull(locations, "Locations must not be null");

        Map<Location, Boolean> toDelete = new HashMap<>(locations.size(), 1.0F);
        for (Location location : locations) {
            Validate.notNull(location, "Locations must not contain null locations");
            toDelete.put(location, destroy);
        }
        deletionQueue.putAll(toDelete);
    }

    @ParametersAreNonnullByDefault
    public void queueDelete(Map<Location, Boolean> locations) {
        Validate.notNull(locations, "Locations must not be null");
        for (Map.Entry<Location, Boolean> entry : locations.entrySet()) {
            Validate.notNull(entry.getKey(), "Location in locations cannot be null");
            Validate.notNull(entry.getValue(), "Boolean toDestroy in locations cannot be null");
        }
        deletionQueue.putAll(locations);
    }

    /**
     * Forgets the error count of the given {@link Location}.
     * Called when a block's data is deleted: without this, a machine placed
     * later at the same spot would inherit the previous block's error count -
     * it would be terminated without ever generating an ErrorReport (those
     * are only written for the first error), and the count would leak.
     *
     * @param l
     *            The {@link Location} whose error count should be reset
     */
    public void resetErrorCount(@Nonnull Location l) {
        Validate.notNull(l, "Location must not be null!");

        bugs.remove(new BlockPosition(l));
    }

    /**
     * Drains every queued deletion and move that belongs to the given {@link World}.
     * This must be called BEFORE that World's {@link BlockStorage} is saved and
     * removed on unload: pending queue entries would otherwise never reach the
     * in-memory state that gets saved, and blocks broken just before the unload
     * would "resurrect" (with their inventories dropping again) when the World
     * is loaded next time.
     *
     * @param world
     *            The {@link World} whose queue entries should be processed now
     */
    public void drainQueues(@Nonnull World world) {
        Validate.notNull(world, "The World cannot be null");

        Iterator<Map.Entry<Location, Boolean>> removals = deletionQueue.entrySet().iterator();
        while (removals.hasNext()) {
            Map.Entry<Location, Boolean> entry = removals.next();
            World entryWorld = entry.getKey().getWorld();

            if (entryWorld != null && entryWorld.getUID().equals(world.getUID())) {
                try {
                    BlockStorage.deleteLocationInfoUnsafely(entry.getKey(), entry.getValue());
                } catch (Exception | LinkageError x) {
                    Slimefun.logger().log(Level.WARNING, x, () -> "Could not delete block data @ " + new BlockPosition(entry.getKey()) + " during world unload");
                }

                removals.remove();
            }
        }

        Iterator<Map.Entry<Location, Location>> moves = movingQueue.entrySet().iterator();
        while (moves.hasNext()) {
            Map.Entry<Location, Location> entry = moves.next();
            World entryWorld = entry.getKey().getWorld();

            if (entryWorld != null && entryWorld.getUID().equals(world.getUID())) {
                try {
                    BlockStorage.moveLocationInfoUnsafely(entry.getKey(), entry.getValue());
                } catch (Exception | LinkageError x) {
                    Slimefun.logger().log(Level.WARNING, x, () -> "Could not move block data @ " + new BlockPosition(entry.getKey()) + " during world unload");
                }

                moves.remove();
            }
        }
    }

    /**
     * This method checks if the given {@link Location} has been reserved
     * by this {@link TickerTask}.
     * A reserved {@link Location} does not currently hold any data but will
     * be occupied upon the next tick.
     * Checking this ensures that our {@link Location} does not get treated like a normal
     * {@link Location} as it is theoretically "moving".
     *
     * @param l
     *            The {@link Location} to check
     * 
     * @return Whether this {@link Location} has been reserved and will be filled upon the next tick
     */
    public boolean isOccupiedSoon(@Nonnull Location l) {
        Validate.notNull(l, "Null is not a valid Location!");

        return movingQueue.containsValue(l);
    }

    /**
     * This method checks if a given {@link Location} will be deleted on the next tick.
     * 
     * @param l
     *            The {@link Location} to check
     * 
     * @return Whether this {@link Location} will be deleted on the next tick
     */
    public boolean isDeletedSoon(@Nonnull Location l) {
        Validate.notNull(l, "Null is not a valid Location!");

        return deletionQueue.containsKey(l);
    }

    /**
     * This returns the delay between ticks
     * 
     * @return The tick delay
     */
    public int getTickRate() {
        return tickRate;
    }

    /**
     * This method returns a <strong>read-only</strong> {@link Map}
     * representation of every {@link ChunkPosition} and its corresponding
     * {@link Set} of ticking {@link Location Locations}.
     * 
     * This does include any {@link Location} from an unloaded {@link Chunk} too!
     * 
     * @return A {@link Map} representation of all ticking {@link Location Locations}
     */
    @Nonnull
    public Map<ChunkPosition, Set<Location>> getLocations() {
        return Collections.unmodifiableMap(tickingLocations);
    }

    /**
     * This method returns a <strong>read-only</strong> {@link Set}
     * of all ticking {@link Location Locations} in a given {@link Chunk}.
     * The {@link Chunk} does not have to be loaded.
     * If no {@link Location} is present, the returned {@link Set} will be empty.
     * 
     * @param chunk
     *            The {@link Chunk}
     * 
     * @return A {@link Set} of all ticking {@link Location Locations}
     */
    @Nonnull
    public Set<Location> getLocations(@Nonnull Chunk chunk) {
        Validate.notNull(chunk, "The Chunk cannot be null!");

        Set<Location> locations = tickingLocations.getOrDefault(new ChunkPosition(chunk), Collections.emptySet());
        return Collections.unmodifiableSet(locations);
    }

    /**
     * This removes every ticking {@link Location} that belongs to the given
     * {@link World}. Called when that {@link World} is unloaded, so no stale
     * entries (and their {@link Location} references) linger around until the
     * World is loaded again.
     *
     * @param world
     *            The {@link World} being unloaded
     */
    public void removeTickingLocations(@Nonnull World world) {
        Validate.notNull(world, "The World cannot be null");

        tickingLocations.keySet().removeIf(chunk -> {
            try {
                World chunkWorld = chunk.getWorld();
                return chunkWorld == null || chunkWorld.getUID().equals(world.getUID());
            } catch (IllegalStateException x) {
                /*
                 * The ChunkPosition's WeakReference to its World was collected:
                 * a dead entry that belongs to an unloaded World either way.
                 */
                return true;
            }
        });
    }

    /**
     * This enables the ticker at the given {@link Location} and adds it to our "queue".
     * 
     * @param l
     *            The {@link Location} to activate
     */
    public void enableTicker(@Nonnull Location l) {
        Validate.notNull(l, "Location cannot be null!");

        ChunkPosition chunk = new ChunkPosition(l.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4);

        /*
         * One atomic compute: a concurrent disableTicker() emptying and
         * removing the chunk entry can no longer make a freshly added
         * Location vanish together with the removed Set.
         */
        tickingLocations.compute(chunk, (key, locations) -> {
            if (locations == null) {
                locations = ConcurrentHashMap.newKeySet();
            }

            locations.add(l);
            return locations;
        });
    }

    /**
     * This method disables the ticker at the given {@link Location} and removes it from our internal
     * "queue".
     *
     * @param l
     *            The {@link Location} to remove
     */
    public void disableTicker(@Nonnull Location l) {
        Validate.notNull(l, "Location cannot be null!");

        ChunkPosition chunk = new ChunkPosition(l.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4);

        /*
         * One atomic computeIfPresent: removing the Location and removing the
         * (then empty) chunk entry happen as a single action, so a concurrent
         * enableTicker() for the same chunk can neither lose its entry nor
         * resurrect an empty Set.
         */
        tickingLocations.computeIfPresent(chunk, (key, locations) -> {
            locations.remove(l);
            return locations.isEmpty() ? null : locations;
        });
    }

}

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
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunTickEvent;
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
     * The value of this map (Map entries) MUST be thread-safe and mutable.
     *
     * <p>Each entry is a {@link TickingBlock} carrying the resolved
     * {@link SlimefunItem} and {@link BlockTicker} for its {@link Location}:
     * the resolution chain (id extraction + registry lookup + ticker lookup)
     * only changes when a block's data is replaced, which every first-party
     * path expresses via {@link #enableTicker(Location)} (re-place, re-store)
     * or {@link #disableTicker(Location)} (break, move-away, delete). The
     * live {@link Config} is still read fresh on every tick - machines mutate
     * their data through it, so caching the reference would silently divert
     * those writes into a dead object.
     */
    private final Map<ChunkPosition, Map<Location, TickingBlock>> tickingLocations = new ConcurrentHashMap<>();

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
        private final BlockTicker ticker;
        private final Config data;

        SynchronizedTick(@Nonnull Location location, @Nonnull SlimefunItem item, @Nonnull BlockTicker ticker, @Nonnull Config data) {
            this.location = location;
            this.item = item;
            this.ticker = ticker;
            this.data = data;
        }
    }

    /**
     * One actively ticking block: its {@link Location} plus the item/ticker
     * resolution that used to be recomputed on every single tick.
     *
     * <p>The resolution is captured when the ticker is enabled (every
     * first-party flow stores the block data before enabling the ticker) and
     * published safely through the enclosing {@link ConcurrentHashMap}. If the
     * data was not resolvable at that moment - or was deleted since via a
     * {@code destroy = false} deletion - {@link #resolved} stays {@code null}
     * and the ticker thread retries the resolution once per tick, exactly the
     * per-tick lookup the un-cached path performed.
     */
    private static final class TickingBlock {

        private final Location location;

        /** Immutable resolution of (item, ticker, synchronised). Volatile: written by either thread, read while ticking. */
        private volatile Resolved resolved;

        TickingBlock(@Nonnull Location location) {
            this.location = location;
            this.resolved = resolve();
        }

        @Nullable
        private Resolved resolve() {
            Config data = BlockStorage.getLocationInfo(location);
            String id = data.getString("id");

            if (id != null) {
                SlimefunItem item = SlimefunItem.getById(id);

                if (item != null) {
                    BlockTicker ticker = item.getBlockTicker();

                    if (ticker != null) {
                        return new Resolved(item, ticker, ticker.isSynchronized());
                    }
                }
            }

            return null;
        }

        /**
         * Drops the cached resolution so the next tick re-resolves from live
         * data (used when data was deleted without disabling the ticker).
         */
        void invalidate() {
            resolved = null;
        }

        /** The resolved dispatch target of this block. */
        private record Resolved(@Nonnull SlimefunItem item, @Nonnull BlockTicker ticker, boolean synchronised) {
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

                    /*
                     * A destroy=false deletion removes the data but leaves the ticker
                     * registered (the block is expected to receive new data): drop the
                     * cached resolution so the next tick resolves the replacement
                     * instead of ticking with the departed item.
                     */
                    if (!entry.getValue()) {
                        invalidateResolution(entry.getKey());
                    }
                } catch (Exception | LinkageError x) {
                    Slimefun.logger().log(Level.WARNING, x, () -> "Could not delete block data @ " + new BlockPosition(entry.getKey()) + ", dropping the queue entry");
                }

                removals.remove();
            }

            // Fixes #2576 - Remove any deleted instances of BlockStorage
            Slimefun.getRegistry().getWorlds().values().removeIf(BlockStorage::isMarkedForRemoval);

            // Run our ticker code
            if (!halted) {
                for (Map.Entry<ChunkPosition, Map<Location, TickingBlock>> entry : tickingLocations.entrySet()) {
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
                        tickBlock(tick.location, b, tick.item, tick.ticker, tick.data, System.nanoTime());
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
            long tickDuration = System.nanoTime() - tickStart;
            Slimefun.getProfiler().endTick(tickDuration);

            if (SlimefunTickEvent.getHandlerList().getRegisteredListeners().length > 0) {
                Bukkit.getPluginManager().callEvent(new SlimefunTickEvent(tickDuration));
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void tickChunk(ChunkPosition chunk, Set<BlockTicker> tickers, Map<Location, TickingBlock> locations, List<SynchronizedTick> synchronizedTicks) {
        try {
            // Only continue if the Chunk is actually loaded
            if (chunk.isLoaded()) {
                // Resolve the world's BlockStorage once per chunk instead of on every block.
                BlockStorage storage = BlockStorage.getStorage(chunk.getWorld());

                for (TickingBlock block : locations.values()) {
                    tickLocation(tickers, block, synchronizedTicks, storage);
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

    private void tickLocation(@Nonnull Set<BlockTicker> tickers, @Nonnull TickingBlock block, @Nonnull List<SynchronizedTick> synchronizedTicks, @Nullable BlockStorage storage) {
        Location l = block.location;
        TickingBlock.Resolved resolution = block.resolved;

        if (resolution == null) {
            /*
             * Not resolvable yet (or deleted since): retry the resolution once,
             * paying exactly the per-tick lookup the un-cached path performed.
             * Stays skipped until data appears - the same visible behaviour as
             * the old getById-null early return.
             */
            resolution = block.resolve();

            if (resolution == null) {
                return;
            }

            block.resolved = resolution;
        }

        // The live Config is read fresh on EVERY tick: machines mutate their
        // data through this object, so a cached reference would divert those
        // writes into a dead object when the storage entry gets replaced.
        Config data = storage != null ? BlockStorage.getLocationInfo(l, storage) : BlockStorage.getLocationInfo(l);
        SlimefunItem item = resolution.item();
        BlockTicker blockTicker = resolution.ticker();

        try {
            if (resolution.synchronised()) {
                Slimefun.getProfiler().scheduleEntries(1);
                blockTicker.update();

                // Buffered: all synchronized blocks are ticked in a single scheduler
                // submission at the end of this run (see run()).
                synchronizedTicks.add(new SynchronizedTick(l, item, blockTicker, data));
            } else {
                long timestamp = Slimefun.getProfiler().newEntry();
                blockTicker.update();
                Block b = l.getBlock();
                tickBlock(l, b, item, blockTicker, data, timestamp);
            }

            tickers.add(blockTicker);
        } catch (Exception x) {
            reportErrors(l, item, x);
        }
    }

    @ParametersAreNonnullByDefault
    private void tickBlock(Location l, Block b, SlimefunItem item, BlockTicker ticker, Config data, long timestamp) {
        try {
            ticker.tick(b, item, data);
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

                    // Same resolution-drop as the run()-side drain (see there)
                    if (!entry.getValue()) {
                        invalidateResolution(entry.getKey());
                    }
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
     * This method checks if the given {@link Location} has been reserved
     * as the <strong>source</strong> of a queued move: its data is still present
     * but will leave upon the next tick. Moving another block into this Location
     * before the move is processed would make the deferred queue overwrite the
     * arriving data with the departing one.
     *
     * @param l
     *            The {@link Location} to check
     *
     * @return Whether this {@link Location}'s data will move away on the next tick
     */
    public boolean isMovingFrom(@Nonnull Location l) {
        Validate.notNull(l, "Null is not a valid Location!");

        return movingQueue.containsKey(l);
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
     * This returns a <strong>read-only</strong> {@link Map}
     * representation of every {@link ChunkPosition} and its corresponding
     * {@link Set} of ticking {@link Location Locations}.
     *
     * This does include any {@link Location} from an unloaded {@link Chunk} too!
     *
     * @return A {@link Map} representation of all ticking {@link Location Locations}
     */
    @Nonnull
    public Map<ChunkPosition, Set<Location>> getLocations() {
        Map<ChunkPosition, Set<Location>> view = new HashMap<>(tickingLocations.size());

        for (Map.Entry<ChunkPosition, Map<Location, TickingBlock>> entry : tickingLocations.entrySet()) {
            view.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue().keySet()));
        }

        return Collections.unmodifiableMap(view);
    }

    /**
     * This returns a <strong>read-only</strong> {@link Set}
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

        Set<Location> locations = tickingLocations.getOrDefault(new ChunkPosition(chunk), Collections.emptyMap()).keySet();
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
         * Location vanish together with the removed entry. The put also
         * REPLACES any prior entry, so a re-enable (re-place, re-store) picks
         * up a fresh item/ticker resolution.
         */
        tickingLocations.compute(chunk, (key, blocks) -> {
            if (blocks == null) {
                blocks = new ConcurrentHashMap<>();
            }

            blocks.put(l, new TickingBlock(l));
            return blocks;
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
         * resurrect an empty entry.
         */
        tickingLocations.computeIfPresent(chunk, (key, blocks) -> {
            blocks.remove(l);
            return blocks.isEmpty() ? null : blocks;
        });
    }

    /**
     * Drops the cached item/ticker resolution of the given {@link Location}
     * (if it is currently ticking): the next tick re-resolves from live data.
     * Used when block data is deleted without disabling the ticker
     * ({@code destroy = false} deletions).
     *
     * @param l
     *            The {@link Location} whose resolution should be dropped
     */
    private void invalidateResolution(@Nonnull Location l) {
        ChunkPosition chunk = new ChunkPosition(l.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4);

        tickingLocations.computeIfPresent(chunk, (key, blocks) -> {
            TickingBlock block = blocks.get(l);

            if (block != null) {
                block.invalidate();
            }

            return blocks;
        });
    }

}

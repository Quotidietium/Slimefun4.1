package io.github.thebusybiscuit.slimefun4.core.networks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.network.Network;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.NetworkListener;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * The {@link NetworkManager} is responsible for holding all instances of {@link Network}
 * and providing some utility methods that would have probably been static otherwise.
 * 
 * @author TheBusyBiscuit
 * @author meiamsome
 * 
 * @see Network
 * @see NetworkListener
 *
 */
public class NetworkManager {

    private final int maxNodes;
    private final boolean enableVisualizer;
    private final boolean deleteExcessItems;

    /**
     * Fixes #3041
     * 
     * We use a {@link CopyOnWriteArrayList} here to ensure thread-safety.
     * This {@link List} is also much more frequently read than being written to.
     * Therefore a {@link CopyOnWriteArrayList} should be perfect for this, even
     * if insertions come at a slight cost.
     */
    private final List<Network> networks = new CopyOnWriteArrayList<>();

    /**
     * A spatial index mapping (world, chunk) to the {@link Network Networks} that occupy
     * that chunk. This allows {@link #getNetworkFromLocation(Location, Class)} and
     * {@link #getNetworksFromLocation(Location, Class)} to only inspect the handful of
     * {@link Network Networks} in the relevant chunk, instead of linearly scanning every
     * {@link Network} on the {@link Server} for each lookup.
     * <p>
     * The index is a <em>superset</em> filter: candidates are always confirmed via
     * {@link Network#connectsTo(Location)}, so correctness never depends on the index
     * being perfectly precise, only on it containing every {@link Network} that could
     * connect to a given {@link Location}.
     */
    private final Map<UUID, Map<Long, CopyOnWriteArrayList<Network>>> networksByChunk = new ConcurrentHashMap<>();

    /**
     * Tracks which chunk keys each {@link Network} has been registered under, so they can
     * all be removed again in {@link #unregisterNetwork(Network)}.
     */
    private final Map<Network, Set<Long>> chunksPerNetwork = new ConcurrentHashMap<>();

    /**
     * A fallback list for {@link Network Networks} that could not be added to the spatial
     * index because their regulator {@link Location} is unavailable. This should always be
     * empty in production, but some consumers (e.g. mocked {@link Network Networks} in unit
     * tests) register a {@link Network} without a regulator. These are looked up linearly,
     * exactly like the pre-index behaviour.
     */
    private final List<Network> unindexedNetworks = new CopyOnWriteArrayList<>();

    /**
     * Computes a unique key for the chunk containing the given {@link Location}.
     *
     * @param l
     *            The {@link Location}
     *
     * @return A key uniquely identifying the chunk (within one world)
     */
    static long chunkKey(@Nonnull Location l) {
        return ((long) (l.getBlockX() >> 4) << 32) | ((l.getBlockZ() >> 4) & 0xffffffffL);
    }

    /**
     * Guards the two-step write in {@link #registerNetworkChunk(Network, Location)}
     * against a concurrent {@link #unregisterNetwork(Network)}: without a common
     * lock, an unregister landing between the two writes would leave a stale
     * bucket entry behind that can never be removed again.
     */
    private final Object chunkIndexLock = new Object();

    /**
     * Registers a {@link Network} under the chunk containing the given {@link Location}.
     * This is called internally whenever a {@link Network} starts occupying a new chunk.
     * <p>
     * <strong>This is for internal use only.</strong>
     *
     * @param network
     *            The {@link Network} to index
     * @param l
     *            A {@link Location} that is part of the {@link Network}
     */
    public void registerNetworkChunk(@Nonnull Network network, @Nonnull Location l) {
        long chunkKey = chunkKey(l);

        synchronized (chunkIndexLock) {
            /*
             * The Network may have been unregistered (its regulator broken) while
             * its asynchronous discovery was still running. Indexing it now would
             * create a "zombie" entry that no code path can ever remove again.
             * The flag is flipped inside the same lock, so the check is exact.
             */
            if (!network.isRegistered()) {
                return;
            }

            networksByChunk
                .computeIfAbsent(l.getWorld().getUID(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chunkKey, k -> new CopyOnWriteArrayList<>())
                .addIfAbsent(network);

            chunksPerNetwork.computeIfAbsent(network, k -> ConcurrentHashMap.newKeySet()).add(chunkKey);
        }
    }

    /**
     * This creates a new {@link NetworkManager} with the given capacity.
     * 
     * @param maxStepSize
     *            The maximum amount of nodes a {@link Network} can have
     * @param enableVisualizer
     *            Whether the {@link Network} visualizer is enabled
     * @param deleteExcessItems
     *            Whether excess items from a {@link CargoNet} should be voided
     */
    public NetworkManager(int maxStepSize, boolean enableVisualizer, boolean deleteExcessItems) {
        Validate.isTrue(maxStepSize > 0, "The maximal Network size must be above zero!");

        this.enableVisualizer = enableVisualizer;
        this.deleteExcessItems = deleteExcessItems;
        maxNodes = maxStepSize;
    }

    /**
     * This creates a new {@link NetworkManager} with the given capacity.
     * 
     * @param maxStepSize
     *            The maximum amount of nodes a {@link Network} can have
     */
    public NetworkManager(int maxStepSize) {
        this(maxStepSize, true, false);
    }

    /**
     * This method returns the limit of nodes a {@link Network} can have.
     * This value is read from the {@link Config} file.
     * 
     * @return the maximum amount of nodes a {@link Network} can have
     */
    public int getMaxSize() {
        return maxNodes;
    }

    /**
     * This returns whether the {@link Network} visualizer is enabled.
     * 
     * @return Whether the {@link Network} visualizer is enabled
     */
    public boolean isVisualizerEnabled() {
        return enableVisualizer;
    }

    /**
     * This returns whether excess items from a {@link CargoNet} should be voided
     * instead of being dropped to the ground.
     * 
     * @return Whether to delete excess items
     */
    public boolean isItemDeletionEnabled() {
        return deleteExcessItems;
    }

    /**
     * This returns a {@link List} of every {@link Network} on the {@link Server}.
     * The returned {@link List} is not modifiable.
     * 
     * @return A {@link List} containing every {@link Network} on the {@link Server}
     */
    @Nonnull
    public List<Network> getNetworkList() {
        return Collections.unmodifiableList(networks);
    }

    @Nonnull
    public <T extends Network> Optional<T> getNetworkFromLocation(@Nullable Location l, @Nonnull Class<T> type) {
        if (l == null) {
            return Optional.empty();
        }

        Validate.notNull(type, "Type must not be null");

        // Always check the (normally empty) fallback list of networks without a regulator.
        for (Network network : unindexedNetworks) {
            if (type.isInstance(network) && network.connectsTo(l)) {
                return Optional.of(type.cast(network));
            }
        }

        Map<Long, CopyOnWriteArrayList<Network>> worldMap = networksByChunk.get(l.getWorld().getUID());

        if (worldMap == null) {
            return Optional.empty();
        }

        List<Network> candidates = worldMap.get(chunkKey(l));

        if (candidates == null) {
            return Optional.empty();
        }

        for (Network network : candidates) {
            if (type.isInstance(network) && network.connectsTo(l)) {
                return Optional.of(type.cast(network));
            }
        }

        return Optional.empty();
    }

    @Nonnull
    public <T extends Network> List<T> getNetworksFromLocation(@Nullable Location l, @Nonnull Class<T> type) {
        if (l == null) {
            // No networks here, if the location does not even exist
            return Collections.emptyList();
        }

        Validate.notNull(type, "Type must not be null");
        List<T> list = new ArrayList<>();

        // Always check the (normally empty) fallback list of networks without a regulator.
        for (Network network : unindexedNetworks) {
            if (type.isInstance(network) && network.connectsTo(l)) {
                list.add(type.cast(network));
            }
        }

        Map<Long, CopyOnWriteArrayList<Network>> worldMap = networksByChunk.get(l.getWorld().getUID());

        if (worldMap == null) {
            return list;
        }

        List<Network> candidates = worldMap.get(chunkKey(l));

        if (candidates == null) {
            return list;
        }

        for (Network network : candidates) {
            if (type.isInstance(network) && network.connectsTo(l)) {
                list.add(type.cast(network));
            }
        }

        return list;
    }

    /**
     * This registers a given {@link Network}.
     * 
     * @param network
     *            The {@link Network} to register
     */
    public void registerNetwork(@Nonnull Network network) {
        Validate.notNull(network, "Cannot register a null Network");
        networks.add(network);

        Location regulator = network.getRegulator();

        if (regulator != null) {
            // Index the Network under the chunk of its regulator.
            registerNetworkChunk(network, regulator);
        } else {
            // A Network without a regulator cannot be indexed, so we fall back to a
            // linear lookup for it (should never happen outside of unit tests).
            unindexedNetworks.add(network);
        }
    }

    /**
     * This removes a {@link Network} from the network system.
     *
     * @param network
     *            The {@link Network} to remove
     */
    public void unregisterNetwork(@Nonnull Network network) {
        Validate.notNull(network, "Cannot unregister a null Network");
        networks.remove(network);
        unindexedNetworks.remove(network);

        synchronized (chunkIndexLock) {
            /*
             * Flip the registered flag inside the lock: a concurrently running
             * registerNetworkChunk (from the Network's asynchronous discovery)
             * will now reliably skip instead of re-indexing a dead Network.
             */
            network.markAsUnregistered();

            // Remove the Network from the chunk index again.
            Set<Long> chunks = chunksPerNetwork.remove(network);

            if (chunks == null) {
                return;
            }

            Location regulator = network.getRegulator();

            if (regulator != null) {
                removeFromChunkIndex(network, networksByChunk.get(regulator.getWorld().getUID()), chunks);
            } else {
                /*
                 * Without a regulator we cannot know the World (unit tests only) -
                 * sweep all worlds so no stale bucket entry is left behind.
                 */
                for (Map<Long, CopyOnWriteArrayList<Network>> worldMap : networksByChunk.values()) {
                    removeFromChunkIndex(network, worldMap, chunks);
                }
            }
        }
    }

    private void removeFromChunkIndex(@Nonnull Network network, Map<Long, CopyOnWriteArrayList<Network>> worldMap, @Nonnull Set<Long> chunks) {
        if (worldMap != null) {
            for (Long chunkKey : chunks) {
                List<Network> bucket = worldMap.get(chunkKey);

                if (bucket != null) {
                    bucket.remove(network);

                    if (bucket.isEmpty()) {
                        worldMap.remove(chunkKey, bucket);
                    }
                }
            }
        }
    }

    /**
     * This removes every {@link Network} that lives in the given {@link World},
     * together with that World's chunk index. Called when a {@link World} is
     * unloaded, so a stale (dead) Network can never shadow a new one after the
     * World is loaded again.
     *
     * @param world
     *            The {@link World} being unloaded
     */
    public void removeAllNetworks(@Nonnull World world) {
        Validate.notNull(world, "The World cannot be null");
        UUID worldId = world.getUID();

        for (Network network : networks) {
            Location regulator = network.getRegulator();

            if (regulator != null && regulator.getWorld().getUID().equals(worldId)) {
                unregisterNetwork(network);
            }
        }

        synchronized (chunkIndexLock) {
            networksByChunk.remove(worldId);
        }
    }

    /**
     * This method updates every {@link Network} found at the given {@link Location}.
     * More precisely, {@link Network#markDirty(Location)} will be called.
     * 
     * @param l
     *            The {@link Location} to update
     */
    public void updateAllNetworks(@Nonnull Location l) {
        Validate.notNull(l, "The Location cannot be null");

        try {
            /*
             * No need to create a sublist and loop through it if
             * there aren't even any networks on the server.
             */
            if (networks.isEmpty()) {
                return;
            }

            /*
             * Only a Slimefun block can be part of a Network.
             * This check helps to speed up performance.
             * 
             * (Skip for Unit Tests as they don't support block info yet)
             */
            if (!BlockStorage.hasBlockInfo(l) && Slimefun.getMinecraftVersion() != MinecraftVersion.UNIT_TEST) {
                return;
            }

            for (Network network : getNetworksFromLocation(l, Network.class)) {
                network.markDirty(l);
            }
        } catch (Exception x) {
            Slimefun.logger().log(Level.SEVERE, x, () -> "An Exception was thrown while causing a networks update @ " + new BlockPosition(l));
        }
    }

}

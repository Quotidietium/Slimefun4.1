package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.CargoNetTickEvent;
import io.github.thebusybiscuit.slimefun4.api.network.Network;
import io.github.thebusybiscuit.slimefun4.api.network.NetworkComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * The {@link CargoNet} is a type of {@link Network} which deals with {@link ItemStack} transportation.
 * It is also an extension of {@link AbstractItemNetwork} which provides methods to deal
 * with the addon ChestTerminal.
 * 
 * @author meiamsome
 * @author Poslovitch
 * @author John000708
 * @author BigBadE
 * @author SoSeDiK
 * @author TheBusyBiscuit
 * @author Walshy
 * @author DNx5
 *
 */
public class CargoNet extends AbstractItemNetwork implements HologramOwner {

    private static final int RANGE = 5;
    private static final int TICK_DELAY = Slimefun.getCfg().getInt("networks.cargo-ticker-delay");

    private final Set<Location> inputNodes = new HashSet<>();
    private final Set<Location> outputNodes = new HashSet<>();

    protected final Map<Location, Integer> roundRobin = new HashMap<>();
    private int tickDelayThreshold = 0;

    /**
     * Cached routing maps for {@link #mapInputNodes()}/{@link #mapOutputNodes()}.
     * With the default {@code cargo-ticker-delay: 0} these maps would otherwise be
     * rebuilt from BlockStorage on every single game tick, even though they only
     * change when a node is added, removed or reconfigured.
     *
     * <p>Invalidated by {@link #markCargoNodeConfigurationDirty(Location)} (node
     * configuration change - the same contract the {@link ItemFilter} cache and
     * the channel selectors use) and by {@link #onClassificationChange} (node
     * added/removed). A null value means "stale, rebuild on next use".
     *
     * <p>Copy-on-write handoff: a rebuild always publishes fresh map instances,
     * so a {@link CargoNetworkTask} that was already scheduled onto the main
     * thread with the previous maps never observes mutation. The maps are only
     * ever read by the task, never written. A rebuild that races an
     * invalidation (node data changed mid-build on the main thread) detects
     * this via {@link #routingGeneration} and discards its result, so the next
     * tick rebuilds from the fresh data instead of publishing stale routing.
     */
    private volatile Map<Location, Integer> cachedInputs = null;
    private volatile Map<Integer, List<Location>> cachedOutputs = null;

    /**
     * Bumped by every routing-cache invalidation. The rebuild reads it before
     * and after building: if it changed, BlockStorage was written concurrently
     * and the built maps are stale - they are dropped instead of published.
     */
    private final AtomicInteger routingGeneration = new AtomicInteger();

    public static @Nullable CargoNet getNetworkFromLocation(@Nonnull Location l) {
        return Slimefun.getNetworkManager().getNetworkFromLocation(l, CargoNet.class).orElse(null);
    }

    public static @Nonnull CargoNet getNetworkFromLocationOrCreate(@Nonnull Location l) {
        Optional<CargoNet> cargoNetwork = Slimefun.getNetworkManager().getNetworkFromLocation(l, CargoNet.class);

        if (cargoNetwork.isPresent()) {
            return cargoNetwork.get();
        } else {
            CargoNet network = new CargoNet(l);
            Slimefun.getNetworkManager().registerNetwork(network);
            return network;
        }
    }

    /**
     * This constructs a new {@link CargoNet} at the given {@link Location}.
     * 
     * @param l
     *            The {@link Location} marking the manager of this {@link Network}.
     */
    protected CargoNet(@Nonnull Location l) {
        super(l);
    }

    @Override
    public String getId() {
        return "CARGO_NETWORK";
    }

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    public NetworkComponent classifyLocation(@Nonnull Location l) {
        String id = BlockStorage.checkID(l);

        if (id == null) {
            return null;
        }

        return switch (id) {
            case "CARGO_MANAGER" -> NetworkComponent.REGULATOR;
            case "CARGO_NODE" -> NetworkComponent.CONNECTOR;
            case "CARGO_NODE_INPUT",
                "CARGO_NODE_OUTPUT",
                "CARGO_NODE_OUTPUT_ADVANCED" -> NetworkComponent.TERMINUS;
            default -> null;
        };
    }

    @Override
    public void onClassificationChange(Location l, NetworkComponent from, NetworkComponent to) {
        connectorCache.remove(l);

        /*
         * The node sets below decide the routing maps - any classification change
         * (node placed, broken or switched to another node type) invalidates them.
         */
        invalidateRoutingCache();

        if (from == NetworkComponent.TERMINUS) {
            inputNodes.remove(l);
            outputNodes.remove(l);

            /*
             * Evict the per-node caches too: both are keyed by Location and would
             * otherwise grow without bound on networks with high node churn.
             */
            filterCache.remove(l);
            roundRobin.remove(l);
        }

        if (to == NetworkComponent.TERMINUS) {
            String id = BlockStorage.checkID(l);
            switch (id) {
                case "CARGO_NODE_INPUT" -> inputNodes.add(l);
                case "CARGO_NODE_OUTPUT",
                    "CARGO_NODE_OUTPUT_ADVANCED" -> outputNodes.add(l);
                default -> {}
            }
        }
    }

    public void tick(@Nonnull Block b) {
        if (!regulator.equals(b.getLocation())) {
            updateHologram(b, "&4连接了多个货运管理器");
            return;
        }

        if (CargoNetTickEvent.getHandlerList().getRegisteredListeners().length > 0) {
            org.bukkit.Bukkit.getPluginManager().callEvent(new CargoNetTickEvent(this, b));
        }

        super.tick();

        if (connectorNodes.isEmpty() && terminusNodes.isEmpty()) {
            updateHologram(b, "&c未找到货运节点");
        } else {
            updateHologram(b, "&7状态： &a&l在线");

            // Skip ticking if the threshold is not reached. The delay is not same as minecraft tick,
            // but it's based on 'custom-ticker-delay' config.
            if (tickDelayThreshold < TICK_DELAY) {
                tickDelayThreshold++;
                return;
            }

            // Reset the internal threshold, so we can start skipping again
            tickDelayThreshold = 0;

            Map<Location, Integer> inputs = mapInputNodes();
            Map<Integer, List<Location>> outputs = mapOutputNodes();

            if (BlockStorage.getLocationInfo(b.getLocation(), "visualizer") == null) {
                display();
            }

            Slimefun.getProfiler().scheduleEntries(inputs.size() + 1);

            CargoNetworkTask runnable = new CargoNetworkTask(this, inputs, outputs);
            Slimefun.runSync(runnable);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Also drops the cached routing maps on this override: a node configuration
     * change (channel, filter, ...) can alter both the input→channel and the
     * channel→outputs grouping, so the next tick rebuilds them from fresh data.
     */
    @Override
    public void markCargoNodeConfigurationDirty(@Nonnull Location node) {
        super.markCargoNodeConfigurationDirty(node);
        invalidateRoutingCache();
    }

    private @Nonnull Map<Location, Integer> mapInputNodes() {
        updateRoutingCache();
        return cachedInputs;
    }

    private @Nonnull Map<Integer, List<Location>> mapOutputNodes() {
        updateRoutingCache();
        return cachedOutputs;
    }

    /**
     * Marks the cached routing maps as stale. Called from the main thread
     * (menu clicks, node placement/removal classification).
     */
    private void invalidateRoutingCache() {
        routingGeneration.incrementAndGet();
        cachedInputs = null;
    }

    /**
     * Rebuilds {@link #cachedInputs}/{@link #cachedOutputs} if a previous
     * invalidation marked them stale. Both maps are rebuilt together (they share
     * the frequency reads) and published as fresh instances - see the field docs
     * for the copy-on-write handoff contract.
     */
    private void updateRoutingCache() {
        if (cachedInputs != null) {
            return;
        }

        int generation = routingGeneration.get();

        Map<Location, Integer> inputs = new HashMap<>();

        for (Location node : inputNodes) {
            int frequency = getFrequency(node);

            if (frequency >= 0 && frequency < 16) {
                inputs.put(node, frequency);
            }
        }

        Map<Integer, List<Location>> output = new HashMap<>();

        List<Location> list = new LinkedList<>();
        int lastFrequency = -1;

        for (Location node : outputNodes) {
            int frequency = getFrequency(node);

            // Symmetric with the input side: only the 16 valid channels (0-15) are routed. An
            // out-of-range frequency (corrupted/NBT-edited data) would otherwise be grouped under
            // a key no input node ever uses, silently disabling that output forever.
            if (frequency < 0 || frequency >= 16) {
                continue;
            }

            if (frequency != lastFrequency && lastFrequency != -1) {
                output.merge(lastFrequency, list, (prev, next) -> {
                    prev.addAll(next);
                    return prev;
                });

                list = new LinkedList<>();
            }

            list.add(node);
            lastFrequency = frequency;
        }

        if (!list.isEmpty()) {
            output.merge(lastFrequency, list, (prev, next) -> {
                prev.addAll(next);
                return prev;
            });
        }

        /*
         * An invalidation raced this build (node data changed on the main thread
         * while we were reading it): the maps are stale, drop them and let the
         * next tick rebuild from the fresh data.
         */
        if (routingGeneration.get() != generation) {
            return;
        }

        cachedOutputs = output;
        cachedInputs = inputs;
    }

    /**
     * Corrupted frequencies are reported once per node (not on every cargo tick) so a
     * single tampered node cannot spam the log for the lifetime of the server.
     */
    private static final Set<Location> CORRUPTED_FREQUENCY_REPORTED = ConcurrentHashMap.newKeySet();

    /**
     * This method returns the frequency a given node is set to.
     * A missing value falls back to zero in order to preserve the integrity of the
     * {@link CargoNet}. Corrupted data (non-numeric or overflowing values, reachable
     * via NBT editing) fails closed with -1, so both {@link #mapInputNodes()} and
     * {@link #mapOutputNodes()} skip the node instead of silently rerouting its
     * container into channel 0 or crashing the Cargo Manager.
     *
     * @param node
     *            The {@link Location} of our cargo node
     *
     * @return The frequency of the given node, or -1 for corrupted data
     */
    private static int getFrequency(@Nonnull Location node) {
        String frequency = BlockStorage.getLocationInfo(node, "frequency");

        if (frequency == null) {
            return 0;
        }

        try {
            return Integer.parseInt(frequency);
        } catch (NumberFormatException x) {
            // Also caught: values that are numeric but exceed the integer range
            if (CORRUPTED_FREQUENCY_REPORTED.add(node)) {
                Slimefun.logger().log(Level.SEVERE, () -> "Failed to parse a Cargo Node Frequency (" + node.getWorld().getName() + " - " + node.getBlockX() + ',' + node.getBlockY() + ',' + node.getBlockZ() + "): " + frequency + " - the node is skipped until fixed");
            }

            return -1;
        }
    }
}

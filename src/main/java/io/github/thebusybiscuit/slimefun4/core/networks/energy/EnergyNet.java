package io.github.thebusybiscuit.slimefun4.core.networks.energy;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.ErrorReport;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.network.Network;
import io.github.thebusybiscuit.slimefun4.api.network.NetworkComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * The {@link EnergyNet} is an implementation of {@link Network} that deals with
 * electrical energy being sent from and to nodes.
 * 
 * @author meiamsome
 * @author TheBusyBiscuit
 * 
 * @see Network
 * @see EnergyNetComponent
 * @see EnergyNetProvider
 * @see EnergyNetComponentType
 *
 */
public class EnergyNet extends Network implements HologramOwner {

    private static final int RANGE = 6;

    private final Map<Location, EnergyNetProvider> generators = new HashMap<>();
    private final Map<Location, EnergyNetComponent> capacitors = new HashMap<>();
    private final Map<Location, EnergyNetComponent> consumers = new HashMap<>();

    /**
     * Components that failed during a previous tick. Used to rate-limit
     * {@link ErrorReport}s to one per failure streak: a persistently broken
     * component is retried every tick (so it heals itself once fixed), but it
     * must not write a new report file every single time.
     */
    private final Set<Location> failedComponents = ConcurrentHashMap.newKeySet();

    protected EnergyNet(@Nonnull Location l) {
        super(Slimefun.getNetworkManager(), l);
    }

    @Override
    public int getRange() {
        return RANGE;
    }
    
    /**
     * This creates an immutable {@link Map} of {@link EnergyNetProvider}s within this {@link EnergyNet} instance.
     *
     * @return An immutable {@link Map} of generators
     */
    public @Nonnull Map<Location, EnergyNetProvider> getGenerators() {
        return Collections.unmodifiableMap(generators);
    }
    
    /**
     * This creates an immutable {@link Map} of {@link EnergyNetComponentType#CAPACITOR} {@link EnergyNetComponent}s within this {@link EnergyNet} instance.
     *
     * @return An immutable {@link Map} of capacitors
     */
    public @Nonnull Map<Location, EnergyNetComponent> getCapacitors() {
        return Collections.unmodifiableMap(capacitors);
    }
    
    /**
     * This creates an immutable {@link Map} of {@link EnergyNetComponentType#CONSUMER} {@link EnergyNetComponent}s within this {@link EnergyNet} instance.
     *
     * @return An immutable {@link Map} of consumers
     */
    public @Nonnull Map<Location, EnergyNetComponent> getConsumers() {
        return Collections.unmodifiableMap(consumers);
    }

    @Override
    public @Nonnull String getId() {
        return "ENERGY_NETWORK";
    }

    @Override
    public NetworkComponent classifyLocation(@Nonnull Location l) {
        if (regulator.equals(l)) {
            return NetworkComponent.REGULATOR;
        }

        EnergyNetComponent component = getComponent(l);

        if (component == null) {
            return null;
        }

        EnergyNetComponentType type = component.getEnergyComponentType();

        if (type == null) {
            // An addon violated the @Nonnull contract - treat it as "not part of the network"
            return null;
        }

        return switch (type) {
            case CONNECTOR,
                CAPACITOR -> NetworkComponent.CONNECTOR;
            case CONSUMER,
                GENERATOR -> NetworkComponent.TERMINUS;
            default -> null;
        };
    }

    @Override
    public void onClassificationChange(Location l, NetworkComponent from, NetworkComponent to) {
        if (from == NetworkComponent.TERMINUS) {
            generators.remove(l);
            consumers.remove(l);
            failedComponents.remove(l);
        }

        EnergyNetComponent component = getComponent(l);

        if (component != null) {
            switch (component.getEnergyComponentType()) {
                case CAPACITOR:
                    capacitors.put(l, component);
                    break;
                case CONSUMER:
                    consumers.put(l, component);
                    break;
                case GENERATOR:
                    if (component instanceof EnergyNetProvider provider) {
                        generators.put(l, provider);
                    } else if (component instanceof SlimefunItem item) {
                        item.warn("This Item is marked as a GENERATOR but does not implement the interface EnergyNetProvider!");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void tick(@Nonnull Block b) {
        AtomicLong timestamp = new AtomicLong(Slimefun.getProfiler().newEntry());

        if (!regulator.equals(b.getLocation())) {
            updateHologram(b, "&4Multiple Energy Regulators connected");
            Slimefun.getProfiler().closeEntry(b.getLocation(), SlimefunItems.ENERGY_REGULATOR.getItem(), timestamp.get());
            return;
        }

        /*
         * When two previously separate Networks get joined (a cable now connects
         * both regulators), both Networks discover the same components and each
         * regulator keeps ticking its own Network: generators would be ticked
         * twice (double fuel consumption) and capacitors settled by whichever
         * Network runs first. Arbitrate deterministically - among all EnergyNets
         * claiming this regulator, only the one with the "smallest" regulator
         * location may tick, the others show the multiple-regulator warning.
         */
        List<EnergyNet> claiming = Slimefun.getNetworkManager().getNetworksFromLocation(b.getLocation(), EnergyNet.class);

        if (claiming.size() > 1) {
            Location primary = null;

            for (EnergyNet network : claiming) {
                Location candidate = network.getRegulator();

                if (primary == null || compareLocations(candidate, primary) < 0) {
                    primary = candidate;
                }
            }

            if (!regulator.equals(primary)) {
                updateHologram(b, "&4Multiple Energy Regulators connected");
                Slimefun.getProfiler().closeEntry(b.getLocation(), SlimefunItems.ENERGY_REGULATOR.getItem(), timestamp.get());
                return;
            }
        }

        super.tick();

        /*
         * super.tick() (Network.discoverStep) may have unregistered this network - a
         * regulator/connector classification change discards the network for a full rebuild.
         * Don't settle an already-discarded network: the rebuilt network will settle these
         * components too, and settling twice can briefly double-charge / double-supply.
         */
        if (!isRegistered()) {
            Slimefun.getProfiler().closeEntry(b.getLocation(), SlimefunItems.ENERGY_REGULATOR.getItem(), timestamp.get());
            return;
        }

        if (connectorNodes.isEmpty() && terminusNodes.isEmpty()) {
            updateHologram(b, "&4No Energy Network found");
        } else {
            int generatorsSupply = tickAllGenerators(timestamp::getAndAdd);
            int capacitorsSupply = tickAllCapacitors();
            int supply = NumberUtils.flowSafeAddition(generatorsSupply, capacitorsSupply);
            int remainingEnergy = supply;
            int demand = 0;

            for (Map.Entry<Location, EnergyNetComponent> entry : consumers.entrySet()) {
                Location loc = entry.getKey();
                EnergyNetComponent component = entry.getValue();

                /*
                 * Isolate every single consumer: one broken component (corrupt
                 * charge data, a misbehaving addon) must not abort the whole
                 * settlement loop - energy charged before the exception would
                 * never be subtracted again, effectively duplicating it each tick.
                 */
                try {
                    int capacity = component.getCapacity();
                    int charge = component.getCharge(loc);

                    if (charge < capacity) {
                        int availableSpace = capacity - charge;
                        demand = NumberUtils.flowSafeAddition(demand, availableSpace);

                        if (remainingEnergy > 0) {
                            if (remainingEnergy > availableSpace) {
                                component.setCharge(loc, capacity);
                                remainingEnergy -= availableSpace;
                            } else {
                                component.setCharge(loc, charge + remainingEnergy);
                                remainingEnergy = 0;
                            }
                        }
                    }

                    failedComponents.remove(loc);
                } catch (Exception | LinkageError x) {
                    reportComponentFailure(x, loc, component);
                }
            }

            storeRemainingEnergy(remainingEnergy);
            updateHologram(b, supply, demand);
        }

        // We have subtracted the timings from Generators, so they do not show up twice.
        Slimefun.getProfiler().closeEntry(b.getLocation(), SlimefunItems.ENERGY_REGULATOR.getItem(), timestamp.get());
    }

    private void storeRemainingEnergy(int remainingEnergy) {
        for (Map.Entry<Location, EnergyNetComponent> entry : capacitors.entrySet()) {
            Location loc = entry.getKey();
            EnergyNetComponent component = entry.getValue();

            try {
                if (remainingEnergy > 0) {
                    int capacity = component.getCapacity();

                    if (remainingEnergy > capacity) {
                        component.setCharge(loc, capacity);
                        remainingEnergy -= capacity;
                    } else {
                        component.setCharge(loc, remainingEnergy);
                        remainingEnergy = 0;
                    }
                } else {
                    component.setCharge(loc, 0);
                }

                failedComponents.remove(loc);
            } catch (Exception | LinkageError x) {
                // The capacitor keeps its previous charge - nothing gained, nothing lost
                reportComponentFailure(x, loc, component);
            }
        }

        for (Map.Entry<Location, EnergyNetProvider> entry : generators.entrySet()) {
            Location loc = entry.getKey();
            EnergyNetProvider component = entry.getValue();

            try {
                int capacity = component.getCapacity();

                if (remainingEnergy > 0) {
                    if (remainingEnergy > capacity) {
                        component.setCharge(loc, capacity);
                        remainingEnergy -= capacity;
                    } else {
                        component.setCharge(loc, remainingEnergy);
                        remainingEnergy = 0;
                    }
                } else {
                    component.setCharge(loc, 0);
                }

                failedComponents.remove(loc);
            } catch (Exception | LinkageError x) {
                reportComponentFailure(x, loc, component);
            }
        }
    }

    private int tickAllGenerators(@Nonnull LongConsumer timings) {
        Set<Location> explodedBlocks = new HashSet<>();
        int supply = 0;

        for (Map.Entry<Location, EnergyNetProvider> entry : generators.entrySet()) {
            long timestamp = Slimefun.getProfiler().newEntry();
            Location loc = entry.getKey();
            EnergyNetProvider provider = entry.getValue();
            SlimefunItem item = (SlimefunItem) provider;

            try {
                Config data = BlockStorage.getLocationInfo(loc);
                int energy = provider.getGeneratedOutput(loc, data);

                if (provider.isChargeable()) {
                    energy = NumberUtils.flowSafeAddition(energy, provider.getCharge(loc, data));
                }

                if (provider.willExplode(loc, data)) {
                    explodedBlocks.add(loc);
                    BlockStorage.clearBlockInfo(loc);

                    /*
                     * Evict the stale TERMINUS classification: this block's data is
                     * queued for deletion. Without re-classification the location
                     * would stay a TERMINUS forever, and a generator placed at this
                     * spot later would never be noticed (classification never
                     * "changes"), silently never joining the Network.
                     */
                    reclassify(loc);

                    Slimefun.runSync(() -> {
                        loc.getBlock().setType(Material.LAVA);
                        loc.getWorld().createExplosion(loc, 0F, false);
                    });
                } else {
                    supply = NumberUtils.flowSafeAddition(supply, energy);
                }

                // It worked - allow a fresh ErrorReport if it fails again later
                failedComponents.remove(loc);
            } catch (Exception | LinkageError throwable) {
                /*
                 * Keep the generator registered so it heals itself once the
                 * underlying problem is fixed (removing it was permanent until a
                 * rebuild), but let it contribute no energy this tick.
                 */
                reportComponentFailure(throwable, loc, item);
            }

            long time = Slimefun.getProfiler().closeEntry(loc, item, timestamp);
            timings.accept(time);
        }

        // Remove all generators which have exploded
        if (!explodedBlocks.isEmpty()) {
            generators.keySet().removeAll(explodedBlocks);
        }

        return supply;
    }

    private int tickAllCapacitors() {
        int supply = 0;

        for (Map.Entry<Location, EnergyNetComponent> entry : capacitors.entrySet()) {
            Location loc = entry.getKey();
            EnergyNetComponent component = entry.getValue();

            try {
                supply = NumberUtils.flowSafeAddition(supply, component.getCharge(loc));
                failedComponents.remove(loc);
            } catch (Exception | LinkageError x) {
                // Contributes nothing this tick, but does not poison the whole network
                reportComponentFailure(x, loc, component);
            }
        }

        return supply;
    }

    /**
     * Reports a failing {@link EnergyNetComponent}, rate-limited to one
     * {@link ErrorReport} per failure streak (see {@link #failedComponents}).
     */
    private void reportComponentFailure(@Nonnull Throwable x, @Nonnull Location loc, @Nonnull EnergyNetComponent component) {
        if (failedComponents.add(loc)) {
            if (component instanceof SlimefunItem item) {
                new ErrorReport<>(x, loc, item);
            } else {
                Slimefun.logger().log(Level.WARNING, x, () -> "An EnergyNet component failed @ " + new BlockPosition(loc));
            }
        }
    }

    /**
     * Reports a failing generator, rate-limited to one {@link ErrorReport}
     * per failure streak (see {@link #failedComponents}).
     */
    private void reportComponentFailure(@Nonnull Throwable x, @Nonnull Location loc, @Nonnull SlimefunItem item) {
        if (failedComponents.add(loc)) {
            new ErrorReport<>(x, loc, item);
        }
    }

    private void updateHologram(@Nonnull Block b, double supply, double demand) {
        if (demand > supply) {
            String netLoss = NumberUtils.getCompactDouble(demand - supply);
            updateHologram(b, "&4&l- &c" + netLoss + " &7J &e\u26A1");
        } else {
            String netGain = NumberUtils.getCompactDouble(supply - demand);
            updateHologram(b, "&2&l+ &a" + netGain + " &7J &e\u26A1");
        }
    }

    /**
     * A deterministic total order on {@link Location}s (World, then X, Y, Z),
     * used to arbitrate between multiple regulators claiming the same Network.
     */
    private static int compareLocations(@Nonnull Location a, @Nonnull Location b) {
        int result = a.getWorld().getUID().compareTo(b.getWorld().getUID());

        if (result != 0) {
            return result;
        }

        result = Integer.compare(a.getBlockX(), b.getBlockX());

        if (result != 0) {
            return result;
        }

        result = Integer.compare(a.getBlockY(), b.getBlockY());

        if (result != 0) {
            return result;
        }

        return Integer.compare(a.getBlockZ(), b.getBlockZ());
    }

    @Nullable
    private static EnergyNetComponent getComponent(@Nonnull Location l) {
        SlimefunItem item = BlockStorage.check(l);

        if (item instanceof EnergyNetComponent component) {
            return component;
        }

        return null;
    }

    /**
     * This attempts to get an {@link EnergyNet} from a given {@link Location}.
     * If no suitable {@link EnergyNet} could be found, {@code null} will be returned.
     *
     * @param l
     *            The target {@link Location}
     *
     * @return The {@link EnergyNet} at that {@link Location}, or {@code null}
     */
    @Nullable
    public static EnergyNet getNetworkFromLocation(@Nonnull Location l) {
        return Slimefun.getNetworkManager().getNetworkFromLocation(l, EnergyNet.class).orElse(null);
    }

    /**
     * This attempts to get an {@link EnergyNet} from a given {@link Location}.
     * If no suitable {@link EnergyNet} could be found, a new one will be created.
     * 
     * @param l
     *            The target {@link Location}
     * 
     * @return The {@link EnergyNet} at that {@link Location}, or a new one
     */
    @Nonnull
    public static EnergyNet getNetworkFromLocationOrCreate(@Nonnull Location l) {
        Optional<EnergyNet> energyNetwork = Slimefun.getNetworkManager().getNetworkFromLocation(l, EnergyNet.class);

        if (energyNetwork.isPresent()) {
            return energyNetwork.get();
        } else {
            EnergyNet network = new EnergyNet(l);
            Slimefun.getNetworkManager().registerNetwork(network);
            return network;
        }
    }
}

package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import org.mockito.Mockito;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Package bridge for the cargo benchmark scenario.
 *
 * <p>{@link CargoNetworkTask} and its constructor are package-private (and so is
 * {@link CargoUtils}), so the scenario cannot assemble a task from the
 * {@code benchmark.scenarios} package. This helper lives in the cargo package
 * itself (a split package across the benchmark jar and the classes under test -
 * fine on the classpath) and builds a <strong>real</strong> {@code CargoNetworkTask}
 * against a mocked {@code CargoNet}, mirroring the setup of
 * {@code TestCargoNetworkTaskItemFlow} in the project's own test suite. The task,
 * the {@code CargoUtils} insert/withdraw paths and the vanilla inventories are
 * all the production code under test; only the network shell (attached-block
 * lookup, item filter, regulator) is mocked, and it is mocked identically for
 * baseline and optimized builds.
 */
public final class BenchCargoRoute {

    private BenchCargoRoute() {}

    /**
     * Creates a mocked cargo network whose attached-block lookups route to the
     * blocks registered via {@link #attach} and whose item filters accept
     * everything (an unconfigured cargo node's effective behavior).
     *
     * @param world
     *            The world the network's regulator lives in (only used for the
     *            profiler's closing entry)
     *
     * @return A mocked {@link CargoNet}
     */
    public static CargoNet mockNetwork(World world) {
        CargoNet network = Mockito.mock(CargoNet.class);
        Mockito.lenient().when(network.getRegulator()).thenReturn(new Location(world, 0, 90, 0));

        ItemFilter allowAll = Mockito.mock(ItemFilter.class);
        Mockito.lenient().when(allowAll.test(Mockito.any())).thenReturn(true);
        Mockito.lenient().when(network.getItemFilter(Mockito.any())).thenReturn(allowAll);

        return network;
    }

    /**
     * Routes the given node location's attached-block lookup to the given target.
     *
     * @param network
     *            The mocked network
     * @param node
     *            The cargo node location
     * @param target
     *            The container block the node is attached to
     */
    public static void attach(CargoNet network, Location node, Block target) {
        Mockito.lenient().when(network.getAttachedBlock(node)).thenReturn(Optional.of(target));
    }

    /**
     * Assembles and runs a real {@link CargoNetworkTask} for one cargo tick.
     * Both construction and execution stay inside the cargo package: the task
     * class is package-private, so callers outside cannot invoke methods on it.
     *
     * @param network
     *            The (mocked) network the task operates on
     * @param inputs
     *            Input node locations mapped to their channel
     * @param outputs
     *            Output node locations grouped by channel
     */
    public static void runNewTask(CargoNet network, Map<Location, Integer> inputs, Map<Integer, List<Location>> outputs) {
        new CargoNetworkTask(network, inputs, outputs).run();
    }

    /**
     * Builds a real {@link CargoNet} whose node sets are populated directly
     * (bypassing network discovery) and whose nodes carry BlockStorage-backed
     * frequencies, then exposes a driver that performs the same per-tick
     * routing-map work {@code CargoNet#tick} does: {@code mapInputNodes()}
     * followed by {@code mapOutputNodes()}.
     *
     * <p>This measures the mapping layer itself, decoupled from holograms,
     * profiler entries and task scheduling - the parts of {@code tick()} that
     * other scenarios already cover.
     *
     * @param world
     *            The world the nodes live in (must be registered with
     *            BlockStorage)
     * @param nodesPerSide
     *            How many input and how many output nodes to build
     * @param zBase
     *            Z offset for this network's node rows (variants use disjoint
     *            bands so their BlockStorage entries never collide)
     *
     * @return A runnable performing one tick's worth of routing-map work
     */
    public static Runnable mappingDriver(World world, int nodesPerSide, int zBase) {
        try {
            CargoNet network = new CargoNet(new Location(world, 0, 100, zBase));

            java.lang.reflect.Field inputsField = CargoNet.class.getDeclaredField("inputNodes");
            inputsField.setAccessible(true);
            java.lang.reflect.Field outputsField = CargoNet.class.getDeclaredField("outputNodes");
            outputsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Set<Location> inputNodes = (Set<Location>) inputsField.get(network);
            @SuppressWarnings("unchecked")
            Set<Location> outputNodes = (Set<Location>) outputsField.get(network);

            for (int i = 0; i < nodesPerSide; i++) {
                Location input = new Location(world, i, 100, zBase);
                BlockStorage.addBlockInfo(input, "frequency", String.valueOf(i % 16), false);
                inputNodes.add(input);

                Location output = new Location(world, i, 100, zBase + 2);
                BlockStorage.addBlockInfo(output, "frequency", String.valueOf((i + 3) % 16), false);
                outputNodes.add(output);
            }

            java.lang.reflect.Method mapInputs = CargoNet.class.getDeclaredMethod("mapInputNodes");
            mapInputs.setAccessible(true);
            java.lang.reflect.Method mapOutputs = CargoNet.class.getDeclaredMethod("mapOutputNodes");
            mapOutputs.setAccessible(true);

            return () -> {
                try {
                    mapInputs.invoke(network);
                    mapOutputs.invoke(network);
                } catch (java.lang.reflect.InvocationTargetException x) {
                    if (x.getCause() instanceof RuntimeException cause) {
                        throw cause;
                    }

                    throw new IllegalStateException("mapping failed", x.getCause());
                } catch (IllegalAccessException x) {
                    throw new IllegalStateException("mapping is not accessible", x);
                }
            };
        } catch (ReflectiveOperationException x) {
            throw new IllegalStateException("Cannot build cargo mapping driver", x);
        }
    }
}

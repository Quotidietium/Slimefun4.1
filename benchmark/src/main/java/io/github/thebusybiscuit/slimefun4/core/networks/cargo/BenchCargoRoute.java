package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import org.mockito.Mockito;

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
}

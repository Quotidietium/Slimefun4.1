package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.network.NetworkComponent;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoInputNode;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Discriminant tests for the routing-map cache in {@link CargoNet}: the maps
 * are cached between ticks and rebuilt only after an invalidation - a node
 * configuration change (the documented contract, same as the {@link ItemFilter}
 * cache), a channel-selector click or a node classification change. The cache
 * must never serve data that is older than the last invalidation.
 *
 * <p>Every test normalizes the node's stored frequency first: JUnit method
 * order is arbitrary and the network (with its cache) is shared per class.
 *
 * @author Zurker
 */
class TestCargoRoutingCache {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static CargoNet network;
    private static Location inputNode;
    private static Location outputNode;

    @BeforeAll
    public static void load() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // A real network: manager regulator, one input node, one output node
        BlockStorage.addBlockInfo(new Location(world, 0, 60, 0), "id", "CARGO_MANAGER", false);
        inputNode = new Location(world, 1, 60, 0);
        BlockStorage.addBlockInfo(inputNode, "id", "CARGO_NODE_INPUT", false);
        outputNode = new Location(world, -1, 60, 0);
        BlockStorage.addBlockInfo(outputNode, "id", "CARGO_NODE_OUTPUT", false);

        network = CargoNet.getNetworkFromLocationOrCreate(new Location(world, 0, 60, 0));

        // Drives the real incremental discovery: classifies both nodes (they are
        // within the cargo range around the regulator) and registers their chunks
        // in the NetworkManager index.
        network.tick();

        Assertions.assertEquals(1, inputs().size(), "Discovery must have classified the input node");
        Assertions.assertEquals(1, outputs().size(), "Discovery must have classified the output node");
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    /**
     * Writes the given frequency and forces a cache rebuild, so each test starts
     * from a known routing state regardless of execution order.
     */
    private void normalizeFrequency(String frequency) throws Exception {
        BlockStorage.addBlockInfo(inputNode, "frequency", frequency, false);
        network.markCargoNodeConfigurationDirty(inputNode);
        mapInputs();
    }

    @SuppressWarnings("unchecked")
    private static Set<Location> inputs() throws Exception {
        var field = CargoNet.class.getDeclaredField("inputNodes");
        field.setAccessible(true);
        return (Set<Location>) field.get(network);
    }

    @SuppressWarnings("unchecked")
    private static Set<Location> outputs() throws Exception {
        var field = CargoNet.class.getDeclaredField("outputNodes");
        field.setAccessible(true);
        return (Set<Location>) field.get(network);
    }

    @SuppressWarnings("unchecked")
    private Map<Location, Integer> mapInputs() throws Exception {
        Method method = CargoNet.class.getDeclaredMethod("mapInputNodes");
        method.setAccessible(true);
        return (Map<Location, Integer>) method.invoke(network);
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, java.util.List<Location>> mapOutputs() throws Exception {
        Method method = CargoNet.class.getDeclaredMethod("mapOutputNodes");
        method.setAccessible(true);
        return (Map<Integer, java.util.List<Location>>) method.invoke(network);
    }

    @Test
    @DisplayName("The routing maps are cached: an out-of-band write does not leak in without invalidation")
    void testCacheHoldsBetweenTicks() throws Exception {
        normalizeFrequency("3");

        Map<Location, Integer> first = mapInputs();
        Assertions.assertEquals(Integer.valueOf(3), first.get(inputNode), "The normalized frequency must be served");

        // Out-of-band write (no invalidation hook): the cache contract keeps
        // serving the current maps - only an invalidation refreshes them.
        BlockStorage.addBlockInfo(inputNode, "frequency", "7", false);

        Map<Location, Integer> second = mapInputs();
        Assertions.assertSame(first, second, "Without invalidation the cached instance must be returned");
        Assertions.assertEquals(Integer.valueOf(3), second.get(inputNode), "The out-of-band write must not leak into the cached maps");
    }

    @Test
    @DisplayName("markCargoNodeConfigurationDirty rebuilds the maps from fresh data")
    void testConfigDirtyRebuilds() throws Exception {
        normalizeFrequency("3");
        mapInputs();

        BlockStorage.addBlockInfo(inputNode, "frequency", "9", false);
        network.markCargoNodeConfigurationDirty(inputNode);

        Assertions.assertEquals(Integer.valueOf(9), mapInputs().get(inputNode), "After invalidation the new frequency must be served");
    }

    @Test
    @DisplayName("A channel-selector click takes effect on the very next mapping")
    void testApplyChannelChangeInvalidates() throws Exception {
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "routing_cache_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ROUTING_CACHE_NODE", Material.BARREL, "&fRouting Cache Node");
        Slimefun.getItemCfg().setValue("_TEST_ROUTING_CACHE_NODE.enabled", true);
        CargoInputNode node = new CargoInputNode(itemGroup, stack, RecipeType.NULL, new ItemStack[9], null);
        node.register(plugin);

        normalizeFrequency("2");
        Assertions.assertEquals(Integer.valueOf(2), mapInputs().get(inputNode), "The node starts on the normalized channel");

        Block block = world.getBlockAt(inputNode);
        Player player = Mockito.mock(Player.class);

        // applyChannelChange is protected in another package - drive it via
        // reflection, exactly like the channel-selector click handlers do.
        Method apply = null;

        for (Class<?> type = node.getClass(); type != null; type = type.getSuperclass()) {
            try {
                apply = type.getDeclaredMethod("applyChannelChange", Player.class, Block.class, int.class);
                break;
            } catch (NoSuchMethodException ignored) {
                // Keep walking up the hierarchy
            }
        }

        Assertions.assertNotNull(apply, "applyChannelChange must exist on the node hierarchy");
        apply.setAccessible(true);

        try {
            Assertions.assertTrue((boolean) apply.invoke(node, player, block, 5), "An unvetoed channel change must be applied");
        } catch (java.lang.reflect.InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            throw new IllegalStateException("applyChannelChange failed", x.getCause());
        }

        Assertions.assertEquals(Integer.valueOf(5), mapInputs().get(inputNode), "applyChannelChange must invalidate the routing cache immediately");
    }

    @Test
    @DisplayName("A classification change (node removed) drops the node from the routing maps")
    void testClassificationChangeInvalidates() throws Exception {
        Assertions.assertEquals(1, mapOutputs().get(0).size(), "The output node is routed on channel 0 initially");

        // Simulates what discovery does when the node is broken: classification
        // change from TERMINUS to none removes it from the node sets.
        network.onClassificationChange(outputNode, NetworkComponent.TERMINUS, null);

        java.util.List<Location> channelZero = mapOutputs().get(0);
        Assertions.assertTrue(channelZero == null || !channelZero.contains(outputNode), "A removed output node must no longer be routed");
    }

    @Test
    @DisplayName("Rebuilt maps are new instances: a handed-out map is never mutated in place")
    void testCopyOnWriteHandoff() throws Exception {
        normalizeFrequency("3");

        Map<Location, Integer> first = mapInputs();
        network.markCargoNodeConfigurationDirty(inputNode);
        Map<Location, Integer> second = mapInputs();

        Assertions.assertNotSame(first, second, "A rebuild must publish a fresh instance (copy-on-write handoff)");
        Assertions.assertEquals(first.get(inputNode), second.get(inputNode), "Same data, different instance");
    }
}

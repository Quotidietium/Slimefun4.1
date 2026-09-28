package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.bakedlibs.dough.protection.ProtectionManager;
import io.github.bakedlibs.dough.protection.ProtectionModule;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Discriminant tests for the per-tick protection-query cache of
 * {@link CargoNetworkTask}: repeated (owner, target) permission checks within
 * one tick must be answered from the cache (the protection module sees one
 * query per distinct pair), while the routing outcomes stay identical to
 * uncached queries - denials still short-circuit the input side and skip
 * denied outputs. A fresh task (the next tick) must see protection changes
 * made in between.
 *
 * @author Zurker
 */
class TestCargoPermissionCache {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    /** Players in this set are denied by the test module. */
    private static final Set<UUID> DENIED = ConcurrentHashMap.newKeySet();

    /** How many times the test module was queried. */
    private static final AtomicInteger QUERIES = new AtomicInteger();

    @BeforeAll
    public static void load() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // The ProtectionManager is normally created by a scheduled first-tick
        // task which MockBukkit never runs - inject it like IntegrationsManager
        // would on a real server (environment setup, not code under test).
        Field field = Slimefun.getIntegrations().getClass().getDeclaredField("protectionManager");
        field.setAccessible(true);
        field.set(Slimefun.getIntegrations(), new ProtectionManager(plugin));

        Slimefun.getProtectionManager().registerModule(server.getPluginManager(), "Slimefun", DenyModule::new);

        /*
         * Cargo stand-ins under the production item ids: CargoNetworkTask#run()
         * feeds the profiler with the input-node and manager items and the
         * profiler rejects null items (same environment setup as the bench).
         */
        if (SlimefunItem.getById("CARGO_NODE_INPUT") == null) {
            ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "cargo_permission_test");
            new SlimefunItem(itemGroup, new SlimefunItemStack("CARGO_NODE_INPUT", Material.PLAYER_HEAD, "Test Cargo Input Node"), RecipeType.NULL, new ItemStack[9]).register(plugin);
            new SlimefunItem(itemGroup, new SlimefunItemStack("CARGO_MANAGER", Material.PLAYER_HEAD, "Test Cargo Manager"), RecipeType.NULL, new ItemStack[9]).register(plugin);
        }
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        DENIED.clear();
        QUERIES.set(0);
    }

    /**
     * A minimal protection module whose denials are controlled by
     * {@link #DENIED}; every query is counted.
     */
    private static final class DenyModule implements ProtectionModule {

        private final Plugin owningPlugin;

        private DenyModule(Plugin owningPlugin) {
            this.owningPlugin = owningPlugin;
        }

        @Override
        public void load() {}

        @Override
        public Plugin getPlugin() {
            return owningPlugin;
        }

        @Override
        public boolean hasPermission(OfflinePlayer player, Location l, Interaction action) {
            QUERIES.incrementAndGet();
            return !DENIED.contains(player.getUniqueId());
        }
    }

    /**
     * A mocked {@link CargoNet} with an allow-all item filter, mirroring the
     * bench setup: routing itself is not under test here, only the protection
     * queries the task performs around it.
     */
    private CargoNet stubNetwork() {
        CargoNet network = Mockito.mock(CargoNet.class);
        Mockito.lenient().when(network.getRegulator()).thenReturn(new Location(world, 0, 100, 0));

        ItemFilter allowAll = Mockito.mock(ItemFilter.class);
        Mockito.lenient().when(allowAll.test(Mockito.any())).thenReturn(true);
        Mockito.lenient().when(network.getItemFilter(Mockito.any())).thenReturn(allowAll);

        return network;
    }

    /**
     * Places a cargo node (any solid block carrying the given owner, or none)
     * with a fresh chest one block behind it.
     *
     * @return the node location
     */
    private Location placeNodeWithChest(CargoNet network, int x, int z, UUID owner) {
        Location node = new Location(world, x, 100, z);
        world.getBlockAt(node).setType(Material.STONE);

        if (owner != null) {
            BlockStorage.addBlockInfo(node, "owner", owner.toString(), false);
        }

        Block chest = world.getBlockAt(x, 100, z + 1);
        chest.setType(Material.CHEST);
        Mockito.when(network.getAttachedBlock(node)).thenReturn(Optional.of(chest));
        return node;
    }

    private Inventory chestInventory(int x, int z) {
        Block chest = world.getBlockAt(x, 100, z + 1);
        return ((InventoryHolder) chest.getState()).getInventory();
    }

    private int count(Inventory inv, Material material) {
        int total = 0;

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }

        return total;
    }

    @Test
    @DisplayName("A denied input owner withdraws nothing: the source chest stays stocked")
    void testDeniedInputShortCircuits() {
        UUID owner = UUID.nameUUIDFromBytes("denied-input-owner".getBytes());
        DENIED.add(owner);

        CargoNet network = stubNetwork();
        Location input = placeNodeWithChest(network, 10, 10, owner);
        Location output = placeNodeWithChest(network, 11, 20, owner);

        Inventory source = chestInventory(10, 10);
        source.setItem(0, new ItemStack(Material.DIAMOND, 5));

        Map<Location, Integer> inputs = new HashMap<>();
        inputs.put(input, 0);
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>();
        outputNodes.add(output);
        outputs.put(0, outputNodes);

        new CargoNetworkTask(network, inputs, outputs).run();

        Assertions.assertEquals(5, count(source, Material.DIAMOND), "A denied owner must not withdraw anything");
        Assertions.assertEquals(0, count(chestInventory(11, 20), Material.DIAMOND), "Nothing may arrive at the output either");
        Assertions.assertEquals(1, QUERIES.get(), "The denied input check must have queried the module exactly once");
    }

    @Test
    @DisplayName("A denied output is skipped and the item routes to an allowed output")
    void testDeniedOutputIsSkipped() {
        UUID allowedOwner = UUID.nameUUIDFromBytes("allowed-owner".getBytes());
        UUID deniedOwner = UUID.nameUUIDFromBytes("denied-output-owner".getBytes());
        DENIED.add(deniedOwner);

        CargoNet network = stubNetwork();
        Location input = placeNodeWithChest(network, 20, 30, allowedOwner);
        Location deniedOutput = placeNodeWithChest(network, 21, 40, deniedOwner);
        Location allowedOutput = placeNodeWithChest(network, 22, 50, allowedOwner);

        Inventory source = chestInventory(20, 30);
        source.setItem(0, new ItemStack(Material.DIAMOND, 5));
        Inventory deniedChest = chestInventory(21, 40);
        deniedChest.setItem(0, new ItemStack(Material.DIAMOND, 3));

        Map<Location, Integer> inputs = new HashMap<>();
        inputs.put(input, 0);
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>();
        outputNodes.add(deniedOutput);
        outputNodes.add(allowedOutput);
        outputs.put(0, outputNodes);

        new CargoNetworkTask(network, inputs, outputs).run();

        Assertions.assertEquals(0, count(source, Material.DIAMOND), "The source chest must have been emptied");
        Assertions.assertEquals(3, count(deniedChest, Material.DIAMOND), "The denied output must not have received anything");
        Assertions.assertEquals(5, count(chestInventory(22, 50), Material.DIAMOND), "The allowed output must have received everything");
    }

    @Test
    @DisplayName("Repeated (owner, target) pairs within one tick query the module once")
    void testCacheCollapsesDuplicateQueries() {
        UUID owner = UUID.nameUUIDFromBytes("shared-owner".getBytes());

        CargoNet network = stubNetwork();

        // 4 inputs of the same owner, each stocking a diamond stack
        Map<Location, Integer> inputs = new HashMap<>();

        for (int i = 0; i < 4; i++) {
            Location input = placeNodeWithChest(network, 30 + i, 60, owner);
            chestInventory(30 + i, 60).setItem(0, new ItemStack(Material.DIAMOND, 5));
            inputs.put(input, 0);
        }

        // 2 shared outputs of the same owner, completely full of mismatched
        // filler: every insert bounces, so every input walks BOTH outputs
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>();
        outputs.put(0, outputNodes);

        for (int o = 0; o < 2; o++) {
            Location output = placeNodeWithChest(network, 40 + o, 70, owner);
            Inventory chest = chestInventory(40 + o, 70);

            for (int slot = 0; slot < 27; slot++) {
                chest.setItem(slot, new ItemStack(Material.STONE, 1));
            }

            outputNodes.add(output);
        }

        new CargoNetworkTask(network, inputs, outputs).run();

        // Distinct (owner, target) pairs: 4 input chests + 2 output chests = 6.
        // Without the cache every route would re-query: 4 x (1 input + 2 outputs) = 12.
        Assertions.assertEquals(6, QUERIES.get(), "The module must be queried once per distinct (owner, target) pair, not once per route");

        // The bounce must also have returned everything to the sources
        for (int i = 0; i < 4; i++) {
            Assertions.assertEquals(5, count(chestInventory(30 + i, 60), Material.DIAMOND), "Bounced items must return to their sources");
        }
    }

    @Test
    @DisplayName("A fresh task (next tick) sees protection changes made between ticks")
    void testPerTickFreshness() {
        UUID owner = UUID.nameUUIDFromBytes("flip-owner".getBytes());

        CargoNet network = stubNetwork();
        Location input = placeNodeWithChest(network, 50, 80, owner);
        Location output = placeNodeWithChest(network, 51, 90, owner);

        Map<Location, Integer> inputs = new HashMap<>();
        inputs.put(input, 0);
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>();
        outputNodes.add(output);
        outputs.put(0, outputNodes);

        Inventory source = chestInventory(50, 80);
        source.setItem(0, new ItemStack(Material.DIAMOND, 5));

        DENIED.add(owner);
        new CargoNetworkTask(network, inputs, outputs).run();
        Assertions.assertEquals(5, count(source, Material.DIAMOND), "Denied on this tick: nothing withdrawn");
        Assertions.assertEquals(0, count(chestInventory(51, 90), Material.DIAMOND), "Denied on this tick: nothing delivered");

        DENIED.remove(owner);
        new CargoNetworkTask(network, inputs, outputs).run();
        Assertions.assertEquals(0, count(source, Material.DIAMOND), "Allowed on the next tick: the item moved");
        Assertions.assertEquals(5, count(chestInventory(51, 90), Material.DIAMOND), "Allowed on the next tick: the item was delivered");
    }

    @Test
    @DisplayName("An ownerless node stays allowed without consulting the protection module")
    void testOwnerlessNodeAlwaysAllowed() {
        // The module would deny this player - irrelevant for ownerless nodes
        DENIED.add(UUID.nameUUIDFromBytes("someone-else".getBytes()));

        CargoNet network = stubNetwork();
        Location input = placeNodeWithChest(network, 60, 100, null);
        Location output = placeNodeWithChest(network, 61, 110, null);

        Inventory source = chestInventory(60, 100);
        source.setItem(0, new ItemStack(Material.DIAMOND, 5));

        Map<Location, Integer> inputs = new HashMap<>();
        inputs.put(input, 0);
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>();
        outputNodes.add(output);
        outputs.put(0, outputNodes);

        new CargoNetworkTask(network, inputs, outputs).run();

        Assertions.assertEquals(0, count(source, Material.DIAMOND), "An ownerless node withdraws normally");
        Assertions.assertEquals(5, count(chestInventory(61, 110), Material.DIAMOND), "The item must have been delivered");
        Assertions.assertEquals(0, QUERIES.get(), "Ownerless nodes must not query the protection module at all");
    }
}

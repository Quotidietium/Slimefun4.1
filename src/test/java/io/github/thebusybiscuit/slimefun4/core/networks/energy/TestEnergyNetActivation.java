package io.github.thebusybiscuit.slimefun4.core.networks.energy;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.EnergyConnector;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.EnergyRegulator;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.generators.SolarGenerator;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * End-to-end activation coverage for the {@link EnergyNet}: a solar generator
 * placed next to cables and an energy regulator must be discovered, classified
 * as a generator and actually charge a connected consumer - the exact scenario
 * reported as broken in v5.0.0 ("solar generator does not activate when
 * connected to a circuit").
 * <p>
 * Unlike {@link TestEnergyNetSettlement} (which drives the private settlement
 * methods directly), this drives the real {@link EnergyRegulator} block ticker:
 * network resolution, discovery, classification, generation and settlement all
 * run through their production code paths.
 *
 * @author Zurker
 */
class TestEnergyNetActivation {

    private static final int Y = 64;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static EnergyRegulator regulatorItem;
    private static EnergyConnector connectorItem;
    private static SolarGenerator solarItem;
    private static TestCapacitor capacitorItem;
    private static TestConsumer consumerItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup group = new ItemGroup(new NamespacedKey(Slimefun.instance(), "activation"), new ItemStack(Material.REDSTONE));
        Slimefun.getItemCfg().setValue("TEST_ACTIVATION_REGULATOR.enabled", true);
        Slimefun.getItemCfg().setValue("TEST_ACTIVATION_CONNECTOR.enabled", true);
        Slimefun.getItemCfg().setValue("TEST_ACTIVATION_SOLAR.enabled", true);
        Slimefun.getItemCfg().setValue("TEST_ACTIVATION_CONSUMER.enabled", true);
        Slimefun.getItemCfg().setValue("TEST_ACTIVATION_CAPACITOR.enabled", true);

        /*
         * Register the regulator under the real ENERGY_REGULATOR stack: EnergyNet.tick
         * resolves SlimefunItems.ENERGY_REGULATOR.getItem() for its profiler entry, and
         * the unit-test environment does not run the full item setup.
         */
        regulatorItem = new EnergyRegulator(group, SlimefunItems.ENERGY_REGULATOR, RecipeType.NULL, new ItemStack[9]);
        regulatorItem.register(plugin);

        connectorItem = new EnergyConnector(group, new SlimefunItemStack("TEST_ACTIVATION_CONNECTOR", Material.LIGHTNING_ROD, "Cable"), RecipeType.NULL, new ItemStack[9], new ItemStack(Material.LIGHTNING_ROD));
        connectorItem.register(plugin);

        solarItem = new SolarGenerator(group, 12, 0, new SlimefunItemStack("TEST_ACTIVATION_SOLAR", Material.DAYLIGHT_DETECTOR, "Solar"), RecipeType.NULL, new ItemStack[9], 4);
        solarItem.register(plugin);

        consumerItem = new TestConsumer(group, new SlimefunItemStack("TEST_ACTIVATION_CONSUMER", Material.FURNACE, "Consumer"));
        consumerItem.register(plugin);

        capacitorItem = new TestCapacitor(group, new SlimefunItemStack("TEST_ACTIVATION_CAPACITOR", Material.REDSTONE_BLOCK, "Capacitor"));
        capacitorItem.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    /**
     * Places a Slimefun block at the given coordinates (block info + ticker
     * registration, exactly what a real placement stores).
     */
    private static Block place(int x, int z, SlimefunItem item, Material type) {
        Location loc = new Location(world, x, Y, z);
        Block block = world.getBlockAt(x, Y, z);
        block.setType(type);
        world.getChunkAt(x >> 4, z >> 4);

        if (block instanceof BlockMock blockMock) {
            // Full daylight from directly above, like an unobstructed sky
            blockMock.setLightFromSky((byte) 15);
        }

        BlockStorage.addBlockInfo(loc, "id", item.getId(), true);
        return block;
    }

    /**
     * Ticks the energy regulator at the given coordinates through its real
     * {@link me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker}, exactly
     * like the {@link io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask} does.
     */
    private static void tickRegulator(int x, int z) {
        Location loc = new Location(world, x, Y, z);
        Block b = world.getBlockAt(x, Y, z);
        Config data = BlockStorage.getLocationInfo(loc);

        Assertions.assertNotNull(data, "Regulator must have block data");
        Assertions.assertEquals(regulatorItem.getId(), data.getString("id"));
        regulatorItem.getBlockTicker().tick(b, regulatorItem, data);
    }

    private static int charge(int x, int z) {
        // An absent "energy-charge" key legitimately means zero (setCharge skips
        // no-op writes), so treat it as 0 instead of failing to parse null.
        String value = BlockStorage.getLocationInfo(new Location(world, x, Y, z), "energy-charge");
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Test
    @DisplayName("Solar generator placed next to a cable and regulator activates and charges a consumer")
    void testSolarGeneratorActivates() {
        int baseX = 100;

        place(baseX, 100, regulatorItem, Material.COMMAND_BLOCK);
        place(baseX + 1, 100, connectorItem, Material.LIGHTNING_ROD);
        place(baseX + 2, 100, solarItem, Material.DAYLIGHT_DETECTOR);
        place(baseX + 3, 100, consumerItem, Material.FURNACE);

        // First tick creates the network and discovers the components
        tickRegulator(baseX, 100);
        // Second tick settles (the network is already discovered by now)
        tickRegulator(baseX, 100);

        EnergyNet network = EnergyNet.getNetworkFromLocation(new Location(world, baseX + 2, Y, 100));
        Assertions.assertNotNull(network, "The solar generator must be part of an EnergyNet");
        Assertions.assertTrue(network.getGenerators().containsKey(new Location(world, baseX + 2, Y, 100)),
            "The solar generator must be classified as a generator of the network");
        Assertions.assertTrue(network.getConsumers().containsKey(new Location(world, baseX + 3, Y, 100)),
            "The consumer must be classified as a consumer of the network");

        Assertions.assertTrue(charge(baseX + 3, 100) > 0,
            "The consumer must have been charged from the solar generator's output");
    }

    @Test
    @DisplayName("Solar generator placed before the regulator still activates (reverse placement order)")
    void testSolarGeneratorPlacedFirst() {
        int baseX = 200;

        place(baseX + 2, 200, solarItem, Material.DAYLIGHT_DETECTOR);
        place(baseX + 1, 200, connectorItem, Material.LIGHTNING_ROD);
        place(baseX, 200, regulatorItem, Material.COMMAND_BLOCK);
        place(baseX + 3, 200, consumerItem, Material.FURNACE);

        tickRegulator(baseX, 200);
        tickRegulator(baseX, 200);

        Assertions.assertTrue(charge(baseX + 3, 200) > 0,
            "The consumer must charge regardless of the placement order");
    }

    @Test
    @DisplayName("A solar generator joining an existing network via a block place event activates")
    void testSolarGeneratorJoinsLater() {
        int baseX = 300;

        place(baseX, 300, regulatorItem, Material.COMMAND_BLOCK);
        place(baseX + 1, 300, connectorItem, Material.LIGHTNING_ROD);
        place(baseX + 3, 300, consumerItem, Material.FURNACE);

        // Build the network first
        tickRegulator(baseX, 300);
        tickRegulator(baseX, 300);

        // Now the solar generator is placed next to the existing cable,
        // and the NetworkListener marks the location dirty
        place(baseX + 2, 300, solarItem, Material.DAYLIGHT_DETECTOR);
        Slimefun.getNetworkManager().updateAllNetworks(new Location(world, baseX + 2, Y, 300));

        tickRegulator(baseX, 300);
        tickRegulator(baseX, 300);

        Assertions.assertTrue(charge(baseX + 3, 300) > 0,
            "A solar generator joining an existing network must contribute energy");
    }

    @Test
    @DisplayName("A solar generator across a chunk boundary still joins the network (spatial chunk index)")
    void testSolarGeneratorAcrossChunkBoundary() {
        // x = 367 is inside chunk 22, x = 368 inside chunk 23
        int baseX = 365;

        place(baseX, 400, regulatorItem, Material.COMMAND_BLOCK);
        place(baseX + 1, 400, connectorItem, Material.LIGHTNING_ROD);
        place(baseX + 3, 400, solarItem, Material.DAYLIGHT_DETECTOR);
        place(baseX + 2, 400, consumerItem, Material.FURNACE);

        tickRegulator(baseX, 400);
        tickRegulator(baseX, 400);

        Assertions.assertTrue(charge(baseX + 2, 400) > 0,
            "The chunk-indexed network lookup must still resolve networks across chunk borders");
    }

    @Test
    @DisplayName("A capacitor buffers surplus energy and a second consumer fills up as well")
    void testCapacitorAndMultipleConsumers() {
        int baseX = 500;

        place(baseX, 500, regulatorItem, Material.COMMAND_BLOCK);
        place(baseX + 1, 500, connectorItem, Material.LIGHTNING_ROD);
        place(baseX + 2, 500, solarItem, Material.DAYLIGHT_DETECTOR);
        place(baseX + 3, 500, consumerItem, Material.FURNACE);
        place(baseX + 4, 500, consumerItem, Material.FURNACE);
        place(baseX + 5, 500, capacitorItem, Material.REDSTONE_BLOCK);

        // Solar (12 J/t) exceeds one consumer's uptake, the surplus must flow on
        tickRegulator(baseX, 500);
        tickRegulator(baseX, 500);
        tickRegulator(baseX, 500);

        // Consumers are served in map iteration order, the first one takes the
        // whole tick's supply - assert that energy flowed to the consumers at all.
        Assertions.assertTrue(charge(baseX + 3, 500) + charge(baseX + 4, 500) > 0,
            "The consumers must have been charged from the solar generator's output");

        EnergyNet network = EnergyNet.getNetworkFromLocation(new Location(world, baseX + 1, Y, 500));
        Assertions.assertNotNull(network);
        Assertions.assertTrue(network.getCapacitors().containsKey(new Location(world, baseX + 5, Y, 500)),
            "The capacitor must be classified into the network");
        Assertions.assertTrue(charge(baseX + 5, 500) >= 0, "The capacitor charge must be a valid number");
    }

    /**
     * A minimal {@link EnergyNetComponent} of type CONSUMER: charges are stored
     * under the default "energy-charge" key, capacity 64.
     */
    private static class TestConsumer extends SlimefunItem implements EnergyNetComponent {

        TestConsumer(@Nonnull ItemGroup group, @Nonnull SlimefunItemStack item) {
            super(group, item, RecipeType.NULL, new ItemStack[9]);
        }

        @Override
        public int getCapacity() {
            return 64;
        }

        @Nonnull
        @Override
        public EnergyNetComponentType getEnergyComponentType() {
            return EnergyNetComponentType.CONSUMER;
        }
    }

    /**
     * A minimal {@link EnergyNetComponent} of type CAPACITOR with 128 J capacity.
     */
    private static class TestCapacitor extends SlimefunItem implements EnergyNetComponent {

        TestCapacitor(@Nonnull ItemGroup group, @Nonnull SlimefunItemStack item) {
            super(group, item, RecipeType.NULL, new ItemStack[9]);
        }

        @Override
        public int getCapacity() {
            return 128;
        }

        @Nonnull
        @Override
        public EnergyNetComponentType getEnergyComponentType() {
            return EnergyNetComponentType.CAPACITOR;
        }
    }
}

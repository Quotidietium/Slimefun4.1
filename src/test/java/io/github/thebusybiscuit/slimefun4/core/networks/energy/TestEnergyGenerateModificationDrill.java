package io.github.thebusybiscuit.slimefun4.core.networks.energy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongConsumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.EnergyGenerateEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Addon-developer contract drill for {@link EnergyGenerateEvent} modification paths.
 * Unlike {@link TestEnergyNetSettlement} (which covers the cancel and exception
 * contracts), this class registers a <em>real</em> listener via the plugin manager and
 * drives the real {@code tickAllGenerators}/{@code storeRemainingEnergy} settlement to
 * verify that {@code setEnergy(...)} adjustments land in the actual supply and stored
 * charge exactly as the event Javadoc promises.
 *
 * @author Zurker
 */
class TestEnergyGenerateModificationDrill {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static TestDrillGenerator generator;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup group = new ItemGroup(new NamespacedKey(Slimefun.instance(), "drill"), new ItemStack(Material.REDSTONE));
        Slimefun.getItemCfg().setValue("TEST_DRILL_GENERATOR.enabled", true);

        generator = new TestDrillGenerator(group, new SlimefunItemStack("TEST_DRILL_GENERATOR", Material.FURNACE, "Drill Generator"));
        generator.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @AfterEach
    public void afterEach() {
        HandlerList.unregisterAll();
        server.getPluginManager().clearEvents();
    }

    /** An addon that doubles every contribution it sees. */
    private static class DoublingListener implements Listener {

        @EventHandler
        public void onGenerate(EnergyGenerateEvent e) {
            e.setEnergy(e.getEnergy() * 2);
        }
    }

    /** An addon that confiscates the entire visible contribution (including pooled stored charge). */
    private static class ZeroingListener implements Listener {

        @EventHandler
        public void onGenerate(EnergyGenerateEvent e) {
            e.setEnergy(0);
        }
    }

    @Test
    @DisplayName("A listener doubling the contribution doubles the real supply (addon drill)")
    void testDoublingDrill() throws ReflectiveOperationException {
        server.getPluginManager().registerEvents(new DoublingListener(), plugin);

        EnergyNet network = createNetwork(10, 10, 50);
        Set<Location> unpooled = new HashSet<>();
        int supply = tickGenerators(network, unpooled);

        // visible value = generated (10) + pooled stored (50) = 60, doubled -> 120
        Assertions.assertEquals(120, supply, "The addon's doubled contribution must reach the real supply");
        Assertions.assertTrue(unpooled.isEmpty(), "An accepted (modified) contribution is still pooled and settled");

        store(network, 0, unpooled);
        Assertions.assertEquals(0, storedCharge(generatorLocation(network)), "Nothing left over - the settlement rewrites the pooled generator");

        // A leftover share must still be written back despite the modification
        EnergyNet network2 = createNetwork(20, 20, 50);
        Set<Location> unpooled2 = new HashSet<>();
        tickGenerators(network2, unpooled2);
        store(network2, 7, unpooled2);
        Assertions.assertEquals(7, storedCharge(generatorLocation(network2)), "The leftover share is written back to the modified generator");
    }

    @Test
    @DisplayName("A listener zeroing the contribution contributes nothing but stays settled (addon drill)")
    void testZeroingDrill() throws ReflectiveOperationException {
        server.getPluginManager().registerEvents(new ZeroingListener(), plugin);

        EnergyNet network = createNetwork(30, 10, 50);
        Set<Location> unpooled = new HashSet<>();
        int supply = tickGenerators(network, unpooled);

        Assertions.assertEquals(0, supply, "A zeroed contribution feeds nothing into the network");
        Assertions.assertTrue(unpooled.isEmpty(), "Zeroing is not cancelling - the stored charge was pooled and the generator stays settled");

        store(network, 9, unpooled);
        Assertions.assertEquals(9, storedCharge(generatorLocation(network)), "The pooled generator receives its leftover share");
    }

    @Test
    @DisplayName("setEnergy rejects negative values even for a registered addon listener")
    void testNegativeModificationRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            EnergyGenerateEvent event = new EnergyGenerateEvent(generator, new Location(world, 1, 60, 1), 5);
            event.setEnergy(-1);
        });
    }

    private EnergyNet createNetwork(int x, int z, int storedCharge) throws ReflectiveOperationException {
        EnergyNet network = new EnergyNet(new Location(world, x, 60, z));

        Location generatorLocation = new Location(world, x + 1, 60, z);
        BlockStorage.addBlockInfo(generatorLocation, "id", generator.getId(), false);
        BlockStorage.addBlockInfo(generatorLocation, "energy-charge", String.valueOf(storedCharge), false);

        java.lang.reflect.Field field = EnergyNet.class.getDeclaredField("generators");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Location, EnergyNetProvider> generators = (Map<Location, EnergyNetProvider>) field.get(network);
        generators.put(generatorLocation, generator);

        return network;
    }

    private Location generatorLocation(EnergyNet network) {
        return network.getGenerators().keySet().iterator().next();
    }

    private int tickGenerators(EnergyNet network, Set<Location> unpooled) throws ReflectiveOperationException {
        Method method = EnergyNet.class.getDeclaredMethod("tickAllGenerators", LongConsumer.class, Set.class);
        method.setAccessible(true);

        try {
            return (int) method.invoke(network, (LongConsumer) time -> {
            }, unpooled);
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            throw new IllegalStateException("tickAllGenerators failed", x.getCause());
        }
    }

    private void store(EnergyNet network, int remainingEnergy, Set<Location> unpooled) throws ReflectiveOperationException {
        Method method = EnergyNet.class.getDeclaredMethod("storeRemainingEnergy", int.class, Set.class);
        method.setAccessible(true);

        try {
            method.invoke(network, remainingEnergy, unpooled);
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            throw new IllegalStateException("storeRemainingEnergy failed", x.getCause());
        }
    }

    private int storedCharge(Location l) {
        return Integer.parseInt(BlockStorage.getLocationInfo(l, "energy-charge"));
    }

    /**
     * Minimal chargeable {@link EnergyNetProvider}: 10 J output per tick, 64 J capacity.
     */
    private static class TestDrillGenerator extends SlimefunItem implements EnergyNetProvider {

        TestDrillGenerator(ItemGroup group, SlimefunItemStack item) {
            super(group, item, RecipeType.NULL, new ItemStack[9]);
        }

        @Override
        public int getCapacity() {
            return 64;
        }

        @Override
        public int getGeneratedOutput(Location l, Config data) {
            return 10;
        }
    }
}

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
 * Regression coverage for the {@link EnergyNet} settlement invariants around stored
 * generator charge:
 * <ul>
 * <li>A cancelled {@link EnergyGenerateEvent} contributes nothing, and the generator's
 * stored charge must stay untouched (the documented event contract) - previously the
 * settlement rewrote EVERY generator to the remaining share (or zero), wiping stored
 * charge that never entered the network.</li>
 * <li>A generator throwing an exception keeps its stored charge for the same reason.</li>
 * <li>A generator whose contribution was accepted has its stored charge pooled and
 * rewritten by the settlement, as before.</li>
 * </ul>
 * The private settlement methods are driven directly via reflection: a full
 * {@code EnergyNet#tick} needs holograms and node discovery, which are outside the
 * scope of these invariants.
 *
 * @author Zurker
 */
class TestEnergyNetSettlement {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static TestGenerator generator;
    private static TestGenerator brokenGenerator;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup group = new ItemGroup(new NamespacedKey(Slimefun.instance(), "settlement"), new ItemStack(Material.REDSTONE));
        Slimefun.getItemCfg().setValue("TEST_SETTLEMENT_GENERATOR.enabled", true);
        Slimefun.getItemCfg().setValue("TEST_SETTLEMENT_BROKEN_GENERATOR.enabled", true);

        generator = new TestGenerator(group, new SlimefunItemStack("TEST_SETTLEMENT_GENERATOR", Material.FURNACE, "Test Generator"), false);
        generator.register(plugin);

        brokenGenerator = new TestGenerator(group, new SlimefunItemStack("TEST_SETTLEMENT_BROKEN_GENERATOR", Material.FURNACE, "Broken Generator"), true);
        brokenGenerator.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    /**
     * Creates an unregistered {@link EnergyNet} (the settlement logic does not need the
     * manager) with the given generator installed at the given location, stocked with
     * the given stored charge.
     */
    private EnergyNet createNetwork(int x, int z, TestGenerator gen, int storedCharge) throws ReflectiveOperationException {
        EnergyNet network = new EnergyNet(new Location(world, x, 60, z));

        Location generatorLocation = new Location(world, x + 1, 60, z);
        BlockStorage.addBlockInfo(generatorLocation, "id", gen.getId(), false);
        BlockStorage.addBlockInfo(generatorLocation, "energy-charge", String.valueOf(storedCharge), false);

        java.lang.reflect.Field field = EnergyNet.class.getDeclaredField("generators");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Location, EnergyNetProvider> generators = (Map<Location, EnergyNetProvider>) field.get(network);
        generators.put(generatorLocation, gen);

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

    @Test
    @DisplayName("A cancelled EnergyGenerateEvent leaves the generator's stored charge untouched")
    void testCancelledEventKeepsStoredCharge() throws ReflectiveOperationException {
        EnergyNet network = createNetwork(10, 10, generator, 30);
        Location loc = generatorLocation(network);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onGenerate(EnergyGenerateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            Set<Location> unpooled = new HashSet<>();
            int supply = tickGenerators(network, unpooled);

            Assertions.assertEquals(0, supply, "A cancelled generator must contribute nothing");

            store(network, 0, unpooled);

            Assertions.assertEquals(30, storedCharge(loc),
                "The stored charge never entered the network - wiping it violates the EnergyGenerateEvent contract");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A generator throwing an exception keeps its stored charge")
    void testBrokenGeneratorKeepsStoredCharge() throws ReflectiveOperationException {
        EnergyNet network = createNetwork(20, 20, brokenGenerator, 30);
        Location loc = generatorLocation(network);

        /*
         * Pre-seed the rate-limit set so this failure streak does not try to write an
         * ErrorReport file (unavailable in unit tests) - the settlement invariant being
         * tested here is independent of the reporting.
         */
        java.lang.reflect.Field failedField = EnergyNet.class.getDeclaredField("failedComponents");
        failedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Location> failedComponents = (Set<Location>) failedField.get(network);
        failedComponents.add(loc);

        Set<Location> unpooled = new HashSet<>();
        int supply = tickGenerators(network, unpooled);

        Assertions.assertEquals(0, supply, "A broken generator must contribute nothing");

        store(network, 0, unpooled);

        Assertions.assertEquals(30, storedCharge(loc),
            "The stored charge was never pooled into the supply - the settlement must not rewrite it");
    }

    @Test
    @DisplayName("An accepted contribution pools the stored charge and the settlement rewrites it")
    void testAcceptedContributionIsSettled() throws ReflectiveOperationException {
        EnergyNet network = createNetwork(30, 30, generator, 30);
        Location loc = generatorLocation(network);

        Set<Location> unpooled = new HashSet<>();
        int supply = tickGenerators(network, unpooled);

        Assertions.assertEquals(40, supply, "Generated output (10) plus pooled stored charge (30)");

        // Nothing left over after feeding consumers: the pooled generator is rewritten to 0
        store(network, 0, unpooled);
        Assertions.assertEquals(0, storedCharge(loc), "A pooled generator must be rewritten by the settlement");
    }

    @Test
    @DisplayName("Leftover energy is stored back into pooled generators")
    void testLeftoverEnergyStoredBack() throws ReflectiveOperationException {
        EnergyNet network = createNetwork(40, 40, generator, 30);
        Location loc = generatorLocation(network);

        Set<Location> unpooled = new HashSet<>();
        tickGenerators(network, unpooled);

        store(network, 7, unpooled);
        Assertions.assertEquals(7, storedCharge(loc), "The remaining share must be written back to the pooled generator");
    }

    /**
     * Minimal chargeable {@link EnergyNetProvider}: 10 J output per tick, 64 J capacity,
     * optionally throwing from {@link #getGeneratedOutput(Location, Config)}.
     */
    private static class TestGenerator extends SlimefunItem implements EnergyNetProvider {

        private final boolean broken;

        TestGenerator(ItemGroup group, SlimefunItemStack item, boolean broken) {
            super(group, item, RecipeType.NULL, new ItemStack[9]);
            this.broken = broken;
        }

        @Override
        public int getCapacity() {
            return 64;
        }

        @Override
        public int getGeneratedOutput(Location l, Config data) {
            if (broken) {
                throw new IllegalStateException("Simulated broken generator");
            }

            return 10;
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.ReactorProduceByproductEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the reactor byproduct API expansion:
 * {@link ReactorProduceByproductEvent}, exercised by driving a {@link NuclearReactor}
 * straight into its byproduct path with an already-finished {@link FuelOperation}, so the
 * water/coolant/energy prerequisites of {@code generateEnergy} are never hit.
 *
 * @author Zurker
 */
class TestReactorProduceByproductEvent {

    private static final int OUTPUT_SLOT = 40;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static NuclearReactor reactor;
    private static final ItemStack byproduct = new ItemStack(Material.NETHERITE_INGOT);

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "reactor_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_NUCLEAR_REACTOR", Material.DISPENSER, "&fTest Nuclear Reactor");
        Slimefun.getItemCfg().setValue("TEST_NUCLEAR_REACTOR.enabled", true);
        reactor = new NuclearReactor(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public int getEnergyProduction() {
                return 100;
            }

            @Override
            public int getCapacity() {
                return 512;
            }
        };
        reactor.register(plugin);
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
     * Places the reactor as a real block backed by {@link BlockStorage} and returns its menu.
     */
    private BlockMenu placeReactor(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", reactor.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Starts a finished fuel operation on the reactor and runs one output tick, which routes
     * straight into {@code createByproduct}.
     */
    private void runFinishedOperation(Block b) {
        Location l = b.getLocation();
        FuelOperation operation = new FuelOperation(new ItemStack(Material.IRON_INGOT), byproduct, 1);
        operation.addProgress(1);

        reactor.getMachineProcessor().startOperation(l, operation);

        Config data = BlockStorage.getLocationInfo(l);
        reactor.getGeneratedOutput(l, data);
    }

    @Test
    @DisplayName("ReactorProduceByproductEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location l = new Location(world, 1, 1, 1);
        ItemStack result = new ItemStack(Material.DIAMOND);

        ReactorProduceByproductEvent event = new ReactorProduceByproductEvent(reactor, l, result);

        Assertions.assertEquals(reactor, event.getReactor());
        Assertions.assertEquals(l, event.getLocation());
        Assertions.assertEquals(result, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.EMERALD);
        event.setResult(swapped);
        Assertions.assertEquals(swapped, event.getResult());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorProduceByproductEvent(null, l, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorProduceByproductEvent(reactor, null, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorProduceByproductEvent(reactor, l, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("A finished fuel operation fires the event and pushes the byproduct to the output")
    void testByproductFiresAndProduces() {
        Block b = world.getBlockAt(10, 1, 10);
        BlockMenu menu = placeReactor(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProduce(ReactorProduceByproductEvent event) {
                seen[0] = true;
                Assertions.assertEquals(reactor, event.getReactor());
                Assertions.assertTrue(byproduct.isSimilar(event.getResult()), "The byproduct must match the operation result");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runFinishedOperation(b);

            Assertions.assertTrue(seen[0], "ReactorProduceByproductEvent was not fired");
            Assertions.assertTrue(byproduct.isSimilar(menu.getItemInSlot(OUTPUT_SLOT)), "The byproduct must have been pushed to the output slot");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ReactorProduceByproductEvent produces no byproduct")
    void testEventCancellationProducesNothing() {
        Block b = world.getBlockAt(20, 1, 20);
        BlockMenu menu = placeReactor(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onProduce(ReactorProduceByproductEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            runFinishedOperation(b);

            ItemStack output = menu.getItemInSlot(OUTPUT_SLOT);
            Assertions.assertTrue(output == null || output.getType() == Material.AIR, "A cancelled byproduct must produce nothing");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the result via setResult produces the custom byproduct instead")
    void testResultSwapProducesCustomItem() {
        Block b = world.getBlockAt(30, 1, 30);
        BlockMenu menu = placeReactor(30, 30);
        ItemStack custom = new ItemStack(Material.GOLDEN_APPLE);

        Listener swapping = new Listener() {
            @EventHandler
            public void onProduce(ReactorProduceByproductEvent event) {
                event.setResult(custom);
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            runFinishedOperation(b);

            Assertions.assertTrue(custom.isSimilar(menu.getItemInSlot(OUTPUT_SLOT)), "The custom byproduct must have been produced");
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Producing without listeners still pushes the byproduct, preserving the old behavior")
    void testProduceWithoutListenersStillProduces() {
        Block b = world.getBlockAt(40, 1, 40);
        BlockMenu menu = placeReactor(40, 40);

        runFinishedOperation(b);

        Assertions.assertTrue(byproduct.isSimilar(menu.getItemInSlot(OUTPUT_SLOT)), "The byproduct must have been pushed to the output slot");
    }
}

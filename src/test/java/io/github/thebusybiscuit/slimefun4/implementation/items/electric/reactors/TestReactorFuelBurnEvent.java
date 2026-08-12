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

import io.github.thebusybiscuit.slimefun4.api.events.ReactorFuelBurnEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the reactor fuel API expansion: {@link ReactorFuelBurnEvent},
 * exercised by driving a {@link NuclearReactor} with no running operation through
 * {@link Reactor#getGeneratedOutput(Location, Config)} with uranium in its fuel slot.
 * <p>
 * The fuel-burn path touches no hologram, so the outcome is fully observable: a started
 * {@code FuelOperation} on the processor, a consumed fuel slot, and zero energy for the
 * tick. A vetoed burn leaves the fuel untouched and starts no operation.
 *
 * @author Zurker
 */
class TestReactorFuelBurnEvent {

    private static final int FUEL_SLOT = 19;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static NuclearReactor reactor;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "reactor_fuel_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_FUEL_REACTOR", Material.DISPENSER, "&fTest Fuel Reactor");
        Slimefun.getItemCfg().setValue("TEST_FUEL_REACTOR.enabled", true);
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
     * Runs one output tick with no running operation, which lands on the fuel-burn path.
     */
    private int runBurnTick(Block b) {
        Location l = b.getLocation();
        Config data = BlockStorage.getLocationInfo(l);
        return reactor.getGeneratedOutput(l, data);
    }

    private boolean hasOperation(Block b) {
        return reactor.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    /**
     * {@link BlockMenu#consumeItem(int, int)} leaves a zero-amount stack behind instead of
     * clearing the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertFuelConsumed(BlockMenu menu) {
        ItemStack slot = menu.getItemInSlot(FUEL_SLOT);
        Assertions.assertTrue(slot == null || slot.getAmount() == 0, "The fuel must have been consumed, got: " + slot);
    }

    @Test
    @DisplayName("ReactorFuelBurnEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location l = new Location(world, 1, 1, 1);

        ReactorFuelBurnEvent event = new ReactorFuelBurnEvent(reactor, l, reactor.getFuelTypes().iterator().next(), FUEL_SLOT);

        Assertions.assertEquals(reactor, event.getReactor());
        Assertions.assertEquals(l, event.getLocation());
        Assertions.assertEquals(reactor.getFuelTypes().iterator().next(), event.getFuel());
        Assertions.assertEquals(FUEL_SLOT, event.getSlot());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorFuelBurnEvent(null, l, reactor.getFuelTypes().iterator().next(), FUEL_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorFuelBurnEvent(reactor, null, reactor.getFuelTypes().iterator().next(), FUEL_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorFuelBurnEvent(reactor, l, null, FUEL_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorFuelBurnEvent(reactor, l, reactor.getFuelTypes().iterator().next(), -1));
    }

    @Test
    @DisplayName("Burning fuel fires the event, consumes the fuel and starts the operation")
    void testBurnFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 1, 10);
        BlockMenu menu = placeReactor(10, 10);
        menu.replaceExistingItem(FUEL_SLOT, SlimefunItems.URANIUM.item());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(ReactorFuelBurnEvent event) {
                seen[0] = true;
                Assertions.assertEquals(reactor, event.getReactor());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertEquals(FUEL_SLOT, event.getSlot());
                Assertions.assertTrue(SlimefunItems.URANIUM.item().isSimilar(event.getFuel().getInput()), "The fuel must be uranium");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            int produced = runBurnTick(b);

            Assertions.assertEquals(0, produced, "A burn tick must yield no energy yet");
            Assertions.assertTrue(seen[0], "ReactorFuelBurnEvent was not fired");
            assertFuelConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The fuel operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ReactorFuelBurnEvent keeps the fuel and starts no operation")
    void testCancelKeepsFuelAndIdles() {
        Block b = world.getBlockAt(20, 1, 20);
        BlockMenu menu = placeReactor(20, 20);
        menu.replaceExistingItem(FUEL_SLOT, SlimefunItems.URANIUM.item());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBurn(ReactorFuelBurnEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            int produced = runBurnTick(b);

            Assertions.assertEquals(0, produced, "A vetoed burn must yield no energy");
            ItemStack slot = menu.getItemInSlot(FUEL_SLOT);
            Assertions.assertNotNull(slot, "A vetoed burn must keep the fuel");
            Assertions.assertEquals(1, slot.getAmount(), "A vetoed burn must keep the fuel untouched");
            Assertions.assertFalse(hasOperation(b), "A vetoed burn must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Burning without listeners still consumes the fuel, preserving the old behavior")
    void testBurnWithoutListenersBurns() {
        Block b = world.getBlockAt(30, 1, 30);
        BlockMenu menu = placeReactor(30, 30);
        menu.replaceExistingItem(FUEL_SLOT, SlimefunItems.URANIUM.item());

        runBurnTick(b);

        assertFuelConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The fuel operation must have been started");
    }

    @Test
    @DisplayName("The operation duration defaults to the fuel ticks and is modifiable and validated")
    void testTicksDefaultAndValidation() {
        Location l = new Location(world, 2, 1, 2);
        MachineFuel fuel = reactor.getFuelTypes().iterator().next();

        ReactorFuelBurnEvent event = new ReactorFuelBurnEvent(reactor, l, fuel, FUEL_SLOT);

        Assertions.assertEquals(fuel.getTicks(), event.getTicks(), "The duration must default to the fuel's ticks");

        event.setTicks(40);
        Assertions.assertEquals(40, event.getTicks());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTicks(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTicks(-1));
    }

    @Test
    @DisplayName("A modified duration is applied to the started operation without touching the fuel definition")
    void testModifiedTicksAppliedToOperationOnly() {
        Block b = world.getBlockAt(60, 1, 60);
        BlockMenu menu = placeReactor(60, 60);
        menu.replaceExistingItem(FUEL_SLOT, SlimefunItems.URANIUM.item());

        MachineFuel fuel = reactor.getFuelTypes().iterator().next();
        int originalTicks = fuel.getTicks();

        Listener slowing = new Listener() {
            @EventHandler
            public void onBurn(ReactorFuelBurnEvent event) {
                event.setTicks(7);
            }
        };
        server.getPluginManager().registerEvents(slowing, plugin);

        try {
            runBurnTick(b);

            assertFuelConsumed(menu);

            FuelOperation operation = reactor.getMachineProcessor().getOperation(b.getLocation());
            Assertions.assertNotNull(operation, "The fuel operation must have been started");
            Assertions.assertEquals(7, operation.getTotalTicks(), "The operation must run with the modified duration");
            Assertions.assertEquals(originalTicks, fuel.getTicks(), "The shared fuel definition must keep its original duration");
        } finally {
            HandlerList.unregisterAll(slowing);
        }
    }

    @Test
    @DisplayName("An unchanged duration starts the operation with the fuel's own ticks")
    void testUnchangedTicksKeepsFuelDuration() {
        Block b = world.getBlockAt(70, 1, 70);
        BlockMenu menu = placeReactor(70, 70);
        menu.replaceExistingItem(FUEL_SLOT, SlimefunItems.URANIUM.item());

        MachineFuel fuel = reactor.getFuelTypes().iterator().next();

        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(ReactorFuelBurnEvent event) {
                // Only observe, do not touch the duration
                Assertions.assertEquals(fuel.getTicks(), event.getTicks());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runBurnTick(b);

            FuelOperation operation = reactor.getMachineProcessor().getOperation(b.getLocation());
            Assertions.assertNotNull(operation, "The fuel operation must have been started");
            Assertions.assertEquals(fuel.getTicks(), operation.getTotalTicks(), "An untouched duration must reproduce the fuel's ticks");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An empty fuel slot fires no event and starts no operation")
    void testEmptySlotFiresNothing() {
        Block b = world.getBlockAt(40, 1, 40);
        placeReactor(40, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(ReactorFuelBurnEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            int produced = runBurnTick(b);

            Assertions.assertEquals(0, produced, "An idle reactor must yield no energy");
            Assertions.assertFalse(seen[0], "No event must be fired without fuel");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A non-fuel item in the fuel slot fires no event and starts no operation")
    void testWrongItemFiresNothing() {
        Block b = world.getBlockAt(50, 1, 50);
        BlockMenu menu = placeReactor(50, 50);
        menu.replaceExistingItem(FUEL_SLOT, new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(ReactorFuelBurnEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            int produced = runBurnTick(b);

            Assertions.assertEquals(0, produced, "An idle reactor must yield no energy");
            Assertions.assertFalse(seen[0], "No event must be fired for a non-fuel item");
            ItemStack slot = menu.getItemInSlot(FUEL_SLOT);
            Assertions.assertNotNull(slot, "The foreign item must have stayed put");
            Assertions.assertEquals(Material.DIRT, slot.getType());
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

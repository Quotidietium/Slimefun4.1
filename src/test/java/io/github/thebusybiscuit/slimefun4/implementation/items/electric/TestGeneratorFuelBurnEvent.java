package io.github.thebusybiscuit.slimefun4.implementation.items.electric;

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

import io.github.thebusybiscuit.slimefun4.api.events.GeneratorFuelBurnEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the generator fuel API expansion:
 * {@link GeneratorFuelBurnEvent}, exercised by driving an {@link AGenerator} with no
 * running operation through {@link AGenerator#getGeneratedOutput(Location, Config)}
 * with coal in its input slot.
 * <p>
 * The fuel-burn path touches no hologram, so the outcome is fully observable: a started
 * {@code FuelOperation} on the processor, a consumed fuel slot, and zero energy for the
 * tick. A vetoed burn leaves the fuel untouched and starts no operation.
 *
 * @author Zurker
 */
class TestGeneratorFuelBurnEvent {

    private static final int FUEL_SLOT = 19;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AGenerator generator;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "generator_fuel_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_FUEL_GENERATOR", Material.DISPENSER, "&fTest Fuel Generator");
        Slimefun.getItemCfg().setValue("TEST_FUEL_GENERATOR.enabled", true);
        generator = new AGenerator(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ItemStack getProgressBar() {
                return new ItemStack(Material.FLINT_AND_STEEL);
            }

            @Override
            protected void registerDefaultFuelTypes() {
                registerFuel(new MachineFuel(8, new ItemStack(Material.COAL)));
            }
        };
        generator.setCapacity(512).setEnergyProduction(100);
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

    /**
     * Places the generator as a real block backed by {@link BlockStorage} and returns its menu.
     */
    private BlockMenu placeGenerator(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", generator.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one output tick with no running operation, which lands on the fuel-burn path.
     */
    private int runBurnTick(Block b) {
        Location l = b.getLocation();
        Config data = BlockStorage.getLocationInfo(l);
        return generator.getGeneratedOutput(l, data);
    }

    private boolean hasOperation(Block b) {
        return generator.getMachineProcessor().getOperation(b.getLocation()) != null;
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
    @DisplayName("GeneratorFuelBurnEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location l = new Location(world, 1, 60, 1);

        GeneratorFuelBurnEvent event = new GeneratorFuelBurnEvent(generator, l, generator.getFuelTypes().iterator().next(), FUEL_SLOT);

        Assertions.assertEquals(generator, event.getGenerator());
        Assertions.assertEquals(l, event.getLocation());
        Assertions.assertEquals(generator.getFuelTypes().iterator().next(), event.getFuel());
        Assertions.assertEquals(FUEL_SLOT, event.getSlot());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GeneratorFuelBurnEvent(null, l, generator.getFuelTypes().iterator().next(), FUEL_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GeneratorFuelBurnEvent(generator, null, generator.getFuelTypes().iterator().next(), FUEL_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GeneratorFuelBurnEvent(generator, l, null, FUEL_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GeneratorFuelBurnEvent(generator, l, generator.getFuelTypes().iterator().next(), -1));
    }

    @Test
    @DisplayName("Burning fuel fires the event, consumes the fuel and starts the operation")
    void testBurnFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeGenerator(10, 10);
        menu.replaceExistingItem(FUEL_SLOT, new ItemStack(Material.COAL));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(GeneratorFuelBurnEvent event) {
                seen[0] = true;
                Assertions.assertEquals(generator, event.getGenerator());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertEquals(FUEL_SLOT, event.getSlot());
                Assertions.assertTrue(new ItemStack(Material.COAL).isSimilar(event.getFuel().getInput()), "The fuel must be coal");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            int produced = runBurnTick(b);

            Assertions.assertEquals(0, produced, "A burn tick must yield no energy yet");
            Assertions.assertTrue(seen[0], "GeneratorFuelBurnEvent was not fired");
            assertFuelConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The fuel operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling GeneratorFuelBurnEvent keeps the fuel and starts no operation")
    void testCancelKeepsFuelAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeGenerator(20, 20);
        menu.replaceExistingItem(FUEL_SLOT, new ItemStack(Material.COAL));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBurn(GeneratorFuelBurnEvent event) {
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
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeGenerator(30, 30);
        menu.replaceExistingItem(FUEL_SLOT, new ItemStack(Material.COAL));

        runBurnTick(b);

        assertFuelConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The fuel operation must have been started");
    }

    @Test
    @DisplayName("An empty input slot fires no event and starts no operation")
    void testEmptySlotFiresNothing() {
        Block b = world.getBlockAt(40, 60, 40);
        placeGenerator(40, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(GeneratorFuelBurnEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            int produced = runBurnTick(b);

            Assertions.assertEquals(0, produced, "An idle generator must yield no energy");
            Assertions.assertFalse(seen[0], "No event must be fired without fuel");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A non-fuel item in the input slot fires no event and starts no operation")
    void testWrongItemFiresNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeGenerator(50, 50);
        menu.replaceExistingItem(FUEL_SLOT, new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(GeneratorFuelBurnEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            int produced = runBurnTick(b);

            Assertions.assertEquals(0, produced, "An idle generator must yield no energy");
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

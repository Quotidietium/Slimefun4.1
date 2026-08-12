package io.github.thebusybiscuit.slimefun4.implementation.items.geo;

import java.util.OptionalInt;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import io.github.thebusybiscuit.slimefun4.api.events.OilPumpExtractEvent;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the oil pump API expansion: {@link OilPumpExtractEvent},
 * exercised by driving the real {@link OilPump} {@link BlockTicker} against a
 * {@link BlockStorage}-backed pump whose chunk holds oil supplies and whose input
 * slot holds an empty bucket.
 * <p>
 * An extraction is fully observable: the bucket is consumed, the chunk supplies
 * drop by one and a {@code CraftingOperation} appears on the processor. A vetoed
 * extraction keeps all three untouched. When the supplies run dry the pump moves
 * the bucket to its output slots instead of extracting, and no event is fired.
 *
 * @author Zurker
 */
class TestOilPumpExtractEvent {

    private static final int INPUT_SLOT = 19;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static OilPump pump;
    private static GEOResource oil;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        /*
         * Unit test startups never register the built-in GEO resources (onUnitTestStart
         * skips GEOResourcesSetup), so the "slimefun:oil" key the OilPump resolves in
         * its constructor would be empty. Stand in with a test resource under that key,
         * registered before the pump is constructed.
         */
        NamespacedKey oilKey = new NamespacedKey(plugin, "oil");
        oil = new GEOResource() {
            @Override
            public NamespacedKey getKey() {
                return oilKey;
            }

            @Override
            public int getDefaultSupply(World.Environment environment, org.bukkit.block.Biome biome) {
                return 0;
            }

            @Override
            public int getMaxDeviation() {
                return 1;
            }

            @Override
            public String getName() {
                return "Test Oil";
            }

            @Override
            public ItemStack getItem() {
                return new ItemStack(Material.BUCKET);
            }

            @Override
            public boolean isObtainableFromGEOMiner() {
                return false;
            }
        };
        oil.register();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "oil_pump_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_OIL_PUMP", Material.DISPENSER, "&fTest Oil Pump");
        Slimefun.getItemCfg().setValue("_TEST_OIL_PUMP.enabled", true);
        pump = new OilPump(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        pump.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        pump.register(plugin);
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
     * Places the pump as a real block backed by {@link BlockStorage} and returns its menu.
     */
    private BlockMenu placePump(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", pump.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the pump's real {@link BlockTicker}.
     */
    private void tick(Block b) {
        pump.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, pump, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return pump.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    private int oilSupplies(Block b) {
        OptionalInt supplies = Slimefun.getGPSNetwork().getResourceManager().getSupplies(oil, world, b.getX() >> 4, b.getZ() >> 4);
        Assertions.assertTrue(supplies.isPresent(), "The chunk must hold oil supplies");
        return supplies.getAsInt();
    }

    private void setOilSupplies(Block b, int value) {
        Slimefun.getGPSNetwork().getResourceManager().setSupplies(oil, world, b.getX() >> 4, b.getZ() >> 4, value);
    }

    /**
     * {@link BlockMenu#consumeItem(int, int)} leaves a zero-amount stack behind instead of
     * clearing the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertBucketConsumed(BlockMenu menu) {
        ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
        Assertions.assertTrue(slot == null || slot.getAmount() == 0, "The bucket must have been consumed, got: " + slot);
    }

    @Test
    @DisplayName("OilPumpExtractEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);

        OilPumpExtractEvent event = new OilPumpExtractEvent(pump, b.getLocation(), oil, INPUT_SLOT, 3);

        Assertions.assertEquals(pump, event.getPump());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertSame(oil, event.getResource());
        Assertions.assertEquals(INPUT_SLOT, event.getSlot());
        Assertions.assertEquals(3, event.getSupplies());
        Assertions.assertEquals(1, event.getSuppliesCost(), "The supplies cost must default to one unit");
        Assertions.assertFalse(event.isCancelled());

        // The extraction can be throttled or made free within the remaining supplies
        event.setSuppliesCost(0);
        Assertions.assertEquals(0, event.getSuppliesCost());
        event.setSuppliesCost(3);
        Assertions.assertEquals(3, event.getSuppliesCost());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSuppliesCost(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSuppliesCost(4), "The cost must not exceed the remaining supplies");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new OilPumpExtractEvent(null, b.getLocation(), oil, INPUT_SLOT, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new OilPumpExtractEvent(pump, null, oil, INPUT_SLOT, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new OilPumpExtractEvent(pump, b.getLocation(), null, INPUT_SLOT, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new OilPumpExtractEvent(pump, b.getLocation(), oil, -1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new OilPumpExtractEvent(pump, b.getLocation(), oil, INPUT_SLOT, 0));
    }

    @Test
    @DisplayName("An extraction fires the event, consumes the bucket and one supply unit and starts the operation")
    void testExtractFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placePump(10, 10);
        setOilSupplies(b, 3);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BUCKET));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExtract(OilPumpExtractEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pump, event.getPump());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertSame(oil, event.getResource(), "The event must carry the oil resource");
                Assertions.assertEquals(INPUT_SLOT, event.getSlot());
                Assertions.assertEquals(3, event.getSupplies(), "The event must carry the supplies before the extraction");
                Assertions.assertEquals(1, event.getSuppliesCost(), "The supplies cost must default to one unit");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "OilPumpExtractEvent was not fired");
            assertBucketConsumed(menu);
            Assertions.assertEquals(2, oilSupplies(b), "One supply unit must have been consumed");
            Assertions.assertTrue(hasOperation(b), "The pumping operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling OilPumpExtractEvent keeps the bucket, the supplies and starts no operation")
    void testCancelKeepsBucketAndSupplies() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placePump(20, 20);
        setOilSupplies(b, 3);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BUCKET));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onExtract(OilPumpExtractEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "A vetoed extraction must keep the bucket");
            Assertions.assertEquals(1, slot.getAmount(), "A vetoed extraction must keep the bucket untouched");
            Assertions.assertEquals(3, oilSupplies(b), "A vetoed extraction must keep the supplies untouched");
            Assertions.assertFalse(hasOperation(b), "A vetoed extraction must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("setSuppliesCost scales the supplies drained per extraction")
    void testSuppliesCostScalesExtraction() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placePump(60, 60);
        setOilSupplies(b, 5);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BUCKET));

        Listener scaling = new Listener() {
            @EventHandler
            public void onExtract(OilPumpExtractEvent event) {
                event.setSuppliesCost(3);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            tick(b);

            assertBucketConsumed(menu);
            Assertions.assertEquals(2, oilSupplies(b), "Three supply units must have been consumed");
            Assertions.assertTrue(hasOperation(b), "The pumping operation must have been started");
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("A zero supplies cost extracts without draining the chunk")
    void testZeroSuppliesCostExtractsForFree() {
        Block b = world.getBlockAt(70, 60, 70);
        BlockMenu menu = placePump(70, 70);
        setOilSupplies(b, 3);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BUCKET));

        Listener freeExtraction = new Listener() {
            @EventHandler
            public void onExtract(OilPumpExtractEvent event) {
                event.setSuppliesCost(0);
            }
        };
        server.getPluginManager().registerEvents(freeExtraction, plugin);

        try {
            tick(b);

            assertBucketConsumed(menu);
            Assertions.assertEquals(3, oilSupplies(b), "A zero cost must keep the supplies untouched");
            Assertions.assertTrue(hasOperation(b), "The pumping operation must have been started");
        } finally {
            HandlerList.unregisterAll(freeExtraction);
        }
    }

    @Test
    @DisplayName("Extracting without listeners still consumes the bucket, preserving the old behavior")
    void testExtractWithoutListenersExtracts() {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placePump(30, 30);
        setOilSupplies(b, 3);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BUCKET));

        tick(b);

        assertBucketConsumed(menu);
        Assertions.assertEquals(2, oilSupplies(b), "One supply unit must have been consumed");
        Assertions.assertTrue(hasOperation(b), "The pumping operation must have been started");
    }

    @Test
    @DisplayName("A missing bucket fires no event and starts no operation")
    void testNoBucketFiresNothing() {
        Block b = world.getBlockAt(40, 60, 40);
        placePump(40, 40);
        setOilSupplies(b, 3);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExtract(OilPumpExtractEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without a bucket");
            Assertions.assertEquals(3, oilSupplies(b), "The supplies must be untouched");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Dry supplies fire no event and move the bucket to the output slots")
    void testDrySuppliesMoveBucketToOutput() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placePump(50, 50);
        setOilSupplies(b, 0);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BUCKET));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExtract(OilPumpExtractEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without remaining supplies");
            ItemStack input = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertTrue(input == null || input.getAmount() == 0, "The bucket must have left the input slot");

            boolean bucketInOutput = false;
            for (int slot : pump.getOutputSlots()) {
                ItemStack item = menu.getItemInSlot(slot);

                if (item != null && item.getType() == Material.BUCKET) {
                    bucketInOutput = true;
                    break;
                }
            }

            Assertions.assertTrue(bucketInOutput, "The bucket must have been moved to the output slots");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

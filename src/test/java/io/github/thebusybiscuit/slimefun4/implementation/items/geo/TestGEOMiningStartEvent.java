package io.github.thebusybiscuit.slimefun4.implementation.items.geo;

import java.util.OptionalInt;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
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

import io.github.thebusybiscuit.slimefun4.api.events.GEOMiningStartEvent;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.geo.ResourceManager;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the geo miner API expansion: {@link GEOMiningStartEvent},
 * exercised by driving the real {@link GEOMiner} {@link BlockTicker} against a
 * {@link BlockStorage}-backed miner whose chunk holds supplies of a test
 * {@link GEOResource}.
 * <p>
 * A starting tick is fully observable: a {@code GEOMiningOperation} appears on the
 * processor and the chunk supplies drop by one. A vetoed start keeps the supplies
 * and starts no operation. The event must not fire when the chunk was never
 * scanned, when no supplies remain or when the output slots are jammed, because
 * no supplies would be consumed in those cases.
 * <p>
 * Note: the hologram update at the end of a successful start hits MockBukkit's
 * unimplemented {@code setRemoveWhenFarAway}, but the {@code HologramsService}
 * catches and logs that failure, so the tick completes normally.
 *
 * @author Zurker
 */
class TestGEOMiningStartEvent {

    private static final int[] OUTPUT_SLOTS = { 29, 30, 31, 32, 33, 38, 39, 40, 41, 42 };

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static GEOMiner miner;
    private static GEOResource resource;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "geo_mining_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_GEO_MINER", Material.DISPENSER, "&fTest GEO Miner");
        Slimefun.getItemCfg().setValue("_TEST_GEO_MINER.enabled", true);
        miner = new GEOMiner(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        miner.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        miner.register(plugin);

        NamespacedKey resourceKey = new NamespacedKey(plugin, "test_geo_mining_resource");
        resource = new GEOResource() {
            @Override
            public NamespacedKey getKey() {
                return resourceKey;
            }

            @Override
            public int getDefaultSupply(World.Environment environment, Biome biome) {
                return 0;
            }

            @Override
            public int getMaxDeviation() {
                return 1;
            }

            @Override
            public String getName() {
                return "Test Resource";
            }

            @Override
            public ItemStack getItem() {
                return new ItemStack(Material.GOLD_ORE);
            }

            @Override
            public boolean isObtainableFromGEOMiner() {
                return true;
            }
        };
        resource.register();
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
     * Places the miner as a real block backed by {@link BlockStorage} and returns its menu.
     */
    private BlockMenu placeMiner(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", miner.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * The miner returns early as soon as any obtainable resource has no supplies recorded
     * for its chunk, so the chunk must hold a value for every registered resource: the
     * given amount for the test resource and zero for everything else (e.g. oil).
     */
    private void prepareChunk(Block b, int testSupplies) {
        ResourceManager resourceManager = Slimefun.getGPSNetwork().getResourceManager();

        for (GEOResource r : Slimefun.getRegistry().getGEOResources().values()) {
            if (r.isObtainableFromGEOMiner()) {
                resourceManager.setSupplies(r, world, b.getX() >> 4, b.getZ() >> 4, r == resource ? testSupplies : 0);
            }
        }
    }

    /**
     * Runs one tick of the miner's real {@link BlockTicker}.
     */
    private void tick(Block b) {
        miner.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, miner, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return miner.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    private int suppliesOf(Block b) {
        OptionalInt supplies = Slimefun.getGPSNetwork().getResourceManager().getSupplies(resource, world, b.getX() >> 4, b.getZ() >> 4);
        Assertions.assertTrue(supplies.isPresent(), "The chunk must hold supplies of the test resource");
        return supplies.getAsInt();
    }

    @Test
    @DisplayName("GEOMiningStartEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);

        GEOMiningStartEvent event = new GEOMiningStartEvent(miner, b.getLocation(), resource, 5);

        Assertions.assertEquals(miner, event.getMiner());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertSame(resource, event.getResource());
        Assertions.assertEquals(5, event.getSupplies());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOMiningStartEvent(null, b.getLocation(), resource, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOMiningStartEvent(miner, null, resource, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOMiningStartEvent(miner, b.getLocation(), null, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOMiningStartEvent(miner, b.getLocation(), resource, 0));
    }

    @Test
    @DisplayName("A starting tick fires the event, starts the operation and consumes one supply unit")
    void testMiningStartFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        placeMiner(10, 10);
        prepareChunk(b, 5);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMiningStart(GEOMiningStartEvent event) {
                seen[0] = true;
                Assertions.assertEquals(miner, event.getMiner());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertSame(resource, event.getResource(), "The event must carry the resource being mined");
                Assertions.assertEquals(5, event.getSupplies(), "The event must carry the supplies before the extraction");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "GEOMiningStartEvent was not fired");
            Assertions.assertTrue(hasOperation(b), "The mining operation must have been started");
            Assertions.assertEquals(4, suppliesOf(b), "One supply unit must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling GEOMiningStartEvent keeps the supplies and starts no operation")
    void testCancelKeepsSuppliesAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        placeMiner(20, 20);
        prepareChunk(b, 5);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onMiningStart(GEOMiningStartEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            Assertions.assertFalse(hasOperation(b), "A vetoed start must not start an operation");
            Assertions.assertEquals(5, suppliesOf(b), "A vetoed start must keep the supplies untouched");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A starting tick without listeners still consumes a supply unit, preserving the old behavior")
    void testMiningStartWithoutListenersStarts() {
        Block b = world.getBlockAt(30, 60, 30);
        placeMiner(30, 30);
        prepareChunk(b, 5);

        tick(b);

        Assertions.assertTrue(hasOperation(b), "The mining operation must have been started");
        Assertions.assertEquals(4, suppliesOf(b), "One supply unit must have been consumed");
    }

    @Test
    @DisplayName("An unscanned chunk fires no event and starts no operation")
    void testUnscannedChunkFiresNothing() {
        Block b = world.getBlockAt(40, 60, 40);
        placeMiner(40, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMiningStart(GEOMiningStartEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without a scanned chunk");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Exhausted supplies fire no event and start no operation")
    void testZeroSuppliesFireNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        placeMiner(50, 50);
        prepareChunk(b, 0);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMiningStart(GEOMiningStartEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without remaining supplies");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A jammed output fires no event, keeps the supplies and starts no operation")
    void testJammedOutputFiresNothing() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeMiner(60, 60);
        prepareChunk(b, 5);

        for (int slot : OUTPUT_SLOTS) {
            menu.replaceExistingItem(slot, new ItemStack(Material.GOLD_ORE, 64));
        }

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMiningStart(GEOMiningStartEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired when the output cannot hold the result");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
            Assertions.assertEquals(5, suppliesOf(b), "A jammed miner must keep the supplies untouched");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Discriminant tests for the resolved-dispatch structure of the
 * {@link TickerTask}: the item/ticker resolution rides the tick registry and
 * must refresh on every first-party data transition (re-place, re-store,
 * destroy-false deletion) while the block {@link Config} stays LIVE on every
 * tick - machines mutate their data through it, so a stale reference would
 * silently divert those writes into a dead object.
 *
 * @author Zurker
 */
class TestTickerResolutionCache {

    private static final String ITEM_A = "_TEST_TICKER_A";
    private static final String ITEM_B = "_TEST_TICKER_B";

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static final AtomicInteger ticksA = new AtomicInteger();
    private static final AtomicInteger ticksB = new AtomicInteger();

    /** Every B tick records the live "flag" value it saw. */
    private static final List<String> flagsSeenByB = new ArrayList<>();

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "ticker_resolution_test");

        SlimefunItem itemA = new SlimefunItem(itemGroup, new SlimefunItemStack(ITEM_A, Material.IRON_BLOCK, "Ticker A"), RecipeType.NULL, new ItemStack[9]);
        itemA.addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                ticksA.incrementAndGet();
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });
        itemA.register(plugin);

        SlimefunItem itemB = new SlimefunItem(itemGroup, new SlimefunItemStack(ITEM_B, Material.GOLD_BLOCK, "Ticker B"), RecipeType.NULL, new ItemStack[9]);
        itemB.addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                ticksB.incrementAndGet();
                flagsSeenByB.add(data.getString("flag"));
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });
        itemB.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        ticksA.set(0);
        ticksB.set(0);
        flagsSeenByB.clear();
    }

    private Location place(int x, int z, String id) {
        Location l = new Location(world, x, 64, z);
        // Force chunk creation so tickChunk() sees a loaded chunk
        world.getChunkAt(l);
        world.getBlockAt(l).setType(Material.IRON_BLOCK);
        BlockStorage.addBlockInfo(l, "id", id, true);
        return l;
    }

    private void runOnce() {
        Slimefun.getTickerTask().run();
    }

    @Test
    @DisplayName("A ticking block is dispatched exactly once per run")
    void testDispatch() {
        Location l = place(10, 10, ITEM_A);
        runOnce();

        Assertions.assertEquals(1, ticksA.get(), "The ticking block must be dispatched exactly once");
        Assertions.assertEquals(0, ticksB.get(), "The other item must not be dispatched");

        Slimefun.getTickerTask().disableTicker(l);
    }

    @Test
    @DisplayName("The block Config is live on every tick: writes between runs are visible")
    void testDataStaysLive() {
        Location l = place(20, 20, ITEM_B);
        BlockStorage.addBlockInfo(l, "flag", "one", false);

        runOnce();
        BlockStorage.addBlockInfo(l, "flag", "two", false);
        runOnce();
        BlockStorage.updateBlockInfo(l, BlockStorage.getLocationInfo(l), "flag", "three");
        runOnce();

        Assertions.assertEquals(List.of("one", "two", "three"), flagsSeenByB, "Every tick must see the current data value");

        Slimefun.getTickerTask().disableTicker(l);
    }

    @Test
    @DisplayName("A re-placed block (disable -> store -> enable) dispatches the new item")
    void testRePlaceRefreshesResolution() {
        Location l = place(30, 30, ITEM_A);

        Slimefun.getTickerTask().disableTicker(l);
        place(30, 30, ITEM_B);

        runOnce();

        Assertions.assertEquals(0, ticksA.get(), "The departed item must no longer dispatch");
        Assertions.assertEquals(1, ticksB.get(), "The replacement item must dispatch");

        Slimefun.getTickerTask().disableTicker(l);
    }

    @Test
    @DisplayName("A destroy=false deletion re-resolves: nothing ticks without data, the replacement ticks")
    void testDestroyFalseDeletionResolvesReplacement() {
        Location l = place(40, 40, ITEM_A);

        // Deletes the data but leaves the ticker registered
        Slimefun.getTickerTask().queueDelete(l, false);
        runOnce();

        Assertions.assertEquals(0, ticksA.get(), "No data: nothing may dispatch (old early-return semantics)");

        // New data arrives for the same, still-registered ticker location
        BlockStorage.addBlockInfo(l, "id", ITEM_B, false);
        runOnce();

        Assertions.assertEquals(1, ticksB.get(), "The replacement data must be re-resolved and dispatched");

        Slimefun.getTickerTask().disableTicker(l);
    }

    @Test
    @DisplayName("An enabled ticker without data ticks nothing until data appears (lazy retry)")
    void testEnableBeforeData() {
        Location l = new Location(world, 50, 64, 50);
        world.getChunkAt(l);
        world.getBlockAt(l).setType(Material.IRON_BLOCK);

        // Enable without any stored data - not a first-party ordering, but the
        // defensive lazy path must behave exactly like the old per-tick lookup
        Slimefun.getTickerTask().enableTicker(l);
        runOnce();

        Assertions.assertEquals(0, ticksA.get(), "No data: nothing may dispatch");

        BlockStorage.addBlockInfo(l, "id", ITEM_A, false);
        runOnce();

        Assertions.assertEquals(1, ticksA.get(), "Once data exists the next run resolves and dispatches");

        Slimefun.getTickerTask().disableTicker(l);
    }

    @Test
    @DisplayName("getLocations() keeps exposing the chunk -> locations shape")
    void testPublicLocationsShape() {
        Location l = place(60, 60, ITEM_A);

        Map<io.github.bakedlibs.dough.blocks.ChunkPosition, Set<Location>> locations = Slimefun.getTickerTask().getLocations();
        boolean found = locations.values().stream().anyMatch(set -> set.contains(l));
        Assertions.assertTrue(found, "The enabled location must appear in getLocations()");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> locations.values().iterator().next().add(new Location(world, 1, 1, 1)), "The exposed sets must stay read-only");

        Slimefun.getTickerTask().disableTicker(l);
    }
}

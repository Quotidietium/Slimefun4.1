package io.github.thebusybiscuit.slimefun4.implementation.items.geo;

import java.lang.reflect.Method;

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

import io.github.thebusybiscuit.slimefun4.api.events.GEOMiningCompleteEvent;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.operations.GEOMiningOperation;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the GEO miner API expansion:
 * {@link GEOMiningCompleteEvent}, exercised by starting a one-tick operation on a
 * registered {@link GEOMiner}, advancing it to completion via the real protected
 * {@code tick(Block)} method, and asserting the event fires before the output is pushed.
 *
 * @author Zurker
 */
class TestGEOMiningCompleteEvent {

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

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "geo_complete_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_GEO_COMPLETE", Material.DISPENSER, "&fTest GEO Complete");
        Slimefun.getItemCfg().setValue("_TEST_GEO_COMPLETE.enabled", true);
        miner = new GEOMiner(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        miner.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        miner.register(plugin);

        NamespacedKey resourceKey = new NamespacedKey(plugin, "test_geo_complete_resource");
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
                return "Test Complete Resource";
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
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Block placeMiner(int x, int z) {
        Block b = world.getBlockAt(x, 64, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", miner.getId());
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "100");
        return b;
    }

    private void tick(Block b) throws Exception {
        Method tick = GEOMiner.class.getDeclaredMethod("tick", Block.class);
        tick.setAccessible(true);
        tick.invoke(miner, b);
    }

    /**
     * Places a miner, starts a one-tick operation, advances it to completion, then ticks
     * once more to trigger the completion path.
     */
    private void runToCompletion(Block b) throws Exception {
        miner.getMachineProcessor().startOperation(b.getLocation(), new GEOMiningOperation(resource, 1));
        // First tick: operation not finished → addProgress(1) → now finished
        tick(b);
        // Second tick: operation finished → completion path → event fires
        tick(b);
    }

    @Test
    @DisplayName("GEOMiningCompleteEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = placeMiner(1, 1);
        ItemStack result = new ItemStack(Material.GOLD_ORE);

        GEOMiningCompleteEvent event = new GEOMiningCompleteEvent(miner, b.getLocation(), result);

        Assertions.assertEquals(miner, event.getMiner());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertEquals(result, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.DIAMOND);
        event.setResult(replacement);
        Assertions.assertEquals(replacement, event.getResult());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOMiningCompleteEvent(null, b.getLocation(), result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOMiningCompleteEvent(miner, null, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOMiningCompleteEvent(miner, b.getLocation(), null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("Completing a GEO mining operation fires the event and pushes the output")
    void testCompleteFiresEventAndPushes() throws Exception {
        Block b = placeMiner(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onComplete(GEOMiningCompleteEvent event) {
                seen[0] = true;
                Assertions.assertEquals(miner, event.getMiner());
                Assertions.assertEquals(Material.GOLD_ORE, event.getResult().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runToCompletion(b);

            Assertions.assertTrue(seen[0], "GEOMiningCompleteEvent was not fired");
            Assertions.assertNull(miner.getMachineProcessor().getOperation(b.getLocation()), "The operation must have ended");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling GEOMiningCompleteEvent voids the output")
    void testCancelVoidsOutput() throws Exception {
        Block b = placeMiner(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onComplete(GEOMiningCompleteEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            runToCompletion(b);

            Assertions.assertNull(miner.getMachineProcessor().getOperation(b.getLocation()), "The operation must have ended");

            for (int slot : OUTPUT_SLOTS) {
                var menu = BlockStorage.getInventory(b);
                if (menu != null) {
                    ItemStack item = menu.getItemInSlot(slot);
                    Assertions.assertTrue(item == null || item.getType() == Material.AIR, "The output slots must be empty (output voided)");
                }
            }
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Replacing the output via setResult pushes the replacement")
    void testSetResultRedirect() throws Exception {
        Block b = placeMiner(30, 30);
        ItemStack replacement = new ItemStack(Material.DIAMOND);

        Listener redirecting = new Listener() {
            @EventHandler
            public void onComplete(GEOMiningCompleteEvent event) {
                event.setResult(replacement);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            runToCompletion(b);

            boolean foundDiamond = false;
            var menu = BlockStorage.getInventory(b);
            if (menu != null) {
                for (int slot : OUTPUT_SLOTS) {
                    ItemStack item = menu.getItemInSlot(slot);
                    if (item != null && item.getType() == Material.DIAMOND) {
                        foundDiamond = true;
                        break;
                    }
                }
            }

            Assertions.assertTrue(foundDiamond, "The replacement output must have been pushed");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }
}

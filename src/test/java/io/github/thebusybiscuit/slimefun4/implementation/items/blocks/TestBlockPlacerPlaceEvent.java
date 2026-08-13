package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.BlockPlacerPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockDispenseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the block placer API expansion:
 * {@link BlockPlacerPlaceEvent#setItemStack(ItemStack)} is now honored by the real
 * {@link BlockPlacer} placement paths, exercised by driving the {@link BlockDispenseHandler}
 * with a real dispenser block state and letting the scheduled sync placement run.
 * <p>
 * The replacement item is what gets placed; the originally dispensed item is what gets
 * consumed from the dispenser's inventory.
 *
 * @author Zurker
 */
class TestBlockPlacerPlaceEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static BlockPlacer blockPlacer;
    private static SlimefunItemStack sfBlockStack;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "block_placer_test");

        SlimefunItemStack placerStack = new SlimefunItemStack("_TEST_BLOCK_PLACER", Material.DISPENSER, "&fTest Block Placer");
        Slimefun.getItemCfg().setValue("_TEST_BLOCK_PLACER.enabled", true);
        blockPlacer = new BlockPlacer(itemGroup, placerStack, RecipeType.NULL, new ItemStack[9]);
        blockPlacer.register(plugin);

        // A placeable Slimefun block for the Slimefun placement path
        sfBlockStack = new SlimefunItemStack("_TEST_PLACER_SF_BLOCK", Material.BRICKS, "&fTest Placer Block");
        Slimefun.getItemCfg().setValue("_TEST_PLACER_SF_BLOCK.enabled", true);
        new SlimefunItem(itemGroup, sfBlockStack, RecipeType.NULL, new ItemStack[9]).register(plugin);
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
     * Places a dispenser with owner info at the given coordinates, so the permission
     * check passes, and returns it together with the faced (empty) block to the north.
     */
    private Block placePlacer(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", "_TEST_BLOCK_PLACER");
        BlockStorage.addBlockInfo(b, "owner", "00000000-0000-0000-0000-000000000042");
        return b;
    }

    /**
     * Dispenses the given item through the placer's real {@link BlockDispenseHandler} and
     * runs the scheduler until the delayed placement has executed.
     */
    private void dispense(Block dispenserBlock, ItemStack dispensed) {
        Dispenser state = (Dispenser) dispenserBlock.getState();
        Block faced = dispenserBlock.getRelative(BlockFace.NORTH);

        BlockDispenseEvent e = new BlockDispenseEvent(dispenserBlock, dispensed, new Vector(0, 0, 0));
        blockPlacer.callItemHandler(BlockDispenseHandler.class, handler -> handler.onBlockDispense(e, state, faced, blockPlacer));

        // The placement is delayed by 2 ticks (dispenser-inventory synchronization)
        server.getScheduler().performTicks(3);
    }

    @Test
    @DisplayName("BlockPlacerPlaceEvent exposes its fields, replacement and immutability")
    void testEventFieldsAndValidation() {
        Block placer = placePlacer(1, 1);
        Block faced = placer.getRelative(BlockFace.NORTH);

        BlockPlacerPlaceEvent event = new BlockPlacerPlaceEvent(placer, new ItemStack(Material.STONE), faced);

        Assertions.assertEquals(placer, event.getBlockPlacer());
        Assertions.assertEquals(new ItemStack(Material.STONE), event.getItemStack());
        Assertions.assertEquals(faced, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        // The placed item can be replaced
        ItemStack replacement = new ItemStack(Material.DIAMOND_BLOCK);
        event.setItemStack(replacement);
        Assertions.assertEquals(replacement, event.getItemStack());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        // Once locked, the event can no longer be modified
        BlockPlacerPlaceEvent locked = new BlockPlacerPlaceEvent(placer, sfBlockStack.item(), faced);
        locked.setImmutable();
        locked.setItemStack(replacement);
        locked.setCancelled(true);
        Assertions.assertEquals(sfBlockStack.item(), locked.getItemStack(), "A locked event must keep its placed item");
        Assertions.assertFalse(locked.isCancelled(), "A locked event must not be cancellable");

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setItemStack(null));
    }

    @Test
    @DisplayName("Without listeners the placer places and consumes the dispensed item")
    void testVanillaPlaceDefault() {
        Block placer = placePlacer(100, 100);
        Block faced = placer.getRelative(BlockFace.NORTH);
        Dispenser state = (Dispenser) placer.getState();
        state.getInventory().addItem(new ItemStack(Material.STONE, 3));

        dispense(placer, new ItemStack(Material.STONE));

        Assertions.assertEquals(Material.STONE, faced.getType(), "The dispensed block must have been placed");

        ItemStack remaining = state.getInventory().getItem(0);
        Assertions.assertNotNull(remaining, "The remaining items must stay in the dispenser");
        Assertions.assertEquals(2, remaining.getAmount(), "Exactly one item must have been consumed");
    }

    @Test
    @DisplayName("setItemStack replaces the placed block but consumes the dispensed item")
    void testVanillaPlaceRedirected() {
        Block placer = placePlacer(200, 200);
        Block faced = placer.getRelative(BlockFace.NORTH);
        Dispenser state = (Dispenser) placer.getState();
        state.getInventory().addItem(new ItemStack(Material.STONE, 3));

        Listener replacing = new Listener() {
            @EventHandler
            public void onPlace(BlockPlacerPlaceEvent event) {
                Assertions.assertEquals(new ItemStack(Material.STONE), event.getItemStack(), "The event must carry the dispensed item");
                event.setItemStack(new ItemStack(Material.DIAMOND_BLOCK));
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            dispense(placer, new ItemStack(Material.STONE));

            Assertions.assertEquals(Material.DIAMOND_BLOCK, faced.getType(), "The replacement block must have been placed");

            ItemStack remaining = state.getInventory().getItem(0);
            Assertions.assertNotNull(remaining, "The remaining items must stay in the dispenser");
            Assertions.assertEquals(Material.STONE, remaining.getType(), "The dispensed item must have been consumed");
            Assertions.assertEquals(2, remaining.getAmount(), "Exactly one item must have been consumed");
        } finally {
            HandlerList.unregisterAll(replacing);
        }
    }

    @Test
    @DisplayName("Cancelling BlockPlacerPlaceEvent places nothing and consumes nothing")
    void testVanillaPlaceCancelled() {
        Block placer = placePlacer(300, 300);
        Block faced = placer.getRelative(BlockFace.NORTH);
        Dispenser state = (Dispenser) placer.getState();
        state.getInventory().addItem(new ItemStack(Material.STONE, 3));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPlace(BlockPlacerPlaceEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            dispense(placer, new ItemStack(Material.STONE));

            Assertions.assertEquals(Material.AIR, faced.getType(), "A cancelled placement must place nothing");

            ItemStack remaining = state.getInventory().getItem(0);
            Assertions.assertNotNull(remaining, "The items must stay in the dispenser");
            Assertions.assertEquals(3, remaining.getAmount(), "Nothing must have been consumed");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A replacement that fails the placement rules is rejected: nothing placed, nothing consumed")
    void testVanillaPlaceRejectsInvalidReplacement() {
        Block placer = placePlacer(600, 600);
        Block faced = placer.getRelative(BlockFace.NORTH);
        Dispenser state = (Dispenser) placer.getState();
        state.getInventory().addItem(new ItemStack(Material.STONE, 3));

        Listener replacing = new Listener() {
            @EventHandler
            public void onPlace(BlockPlacerPlaceEvent event) {
                // A stick is not a block: the replacement must be re-validated against
                // the same isAllowed(...) rules the original item passed.
                event.setItemStack(new ItemStack(Material.STICK));
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            dispense(placer, new ItemStack(Material.STONE));

            Assertions.assertEquals(Material.AIR, faced.getType(), "An invalid replacement must place nothing");

            ItemStack remaining = state.getInventory().getItem(0);
            Assertions.assertNotNull(remaining, "The items must stay in the dispenser");
            Assertions.assertEquals(3, remaining.getAmount(), "Nothing must have been consumed");
        } finally {
            HandlerList.unregisterAll(replacing);
        }
    }

    @Test
    @DisplayName("A Slimefun block is placed with its Slimefun identity")
    void testSlimefunPlaceDefault() {
        Block placer = placePlacer(400, 400);
        Block faced = placer.getRelative(BlockFace.NORTH);
        Dispenser state = (Dispenser) placer.getState();
        state.getInventory().addItem(sfBlockStack.item());

        dispense(placer, sfBlockStack.item());

        Assertions.assertEquals(Material.BRICKS, faced.getType(), "The Slimefun block's material must have been placed");
        Assertions.assertEquals("_TEST_PLACER_SF_BLOCK", BlockStorage.getLocationInfo(faced.getLocation(), "id"), "The placed block must carry the Slimefun id");
    }

    @Test
    @DisplayName("Redirecting a Slimefun placement keeps the original Slimefun identity")
    void testSlimefunPlaceRedirected() {
        Block placer = placePlacer(500, 500);
        Block faced = placer.getRelative(BlockFace.NORTH);
        Dispenser state = (Dispenser) placer.getState();
        state.getInventory().addItem(sfBlockStack.item());

        Listener replacing = new Listener() {
            @EventHandler
            public void onPlace(BlockPlacerPlaceEvent event) {
                event.setItemStack(new ItemStack(Material.DIAMOND_BLOCK));
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            dispense(placer, sfBlockStack.item());

            Assertions.assertEquals(Material.DIAMOND_BLOCK, faced.getType(), "The replacement material must have been placed");
            Assertions.assertEquals("_TEST_PLACER_SF_BLOCK", BlockStorage.getLocationInfo(faced.getLocation(), "id"), "The placed block must keep the original Slimefun id");
        } finally {
            HandlerList.unregisterAll(replacing);
        }
    }
}

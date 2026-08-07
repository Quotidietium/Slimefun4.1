package io.github.thebusybiscuit.slimefun4.core.handlers;

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

import io.github.thebusybiscuit.slimefun4.api.events.RainbowBlockCycleEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.RainbowBlock;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the rainbow block API expansion:
 * {@link RainbowBlockCycleEvent}, exercised by driving a real {@link RainbowTickHandler}
 * against {@link BlockStorage}-backed wool blocks.
 * <p>
 * A tick recolors the block, so tests assert the outcome end-to-end through the block's
 * type: a cancelled event keeps the current color. Tests that need a controlled cycle
 * position use a fresh {@link RainbowTickHandler}, because the color sequence is global
 * to the handler and {@code uniqueTick()} never runs under MockBukkit.
 *
 * @author Zurker
 */
class TestRainbowBlockCycleEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static RainbowBlock rainbowBlock;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "rainbow_block_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_RAINBOW_BLOCK", Material.WHITE_WOOL, "&7Test Rainbow Block");
        Slimefun.getItemCfg().setValue("_TEST_RAINBOW_BLOCK.enabled", true);
        rainbowBlock = new RainbowBlock(itemGroup, stack, RecipeType.NULL, new ItemStack[9], null, newTicker());
        rainbowBlock.register(plugin);
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
     * A fresh two-color ticker, starting at {@link Material#RED_WOOL}.
     */
    private static RainbowTickHandler newTicker() {
        return new RainbowTickHandler(Material.RED_WOOL, Material.BLUE_WOOL);
    }

    /**
     * Places a white wool rainbow block at a high, flat y to stay clear of the terrain.
     */
    private Block placeRainbowBlock(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.WHITE_WOOL);
        BlockStorage.addBlockInfo(b, "id", "_TEST_RAINBOW_BLOCK");
        return b;
    }

    /**
     * Runs one tick of a fresh {@link RainbowTickHandler} against the given block.
     */
    private void tick(RainbowTickHandler ticker, Block b) {
        ticker.tick(b, rainbowBlock, BlockStorage.getLocationInfo(b.getLocation()));
    }

    @Test
    @DisplayName("RainbowBlockCycleEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);

        RainbowBlockCycleEvent event = new RainbowBlockCycleEvent(rainbowBlock, b, Material.WHITE_WOOL, Material.RED_WOOL);

        Assertions.assertEquals(rainbowBlock, event.getItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(Material.WHITE_WOOL, event.getPreviousMaterial());
        Assertions.assertEquals(Material.RED_WOOL, event.getNextMaterial());
        Assertions.assertFalse(event.isCancelled());

        event.setNextMaterial(Material.GREEN_WOOL);
        Assertions.assertEquals(Material.GREEN_WOOL, event.getNextMaterial());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowBlockCycleEvent(null, b, Material.WHITE_WOOL, Material.RED_WOOL));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowBlockCycleEvent(rainbowBlock, null, Material.WHITE_WOOL, Material.RED_WOOL));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowBlockCycleEvent(rainbowBlock, b, null, Material.RED_WOOL));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowBlockCycleEvent(rainbowBlock, b, Material.WHITE_WOOL, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNextMaterial(null));
    }

    @Test
    @DisplayName("A tick fires the event through the registered BlockTicker and recolors the block")
    void testCycleFiresEventAndRecolors() {
        Block b = placeRainbowBlock(100, 100);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCycle(RainbowBlockCycleEvent event) {
                seen[0] = true;
                Assertions.assertEquals(rainbowBlock, event.getItem());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(Material.WHITE_WOOL, event.getPreviousMaterial());
                Assertions.assertEquals(Material.RED_WOOL, event.getNextMaterial());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            rainbowBlock.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, rainbowBlock, BlockStorage.getLocationInfo(b.getLocation())));

            Assertions.assertTrue(seen[0], "RainbowBlockCycleEvent was not fired");
            Assertions.assertEquals(Material.RED_WOOL, b.getType(), "The block must have changed to the next color");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling RainbowBlockCycleEvent keeps the current color")
    void testCancelKeepsColor() {
        Block b = placeRainbowBlock(200, 200);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCycle(RainbowBlockCycleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(newTicker(), b);

            Assertions.assertEquals(Material.WHITE_WOOL, b.getType(), "A vetoed cycle must keep the current color");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A tick without listeners still recolors the block, preserving the old behavior")
    void testCycleWithoutListenersRecolors() {
        Block b = placeRainbowBlock(300, 300);

        tick(newTicker(), b);

        Assertions.assertEquals(Material.RED_WOOL, b.getType(), "The block must have changed to the next color");
    }

    @Test
    @DisplayName("Overriding the next material via setNextMaterial applies the override")
    void testOverrideAppliesOverriddenMaterial() {
        Block b = placeRainbowBlock(400, 400);

        Listener overriding = new Listener() {
            @EventHandler
            public void onCycle(RainbowBlockCycleEvent event) {
                event.setNextMaterial(Material.GREEN_WOOL);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            tick(newTicker(), b);

            Assertions.assertEquals(Material.GREEN_WOOL, b.getType(), "The overridden color must have been applied");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("A vetoed block rejoins the color sequence at its next position")
    void testVetoedBlockRejoinsSequence() {
        Block b = placeRainbowBlock(500, 500);
        RainbowTickHandler ticker = newTicker();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCycle(RainbowBlockCycleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(ticker, b);
            Assertions.assertEquals(Material.WHITE_WOOL, b.getType(), "A vetoed cycle must keep the current color");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }

        ticker.uniqueTick();
        tick(ticker, b);

        Assertions.assertEquals(Material.BLUE_WOOL, b.getType(), "The block must rejoin the sequence at its next position");
    }

    @Test
    @DisplayName("An air block fires no event and stays air")
    void testAirBlockFiresNoEvent() {
        Block b = world.getBlockAt(600, 60, 600);
        b.setType(Material.AIR);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCycle(RainbowBlockCycleEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(newTicker(), b);

            Assertions.assertFalse(seen[0], "No event must be fired for a broken (air) block");
            Assertions.assertEquals(Material.AIR, b.getType(), "The air block must not have been recolored");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

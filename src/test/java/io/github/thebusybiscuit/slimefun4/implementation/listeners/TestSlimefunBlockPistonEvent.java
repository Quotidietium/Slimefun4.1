package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockPistonEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the piston protection API expansion:
 * {@link SlimefunBlockPistonEvent}, exercised through the real {@link BlockPhysicsListener}
 * piston extend/retract protection paths.
 *
 * @author Zurker
 */
class TestSlimefunBlockPistonEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static MockSlimefunItem sfItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new BlockPhysicsListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "piston_test");
        Slimefun.getItemCfg().setValue("TEST_PISTON_BLOCK.enabled", true);
        sfItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.DISPENSER), "TEST_PISTON_BLOCK");
        sfItem.register(plugin);
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
     * Places the test item's block backed by {@link BlockStorage} and returns it.
     */
    private Block placeSlimefunBlock(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", sfItem.getId(), true);
        return b;
    }

    private Block piston(int x, int z) {
        Block b = world.getBlockAt(x, 5, z);
        b.setType(Material.PISTON);
        return b;
    }

    @Test
    @DisplayName("SlimefunBlockPistonEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block pistonBlock = world.getBlockAt(1, 1, 1);
        Block protectedBlock = world.getBlockAt(2, 1, 1);

        SlimefunBlockPistonEvent event = new SlimefunBlockPistonEvent(pistonBlock, BlockFace.NORTH, protectedBlock, false);

        Assertions.assertEquals(pistonBlock, event.getPiston());
        Assertions.assertEquals(BlockFace.NORTH, event.getDirection());
        Assertions.assertEquals(protectedBlock, event.getProtectedBlock());
        Assertions.assertFalse(event.isRetract());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockPistonEvent(null, BlockFace.NORTH, protectedBlock, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockPistonEvent(pistonBlock, null, protectedBlock, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockPistonEvent(pistonBlock, BlockFace.NORTH, null, false));
    }

    @Test
    @DisplayName("A piston extending into a Slimefun block fires the event and is cancelled")
    void testExtendFiresAndCancels() {
        Block pistonBlock = piston(10, 10);
        Block sfBlock = placeSlimefunBlock(11, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPiston(SlimefunBlockPistonEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pistonBlock, event.getPiston());
                Assertions.assertEquals(sfBlock, event.getProtectedBlock());
                Assertions.assertFalse(event.isRetract());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockPistonExtendEvent event = new BlockPistonExtendEvent(pistonBlock, new ArrayList<>(List.of(sfBlock)), BlockFace.EAST);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0], "SlimefunBlockPistonEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The piston extend must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A sticky piston retracting a Slimefun block fires the event and is cancelled")
    void testRetractFiresAndCancels() {
        Block pistonBlock = piston(20, 20);
        pistonBlock.setType(Material.STICKY_PISTON);
        Block sfBlock = placeSlimefunBlock(21, 20);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPiston(SlimefunBlockPistonEvent event) {
                seen[0] = true;
                Assertions.assertTrue(event.isRetract());
                Assertions.assertEquals(sfBlock, event.getProtectedBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockPistonRetractEvent event = new BlockPistonRetractEvent(pistonBlock, new ArrayList<>(List.of(sfBlock)), BlockFace.WEST);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0], "SlimefunBlockPistonEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The piston retract must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBlockPistonEvent allows the piston to move")
    void testEventCancellationAllowsMove() {
        Block pistonBlock = piston(30, 30);
        Block sfBlock = placeSlimefunBlock(31, 30);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPiston(SlimefunBlockPistonEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            BlockPistonExtendEvent event = new BlockPistonExtendEvent(pistonBlock, new ArrayList<>(List.of(sfBlock)), BlockFace.EAST);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(event.isCancelled(), "A vetoed protection must allow the piston move");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protection without listeners still cancels, preserving the old behavior")
    void testProtectionWithoutListenersStillCancels() {
        Block pistonBlock = piston(40, 40);
        Block sfBlock = placeSlimefunBlock(41, 40);

        BlockPistonExtendEvent event = new BlockPistonExtendEvent(pistonBlock, new ArrayList<>(List.of(sfBlock)), BlockFace.EAST);
        server.getPluginManager().callEvent(event);

        Assertions.assertTrue(event.isCancelled(), "The piston extend must have been cancelled");
    }

    @Test
    @DisplayName("A piston moving only vanilla blocks fires no event")
    void testVanillaBlocksFireNothing() {
        Block pistonBlock = piston(50, 50);
        Block vanilla = world.getBlockAt(51, 5, 50);
        vanilla.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPiston(SlimefunBlockPistonEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockPistonExtendEvent event = new BlockPistonExtendEvent(pistonBlock, new ArrayList<>(List.of(vanilla)), BlockFace.EAST);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for vanilla blocks");
            Assertions.assertFalse(event.isCancelled(), "A vanilla piston move must be left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.bukkit.util.Vector;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockDispenseEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockInteractEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockDispenseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the block behavior API expansion: {@link SlimefunBlockInteractEvent}
 * and {@link SlimefunBlockDispenseEvent}, exercised through the real {@link SlimefunItemInteractListener}
 * and {@link DispenserListener} dispatch paths.
 *
 * @author Zurker
 */
class TestBlockBehaviorEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SlimefunItem blockItem;
    private static SlimefunItem dispenserItem;

    private static final AtomicBoolean useHandlerCalled = new AtomicBoolean(false);
    private static final AtomicBoolean dispenseHandlerCalled = new AtomicBoolean(false);

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register them manually
        new BlockListener(plugin);
        new SlimefunItemInteractListener(plugin);
        new DispenserListener(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "block_behavior_test");

        SlimefunItemStack blockStack = new SlimefunItemStack("TEST_BLOCK_USE_STONE", Material.STONE, "&7Test Block");
        blockItem = new SimpleSlimefunItem<BlockUseHandler>(itemGroup, blockStack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public BlockUseHandler getItemHandler() {
                return e -> useHandlerCalled.set(true);
            }
        };
        blockItem.register(plugin);

        SlimefunItemStack dispenserStack = new SlimefunItemStack("TEST_DISPENSE_MACHINE", Material.DISPENSER, "&7Test Dispenser Machine");
        dispenserItem = new SimpleSlimefunItem<BlockDispenseHandler>(itemGroup, dispenserStack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public BlockDispenseHandler getItemHandler() {
                return (e, dispenser, facedBlock, machine) -> dispenseHandlerCalled.set(true);
            }
        };
        dispenserItem.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
        useHandlerCalled.set(false);
        dispenseHandlerCalled.set(false);
    }

    private PlayerInteractEvent newInteractEvent(Player player, Block clickedBlock) {
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);
    }

    // ---------- SlimefunBlockInteractEvent ----------

    @Test
    @DisplayName("SlimefunBlockInteractEvent exposes its fields and validates constructor arguments")
    void testInteractEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Block block = world.getBlockAt(1, 64, 1);
        PlayerRightClickEvent rightClickEvent = new PlayerRightClickEvent(newInteractEvent(player, block));

        SlimefunBlockInteractEvent event = new SlimefunBlockInteractEvent(player, blockItem, block, rightClickEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(blockItem, event.getSlimefunItem());
        Assertions.assertEquals(block, event.getClickedBlock());
        Assertions.assertEquals(rightClickEvent, event.getRightClickEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockInteractEvent(player, null, block, rightClickEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockInteractEvent(player, blockItem, null, rightClickEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockInteractEvent(player, blockItem, block, null));
    }

    @Test
    @DisplayName("Right-clicking a Slimefun block fires SlimefunBlockInteractEvent and runs the BlockUseHandler")
    void testInteractFiresAndRunsHandler() {
        PlayerMock player = server.addPlayer();
        Block block = TestUtilities.placeSlimefunBlock(server, blockItem.getItem(), world, player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onInteract(SlimefunBlockInteractEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(blockItem, event.getSlimefunItem());
                Assertions.assertEquals(block, event.getClickedBlock());
                Assertions.assertNotNull(event.getRightClickEvent());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = newInteractEvent(player, block);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertTrue(seen[0], "SlimefunBlockInteractEvent was not fired");
            Assertions.assertTrue(useHandlerCalled.get(), "The BlockUseHandler was not called");
            Assertions.assertFalse(interactEvent.isCancelled(), "The interaction must not be cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBlockInteractEvent skips the BlockUseHandler and cancels the interaction")
    void testInteractCancellationSkipsHandler() {
        PlayerMock player = server.addPlayer();
        Block block = TestUtilities.placeSlimefunBlock(server, blockItem.getItem(), world, player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onInteract(SlimefunBlockInteractEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEvent interactEvent = newInteractEvent(player, block);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertFalse(useHandlerCalled.get(), "A cancelled interaction must not call the BlockUseHandler");
            Assertions.assertTrue(interactEvent.isCancelled(), "A cancelled interaction must cancel the underlying PlayerInteractEvent");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("SlimefunBlockInteractEvent is not fired for vanilla blocks")
    void testInteractNotFiredForVanillaBlock() {
        PlayerMock player = server.addPlayer();
        Block vanillaBlock = world.getBlockAt(20, 64, 20);
        vanillaBlock.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onInteract(SlimefunBlockInteractEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = newInteractEvent(player, vanillaBlock);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla block");
            Assertions.assertFalse(interactEvent.isCancelled(), "A vanilla block interaction must stay untouched");
            Assertions.assertSame(Result.ALLOW, interactEvent.useInteractedBlock());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    // ---------- SlimefunBlockDispenseEvent ----------

    /**
     * Creates a mock of the placed dispenser block. MockBukkit does not implement
     * {@link Directional} block data, so the faced-block lookup of the real listener
     * needs a stubbed block data. BlockStorage was already registered for the location
     * by {@link TestUtilities#placeSlimefunBlock}.
     */
    private Block newDispenserBlockMock(Location loc) {
        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getType()).thenReturn(Material.DISPENSER);
        Mockito.when(block.getWorld()).thenReturn(world);
        Mockito.when(block.getLocation()).thenReturn(loc);

        Block below = Mockito.mock(Block.class);
        Mockito.when(below.getType()).thenReturn(Material.AIR);
        Mockito.when(block.getRelative(BlockFace.DOWN)).thenReturn(below);

        Block target = Mockito.mock(Block.class);
        Mockito.when(block.getRelative(BlockFace.NORTH)).thenReturn(target);

        Directional directional = Mockito.mock(Directional.class);
        Mockito.when(directional.getFacing()).thenReturn(BlockFace.NORTH);
        Mockito.when(block.getBlockData()).thenReturn(directional);

        Mockito.when(block.getState()).thenReturn(Mockito.mock(Dispenser.class));
        return block;
    }

    private BlockDispenseEvent newDispenseEvent(Block block) {
        return new BlockDispenseEvent(block, new ItemStack(Material.DIRT), new Vector(0, 0, 0));
    }

    @Test
    @DisplayName("SlimefunBlockDispenseEvent exposes its fields and validates constructor arguments")
    void testDispenseEventFieldsAndValidation() {
        Block block = world.getBlockAt(30, 64, 30);
        BlockDispenseEvent dispenseEvent = newDispenseEvent(block);

        SlimefunBlockDispenseEvent event = new SlimefunBlockDispenseEvent(dispenserItem, block, dispenseEvent);

        Assertions.assertEquals(dispenserItem, event.getSlimefunItem());
        Assertions.assertEquals(block, event.getBlock());
        Assertions.assertEquals(dispenseEvent, event.getDispenseEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockDispenseEvent(null, block, dispenseEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockDispenseEvent(dispenserItem, null, dispenseEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockDispenseEvent(dispenserItem, block, null));
    }

    @Test
    @DisplayName("Triggering a Slimefun dispenser fires SlimefunBlockDispenseEvent and runs the BlockDispenseHandler")
    void testDispenseFiresAndRunsHandler() {
        PlayerMock player = server.addPlayer();
        Location loc = TestUtilities.placeSlimefunBlock(server, dispenserItem.getItem(), world, player).getLocation();
        Block block = newDispenserBlockMock(loc);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDispense(SlimefunBlockDispenseEvent event) {
                seen[0] = true;
                Assertions.assertEquals(dispenserItem, event.getSlimefunItem());
                Assertions.assertEquals(block, event.getBlock());
                Assertions.assertNotNull(event.getDispenseEvent());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            server.getPluginManager().callEvent(newDispenseEvent(block));

            Assertions.assertTrue(seen[0], "SlimefunBlockDispenseEvent was not fired");
            Assertions.assertTrue(dispenseHandlerCalled.get(), "The BlockDispenseHandler was not called");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBlockDispenseEvent skips the BlockDispenseHandler")
    void testDispenseCancellationSkipsHandler() {
        PlayerMock player = server.addPlayer();
        Location loc = TestUtilities.placeSlimefunBlock(server, dispenserItem.getItem(), world, player).getLocation();
        Block block = newDispenserBlockMock(loc);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onDispense(SlimefunBlockDispenseEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            server.getPluginManager().callEvent(newDispenseEvent(block));

            Assertions.assertFalse(dispenseHandlerCalled.get(), "A cancelled dispense must not call the BlockDispenseHandler");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Without listeners the BlockDispenseHandler runs as before")
    void testDispenseNoListenersIdentical() {
        PlayerMock player = server.addPlayer();
        Location loc = TestUtilities.placeSlimefunBlock(server, dispenserItem.getItem(), world, player).getLocation();
        Block block = newDispenserBlockMock(loc);

        server.getPluginManager().callEvent(newDispenseEvent(block));

        Assertions.assertTrue(dispenseHandlerCalled.get(), "The BlockDispenseHandler must run unchanged without listeners");
    }
}

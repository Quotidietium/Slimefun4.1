package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.HopperTransferPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotHopperable;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the hopper transfer API expansion:
 * {@link HopperTransferPreventEvent}, exercised through the real {@link HopperListener}
 * hopper-prevention path. The hopper and machine inventories are Mockito mocks whose
 * locations point at a real {@link BlockStorage}-registered {@link NotHopperable} block.
 *
 * @author Zurker
 */
class TestHopperTransferPreventEvent {

    /**
     * A minimal {@link NotHopperable} test item.
     */
    static class NotHopperableMockItem extends MockSlimefunItem implements NotHopperable {
        NotHopperableMockItem(ItemGroup itemGroup, ItemStack item, String id) {
            super(itemGroup, item, id);
        }
    }

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static NotHopperableMockItem machine;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new HopperListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "hopper_test");
        Slimefun.getItemCfg().setValue("TEST_NOT_HOPPERABLE.enabled", true);
        machine = new NotHopperableMockItem(itemGroup, new ItemStack(Material.FURNACE), "TEST_NOT_HOPPERABLE");
        machine.register(plugin);
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
     * Places the not-hopperable machine as a real block backed by {@link BlockStorage}.
     */
    private Block placeMachine(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.FURNACE);
        BlockStorage.addBlockInfo(b, "id", machine.getId(), true);
        return b;
    }

    /**
     * Fires a hopper {@code ->} machine transfer through the real event pipeline and
     * returns the event for assertions.
     */
    private InventoryMoveItemEvent transferFromHopper(Block machineBlock, ItemStack item) {
        Inventory hopper = Mockito.mock(Inventory.class);
        Mockito.when(hopper.getType()).thenReturn(InventoryType.HOPPER);

        Inventory destination = Mockito.mock(Inventory.class);
        Mockito.when(destination.getLocation()).thenReturn(machineBlock.getLocation());

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(hopper, item, destination, true);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("HopperTransferPreventEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Inventory source = Mockito.mock(Inventory.class);
        Inventory destination = Mockito.mock(Inventory.class);
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        InventoryMoveItemEvent moveEvent = new InventoryMoveItemEvent(source, item, destination, true);

        HopperTransferPreventEvent event = new HopperTransferPreventEvent(machine, source, destination, item, moveEvent);

        Assertions.assertEquals(machine, event.getSlimefunItem());
        Assertions.assertEquals(source, event.getSource());
        Assertions.assertEquals(destination, event.getDestination());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(moveEvent, event.getMoveEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new HopperTransferPreventEvent(null, source, destination, item, moveEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HopperTransferPreventEvent(machine, null, destination, item, moveEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HopperTransferPreventEvent(machine, source, null, item, moveEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HopperTransferPreventEvent(machine, source, destination, null, moveEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HopperTransferPreventEvent(machine, source, destination, item, null));
    }

    @Test
    @DisplayName("A hopper transfer into a not-hopperable machine fires the event and is cancelled")
    void testTransferFiresAndCancels() {
        Block b = placeMachine(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(HopperTransferPreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(machine, event.getSlimefunItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryMoveItemEvent event = transferFromHopper(b, new ItemStack(Material.IRON_INGOT));

            Assertions.assertTrue(seen[0], "HopperTransferPreventEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The transfer must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling HopperTransferPreventEvent allows the transfer")
    void testEventCancellationAllowsTransfer() {
        Block b = placeMachine(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPrevent(HopperTransferPreventEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            InventoryMoveItemEvent event = transferFromHopper(b, new ItemStack(Material.IRON_INGOT));

            Assertions.assertFalse(event.isCancelled(), "A vetoed prevention must allow the transfer");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Prevention without listeners still cancels, preserving the old behavior")
    void testPreventionWithoutListenersStillCancels() {
        Block b = placeMachine(30, 30);

        InventoryMoveItemEvent event = transferFromHopper(b, new ItemStack(Material.IRON_INGOT));

        Assertions.assertTrue(event.isCancelled(), "The transfer must have been cancelled");
    }

    @Test
    @DisplayName("A transfer from a non-hopper inventory fires no event")
    void testNonHopperSourceFiresNothing() {
        Block b = placeMachine(40, 40);

        Inventory nonHopper = Mockito.mock(Inventory.class);
        Mockito.when(nonHopper.getType()).thenReturn(InventoryType.CHEST);

        Inventory destination = Mockito.mock(Inventory.class);
        Mockito.when(destination.getLocation()).thenReturn(b.getLocation());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(HopperTransferPreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryMoveItemEvent event = new InventoryMoveItemEvent(nonHopper, new ItemStack(Material.IRON_INGOT), destination, true);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-hopper source");
            Assertions.assertFalse(event.isCancelled(), "A non-hopper transfer must be left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A transfer into a vanilla block fires no event")
    void testVanillaDestinationFiresNothing() {
        Block b = world.getBlockAt(50, 1, 50);
        b.setType(Material.FURNACE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(HopperTransferPreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryMoveItemEvent event = transferFromHopper(b, new ItemStack(Material.IRON_INGOT));

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla destination");
            Assertions.assertFalse(event.isCancelled(), "A vanilla destination must be left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

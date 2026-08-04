package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Location;
import org.bukkit.Material;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;

import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the cargo API expansion: {@link CargoItemWithdrawEvent} and
 * {@link CargoItemInsertEvent}.
 *
 * @author Zurker
 */
class TestCargoItemEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = server.addSimpleWorld("cargo");
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("CargoItemWithdrawEvent exposes network, nodes, item, slot and cancellation")
    void testWithdrawEventFieldsAndCancellation() {
        CargoNet network = Mockito.mock(CargoNet.class);
        Location inputNode = new Location(world, 1, 64, 1);
        Block inputTarget = Mockito.mock(Block.class);
        ItemStack item = new ItemStack(Material.DIAMOND, 7);

        CargoItemWithdrawEvent event = new CargoItemWithdrawEvent(network, inputNode, inputTarget, item, 3);

        Assertions.assertEquals(network, event.getNetwork());
        Assertions.assertEquals(inputNode, event.getInputNode());
        Assertions.assertEquals(inputTarget, event.getInputTarget());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(3, event.getPreviousSlot());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        event.setCancelled(false);
        Assertions.assertFalse(event.isCancelled());
    }

    @Test
    @DisplayName("CargoItemInsertEvent exposes network, both nodes, target, item and cancellation")
    void testInsertEventFieldsAndCancellation() {
        CargoNet network = Mockito.mock(CargoNet.class);
        Location inputNode = new Location(world, 1, 64, 1);
        Location outputNode = new Location(world, 2, 64, 2);
        Block outputTarget = Mockito.mock(Block.class);
        ItemStack item = new ItemStack(Material.IRON_INGOT, 12);

        CargoItemInsertEvent event = new CargoItemInsertEvent(network, inputNode, outputNode, outputTarget, item);

        Assertions.assertEquals(network, event.getNetwork());
        Assertions.assertEquals(inputNode, event.getInputNode());
        Assertions.assertEquals(outputNode, event.getOutputNode());
        Assertions.assertEquals(outputTarget, event.getOutputTarget());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("Both cargo events are dispatchable and cancellation propagates to listeners")
    void testEventDispatchAndCancellationPropagation() {
        CargoNet network = Mockito.mock(CargoNet.class);
        Location inputNode = new Location(world, 1, 64, 1);
        Location outputNode = new Location(world, 2, 64, 2);
        Block target = Mockito.mock(Block.class);
        ItemStack item = new ItemStack(Material.GOLD_INGOT, 4);

        CargoListener listener = new CargoListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            CargoItemWithdrawEvent withdrawEvent = new CargoItemWithdrawEvent(network, inputNode, target, item, 0);
            server.getPluginManager().callEvent(withdrawEvent);

            Assertions.assertTrue(listener.withdrawSeen);
            Assertions.assertTrue(withdrawEvent.isCancelled());

            CargoItemInsertEvent insertEvent = new CargoItemInsertEvent(network, inputNode, outputNode, target, item);
            server.getPluginManager().callEvent(insertEvent);

            Assertions.assertTrue(listener.insertSeen);
            Assertions.assertEquals(outputNode, listener.seenOutputNode);
            Assertions.assertTrue(insertEvent.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    private static class CargoListener implements Listener {
        boolean withdrawSeen;
        boolean insertSeen;
        Location seenOutputNode;

        @EventHandler
        public void onWithdraw(CargoItemWithdrawEvent event) {
            withdrawSeen = true;
            event.setCancelled(true);
        }

        @EventHandler
        public void onInsert(CargoItemInsertEvent event) {
            insertSeen = true;
            seenOutputNode = event.getOutputNode();
            event.setCancelled(true);
        }
    }
}

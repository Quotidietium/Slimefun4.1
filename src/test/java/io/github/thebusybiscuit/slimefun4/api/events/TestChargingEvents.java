package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.Jetpack;

/**
 * Regression coverage for the rechargeable-item API expansion:
 * {@link ChargingBenchChargeEvent} and {@link JetpackThrustEvent}.
 *
 * @author Zurker
 */
class TestChargingEvents {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
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
    @DisplayName("ChargingBenchChargeEvent exposes bench, item, rechargeable, adjustable charge and cancellation")
    void testChargingBenchEventFields() {
        Block bench = Mockito.mock(Block.class);
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        Rechargeable rechargeable = Mockito.mock(Rechargeable.class);

        ChargingBenchChargeEvent event = new ChargingBenchChargeEvent(bench, item, rechargeable, 2.5F);

        Assertions.assertEquals(bench, event.getBench());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(rechargeable, event.getRechargeable());
        Assertions.assertEquals(2.5F, event.getCharge());
        Assertions.assertFalse(event.isCancelled());

        event.setCharge(4F);
        Assertions.assertEquals(4F, event.getCharge());

        // Zero and negative charge are rejected (Rechargeable#addItemCharge requires > 0)
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCharge(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCharge(-1));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("JetpackThrustEvent exposes jetpack, cost and cancellation")
    void testJetpackThrustEventFields() {
        Player player = server.addPlayer();
        Jetpack jetpack = Mockito.mock(Jetpack.class);

        JetpackThrustEvent event = new JetpackThrustEvent(player, jetpack, 0.08F);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(jetpack, event.getJetpack());
        Assertions.assertEquals(0.08F, event.getCost());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new JetpackThrustEvent(player, null, 0.08F));
    }

    @Test
    @DisplayName("Both charging events are dispatchable and listener mutations are visible")
    void testEventDispatch() {
        Block bench = Mockito.mock(Block.class);
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        Rechargeable rechargeable = Mockito.mock(Rechargeable.class);
        Player player = server.addPlayer();
        Jetpack jetpack = Mockito.mock(Jetpack.class);

        ChargingListener listener = new ChargingListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            ChargingBenchChargeEvent benchEvent = new ChargingBenchChargeEvent(bench, item, rechargeable, 2F);
            server.getPluginManager().callEvent(benchEvent);

            Assertions.assertTrue(listener.benchSeen);
            Assertions.assertEquals(6F, benchEvent.getCharge());

            JetpackThrustEvent thrustEvent = new JetpackThrustEvent(player, jetpack, 0.08F);
            server.getPluginManager().callEvent(thrustEvent);

            Assertions.assertTrue(listener.thrustSeen);
            Assertions.assertTrue(thrustEvent.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    private static class ChargingListener implements Listener {
        boolean benchSeen;
        boolean thrustSeen;

        @EventHandler
        public void onBenchCharge(ChargingBenchChargeEvent event) {
            benchSeen = true;
            event.setCharge(6F);
        }

        @EventHandler
        public void onThrust(JetpackThrustEvent event) {
            thrustSeen = true;
            event.setCancelled(true);
        }
    }
}

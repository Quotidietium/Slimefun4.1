package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the energy API expansion: {@link EnergyGenerateEvent} and
 * {@link EnergyNetTickEvent}.
 *
 * @author Zurker
 */
class TestEnergyEvents {

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
    @DisplayName("EnergyGenerateEvent exposes and allows modifying the contributed energy")
    void testGenerateEventFieldsAndMutation() {
        EnergyNetProvider provider = Mockito.mock(EnergyNetProvider.class);
        Location location = new Location(Mockito.mock(World.class), 1, 2, 3);

        EnergyGenerateEvent event = new EnergyGenerateEvent(provider, location, 50);

        Assertions.assertEquals(provider, event.getProvider());
        Assertions.assertEquals(location, event.getLocation());
        Assertions.assertEquals(50, event.getEnergy());
        Assertions.assertFalse(event.isCancelled());

        event.setEnergy(120);
        Assertions.assertEquals(120, event.getEnergy());

        // Negative energy is rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEnergy(-1));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("EnergyNetTickEvent reports supply, demand, net and surplus/deficit")
    void testTickEventComputedValues() {
        EnergyNet network = Mockito.mock(EnergyNet.class);
        Block regulator = Mockito.mock(Block.class);

        // Surplus case: supply exceeds demand
        EnergyNetTickEvent surplus = new EnergyNetTickEvent(network, regulator, 100, 40);
        Assertions.assertEquals(network, surplus.getNetwork());
        Assertions.assertEquals(regulator, surplus.getRegulator());
        Assertions.assertEquals(100, surplus.getSupply());
        Assertions.assertEquals(40, surplus.getDemand());
        Assertions.assertEquals(60, surplus.getNetEnergy());
        Assertions.assertTrue(surplus.isSurplus());
        Assertions.assertFalse(surplus.isDeficit());

        // Deficit case: demand exceeds supply
        EnergyNetTickEvent deficit = new EnergyNetTickEvent(network, regulator, 10, 90);
        Assertions.assertEquals(-80, deficit.getNetEnergy());
        Assertions.assertFalse(deficit.isSurplus());
        Assertions.assertTrue(deficit.isDeficit());

        // Balanced case
        EnergyNetTickEvent balanced = new EnergyNetTickEvent(network, regulator, 50, 50);
        Assertions.assertEquals(0, balanced.getNetEnergy());
        Assertions.assertFalse(balanced.isSurplus());
        Assertions.assertFalse(balanced.isDeficit());
    }

    @Test
    @DisplayName("EnergyNetTickEvent is dispatchable to listeners")
    void testTickEventDispatch() {
        EnergyNet network = Mockito.mock(EnergyNet.class);
        Block regulator = Mockito.mock(Block.class);

        CapturingListener listener = new CapturingListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            server.getPluginManager().callEvent(new EnergyNetTickEvent(network, regulator, 80, 80));

            Assertions.assertTrue(listener.fired);
            Assertions.assertEquals(80, listener.seenSupply);
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
    }

    @Test
    @DisplayName("EnergyConsumeEvent exposes and allows modifying the transferred energy")
    void testConsumeEventFieldsAndMutation() {
        EnergyNet network = Mockito.mock(EnergyNet.class);
        EnergyNetComponent component = Mockito.mock(EnergyNetComponent.class);
        Mockito.when(component.getCapacity()).thenReturn(100);
        Location location = new Location(Mockito.mock(World.class), 1, 2, 3);

        EnergyConsumeEvent event = new EnergyConsumeEvent(network, component, location, 40, 40);

        Assertions.assertEquals(network, event.getNetwork());
        Assertions.assertEquals(component, event.getComponent());
        Assertions.assertEquals(location, event.getLocation());
        Assertions.assertEquals(40, event.getEnergy());
        Assertions.assertEquals(40, event.getMaxTransfer());
        Assertions.assertFalse(event.isCancelled());

        event.setEnergy(0);
        Assertions.assertEquals(0, event.getEnergy());

        event.setEnergy(25);
        Assertions.assertEquals(25, event.getEnergy());

        // Energy above the maximum transfer or below zero is rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEnergy(41));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEnergy(-1));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnergyConsumeEvent(null, component, location, 40, 40));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnergyConsumeEvent(network, null, location, 40, 40));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnergyConsumeEvent(network, component, null, 40, 40));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnergyConsumeEvent(network, component, location, -1, 40));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnergyConsumeEvent(network, component, location, 40, 30));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnergyConsumeEvent(network, component, location, 40, 101));
    }

    @Test
    @DisplayName("EnergyConsumeEvent is dispatchable to listeners")
    void testConsumeEventDispatch() {
        EnergyNet network = Mockito.mock(EnergyNet.class);
        EnergyNetComponent component = Mockito.mock(EnergyNetComponent.class);
        Mockito.when(component.getCapacity()).thenReturn(100);
        Location location = new Location(Mockito.mock(World.class), 1, 2, 3);

        CapturingConsumeListener listener = new CapturingConsumeListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            server.getPluginManager().callEvent(new EnergyConsumeEvent(network, component, location, 60, 60));

            Assertions.assertTrue(listener.fired);
            Assertions.assertEquals(60, listener.seenEnergy);
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
    }

    private static class CapturingConsumeListener implements Listener {
        boolean fired;
        int seenEnergy;

        @EventHandler
        public void onConsume(EnergyConsumeEvent event) {
            fired = true;
            seenEnergy = event.getEnergy();
        }
    }

    private static class CapturingListener implements Listener {
        boolean fired;
        int seenSupply;

        @EventHandler
        public void onTick(EnergyNetTickEvent event) {
            fired = true;
            seenSupply = event.getSupply();
        }
    }
}

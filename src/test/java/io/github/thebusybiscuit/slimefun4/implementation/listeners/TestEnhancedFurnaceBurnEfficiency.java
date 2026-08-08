package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.EnhancedFurnaceBurnEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;

/**
 * Regression coverage for the EnhancedFurnaceBurnEvent setFuelEfficiency enhancement:
 * verifying that the fuel efficiency multiplier is now mutable and that
 * setFuelEfficiency validation works correctly.
 *
 * @author Zurker
 */
class TestEnhancedFurnaceBurnEfficiency {

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

    @Test
    @DisplayName("EnhancedFurnaceBurnEvent fuelEfficiency is initialized from the furnace")
    void testFuelEfficiencyInitialized() {
        EnhancedFurnace furnace = Mockito.mock(EnhancedFurnace.class);
        Mockito.when(furnace.getFuelEfficiency()).thenReturn(3);
        Block block = Mockito.mock(Block.class);
        FurnaceBurnEvent burnEvent = Mockito.mock(FurnaceBurnEvent.class);

        EnhancedFurnaceBurnEvent event = new EnhancedFurnaceBurnEvent(furnace, block, burnEvent);

        Assertions.assertEquals(3, event.getFuelEfficiency());
    }

    @Test
    @DisplayName("setFuelEfficiency overrides the multiplier")
    void testSetFuelEfficiency() {
        EnhancedFurnace furnace = Mockito.mock(EnhancedFurnace.class);
        Mockito.when(furnace.getFuelEfficiency()).thenReturn(2);
        Block block = Mockito.mock(Block.class);
        FurnaceBurnEvent burnEvent = Mockito.mock(FurnaceBurnEvent.class);

        EnhancedFurnaceBurnEvent event = new EnhancedFurnaceBurnEvent(furnace, block, burnEvent);

        event.setFuelEfficiency(5);
        Assertions.assertEquals(5, event.getFuelEfficiency());

        event.setFuelEfficiency(0);
        Assertions.assertEquals(0, event.getFuelEfficiency());
    }

    @Test
    @DisplayName("setFuelEfficiency rejects negative values")
    void testSetFuelEfficiencyValidation() {
        EnhancedFurnace furnace = Mockito.mock(EnhancedFurnace.class);
        Mockito.when(furnace.getFuelEfficiency()).thenReturn(2);
        Block block = Mockito.mock(Block.class);
        FurnaceBurnEvent burnEvent = Mockito.mock(FurnaceBurnEvent.class);

        EnhancedFurnaceBurnEvent event = new EnhancedFurnaceBurnEvent(furnace, block, burnEvent);

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setFuelEfficiency(-1));
    }

    @Test
    @DisplayName("Default efficiency is preserved when setFuelEfficiency is not called")
    void testDefaultEfficiencyPreserved() {
        EnhancedFurnace furnace = Mockito.mock(EnhancedFurnace.class);
        Mockito.when(furnace.getFuelEfficiency()).thenReturn(4);
        Block block = Mockito.mock(Block.class);
        FurnaceBurnEvent burnEvent = Mockito.mock(FurnaceBurnEvent.class);

        EnhancedFurnaceBurnEvent event = new EnhancedFurnaceBurnEvent(furnace, block, burnEvent);

        Assertions.assertEquals(4, event.getFuelEfficiency());
    }
}

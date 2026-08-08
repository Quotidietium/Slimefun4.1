package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.EnhancedFurnaceSmeltEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;

/**
 * Regression coverage for the EnhancedFurnaceSmeltEvent setAmount enhancement:
 * verifying that the fortune output amount is now mutable and that setAmount
 * validation works correctly.
 *
 * @author Zurker
 */
class TestEnhancedFurnaceSmeltAmount {

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
    @DisplayName("EnhancedFurnaceSmeltEvent amount is mutable via setAmount")
    void testAmountIsMutable() {
        EnhancedFurnace furnace = Mockito.mock(EnhancedFurnace.class);
        Block block = Mockito.mock(Block.class);
        FurnaceSmeltEvent smeltEvent = Mockito.mock(FurnaceSmeltEvent.class);

        EnhancedFurnaceSmeltEvent event = new EnhancedFurnaceSmeltEvent(furnace, block, smeltEvent, 3);

        Assertions.assertEquals(3, event.getAmount());

        event.setAmount(5);
        Assertions.assertEquals(5, event.getAmount());

        event.setAmount(1);
        Assertions.assertEquals(1, event.getAmount());
    }

    @Test
    @DisplayName("setAmount rejects values below 1")
    void testSetAmountValidation() {
        EnhancedFurnace furnace = Mockito.mock(EnhancedFurnace.class);
        Block block = Mockito.mock(Block.class);
        FurnaceSmeltEvent smeltEvent = Mockito.mock(FurnaceSmeltEvent.class);

        EnhancedFurnaceSmeltEvent event = new EnhancedFurnaceSmeltEvent(furnace, block, smeltEvent, 2);

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setAmount(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setAmount(-1));
    }

    @Test
    @DisplayName("Default amount is preserved when setAmount is not called")
    void testDefaultAmountPreserved() {
        EnhancedFurnace furnace = Mockito.mock(EnhancedFurnace.class);
        Block block = Mockito.mock(Block.class);
        FurnaceSmeltEvent smeltEvent = Mockito.mock(FurnaceSmeltEvent.class);

        EnhancedFurnaceSmeltEvent event = new EnhancedFurnaceSmeltEvent(furnace, block, smeltEvent, 4);

        Assertions.assertEquals(4, event.getAmount());
    }
}

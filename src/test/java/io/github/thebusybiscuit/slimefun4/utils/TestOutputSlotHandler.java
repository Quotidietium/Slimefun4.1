package io.github.thebusybiscuit.slimefun4.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;

class TestOutputSlotHandler {

    private static ServerMock server;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private InventoryClickEvent mockEvent(ClickType clickType) {
        InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class);
        Mockito.when(event.getClick()).thenReturn(clickType);
        Mockito.when(event.getHotbarButton()).thenReturn(0);
        return event;
    }

    @Test
    @DisplayName("Test that items can be taken out with an empty cursor")
    void testEmptyCursorAllowed() {
        Player player = server.addPlayer();
        ChestMenu.AdvancedMenuClickHandler handler = ChestMenuUtils.getDefaultOutputHandler();

        Assertions.assertTrue(handler.onClick(mockEvent(ClickType.LEFT), player, 0, null, null));
    }

    @Test
    @DisplayName("Test that items cannot be placed with a non-empty cursor")
    void testNonEmptyCursorDenied() {
        Player player = server.addPlayer();
        ChestMenu.AdvancedMenuClickHandler handler = ChestMenuUtils.getDefaultOutputHandler();

        Assertions.assertFalse(handler.onClick(mockEvent(ClickType.LEFT), player, 0, new ItemStack(Material.DIAMOND), null));
    }

    @Test
    @DisplayName("Test that number keys cannot swap items into the output slot")
    void testNumberKey() {
        Player player = server.addPlayer();
        ChestMenu.AdvancedMenuClickHandler handler = ChestMenuUtils.getDefaultOutputHandler();
        InventoryClickEvent event = mockEvent(ClickType.NUMBER_KEY);

        // An occupied hotbar slot would place its item into the output slot
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND));
        Assertions.assertFalse(handler.onClick(event, player, 0, null, null));

        // An empty hotbar slot is a pure take-out and stays allowed
        player.getInventory().setItem(0, null);
        Assertions.assertTrue(handler.onClick(event, player, 0, null, null));
    }

    @Test
    @DisplayName("Test that the offhand swap cannot place items into the output slot")
    void testSwapOffhand() {
        Player player = server.addPlayer();
        ChestMenu.AdvancedMenuClickHandler handler = ChestMenuUtils.getDefaultOutputHandler();
        InventoryClickEvent event = mockEvent(ClickType.SWAP_OFFHAND);

        player.getInventory().setItemInOffHand(new ItemStack(Material.DIAMOND));
        Assertions.assertFalse(handler.onClick(event, player, 0, null, null));

        player.getInventory().setItemInOffHand(null);
        Assertions.assertTrue(handler.onClick(event, player, 0, null, null));
    }

}

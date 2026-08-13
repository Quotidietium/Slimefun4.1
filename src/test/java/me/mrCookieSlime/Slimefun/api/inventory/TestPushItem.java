package me.mrCookieSlime.Slimefun.api.inventory;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * Regression coverage for {@link DirtyChestMenu#pushItem}: merging a stack smaller
 * than the free space of a partial slot previously subtracted the full free space
 * from the running amount, mutating the caller's input stack to a negative amount.
 *
 * @author Zurker
 */
class TestPushItem {

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

    private DirtyChestMenu newMenu() {
        BlockMenuPreset preset = new BlockMenuPreset("test_push_item", "test") {

            @Override
            public void init() {}

            @Override
            public boolean canOpen(Block b, Player p) {
                return true;
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };

        return new DirtyChestMenu(preset);
    }

    @Test
    @DisplayName("Merging less than the free space does not corrupt the input stack")
    void testPartialMergeKeepsInputConsistent() {
        DirtyChestMenu menu = newMenu();
        menu.addItem(0, new ItemStack(Material.STONE, 60));

        ItemStack input = new ItemStack(Material.STONE, 2);
        ItemStack rest = menu.pushItem(input, 0);

        Assertions.assertNull(rest, "Both items must have fit into the partial slot");
        Assertions.assertEquals(62, menu.getItemInSlot(0).getAmount(), "The slot must hold the merged total");
        Assertions.assertEquals(0, input.getAmount(), "The input stack must not be mutated to a negative amount");
    }

    @Test
    @DisplayName("Merging more than the free space returns the correct leftover")
    void testOverflowMergeReturnsLeftover() {
        DirtyChestMenu menu = newMenu();
        menu.addItem(0, new ItemStack(Material.STONE, 60));

        ItemStack input = new ItemStack(Material.STONE, 10);
        ItemStack rest = menu.pushItem(input, 0);

        Assertions.assertEquals(64, menu.getItemInSlot(0).getAmount(), "The slot must have been topped up");
        Assertions.assertNotNull(rest, "The surplus must be returned as leftover");
        Assertions.assertEquals(6, rest.getAmount(), "The leftover must hold exactly what did not fit");
        Assertions.assertEquals(6, input.getAmount(), "The input stack must reflect the leftover amount");
    }

    @Test
    @DisplayName("Pushing into an empty slot consumes the whole stack")
    void testPushIntoEmptySlot() {
        DirtyChestMenu menu = newMenu();

        ItemStack rest = menu.pushItem(new ItemStack(Material.DIRT, 5), 0);

        Assertions.assertNull(rest);
        Assertions.assertEquals(5, menu.getItemInSlot(0).getAmount());
    }
}

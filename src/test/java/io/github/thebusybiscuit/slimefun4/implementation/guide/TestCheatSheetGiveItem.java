package io.github.thebusybiscuit.slimefun4.implementation.guide;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the cheat-sheet give path in
 * {@link SurvivalSlimefunGuide#giveCheatItem(Player, SlimefunItem, boolean)}: the
 * old code dropped the addItem(...) return value, so a full inventory silently
 * voided the requested item instead of dropping it at the player's feet.
 *
 * @author Zurker
 */
class TestCheatSheetGiveItem {

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
    @DisplayName("A cheat-sheet item that does not fit is dropped instead of voided")
    void testFullInventoryDrops() {
        Player player = server.addPlayer();

        // Fill every inventory slot (including armor and off-hand) so nothing fits
        ItemStack filler = new ItemStack(Material.STONE);
        for (int i = 0; i < 41; i++) {
            player.getInventory().setItem(i, filler);
        }

        SlimefunItemStack stack = new SlimefunItemStack("_CHEAT_GIVE_TEST", Material.DIAMOND, "&bCheat Test");
        SlimefunItem item = new SlimefunItem(TestUtilities.getItemGroup(plugin, "cheat_give_test"), stack, RecipeType.NULL, new ItemStack[9]);
        item.register(plugin);

        int itemsBefore = player.getWorld().getEntities().stream().filter(e -> e instanceof org.bukkit.entity.Item).mapToInt(e -> 1).sum();

        SurvivalSlimefunGuide.giveCheatItem(player, item, false);

        int itemsAfter = player.getWorld().getEntities().stream().filter(e -> e instanceof org.bukkit.entity.Item).mapToInt(e -> 1).sum();
        Assertions.assertEquals(itemsBefore + 1, itemsAfter, "The item must be dropped at the player's feet when the inventory is full, never voided");
    }

    @Test
    @DisplayName("A cheat-sheet item that fits lands in the inventory")
    void testFittingItemArrives() {
        PlayerMock player = server.addPlayer();

        SlimefunItemStack stack = new SlimefunItemStack("_CHEAT_GIVE_TEST_2", Material.EMERALD, "&bCheat Test 2");
        SlimefunItem item = new SlimefunItem(TestUtilities.getItemGroup(plugin, "cheat_give_test_2"), stack, RecipeType.NULL, new ItemStack[9]);
        item.register(plugin);

        SurvivalSlimefunGuide.giveCheatItem(player, item, false);

        Assertions.assertTrue(player.getInventory().contains(Material.EMERALD), "The item must be added to the inventory");
    }
}

package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoInputNode;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for {@link ItemFilter} corruption tolerance: a corrupted
 * {@code filter-type} value must fail closed (deny everything) instead of silently
 * flipping the filter's semantics, which would reroute items to destinations they
 * were never meant to reach.
 *
 * @author Zurker
 */
class TestItemFilterCorruption {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "item_filter_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_FILTER_INPUT_NODE", Material.CHEST, "&7Test Filter Node");
        Slimefun.getItemCfg().setValue("_TEST_FILTER_INPUT_NODE.enabled", true);
        new CargoInputNode(itemGroup, stack, RecipeType.NULL, new ItemStack[9], null).register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private Block placeNode(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.CHEST);
        BlockStorage.addBlockInfo(b, "id", "_TEST_FILTER_INPUT_NODE");
        return b;
    }

    @Test
    @DisplayName("A corrupted filter-type fails closed instead of flipping to blacklist semantics")
    void testCorruptedFilterTypeFailsClosed() {
        Block b = placeNode(10, 10);
        // One item in a filter slot: blacklist semantics would allow everything else
        BlockStorage.getInventory(b).replaceExistingItem(19, new ItemStack(Material.STONE));
        BlockStorage.addBlockInfo(b.getLocation(), "filter-type", "blacklist", false);

        ItemFilter filter = new ItemFilter(b);
        Assertions.assertTrue(filter.test(new ItemStack(Material.DIRT)), "A blacklist must allow unlisted items");

        // The value is corrupted afterwards: the filter must fail closed (deny everything).
        // SlimefunItem#error rethrows RuntimeExceptions in unit tests (production logs and swallows),
        // so the update surfaces the exception here while the fail-closed state has already been applied.
        BlockStorage.addBlockInfo(b.getLocation(), "filter-type", "bogus-corrupted-value", false);
        Assertions.assertThrows(IllegalStateException.class, () -> filter.update(b));

        Assertions.assertFalse(filter.test(new ItemStack(Material.DIRT)), "A corrupted filter-type must deny everything, not silently act as a blacklist");
        Assertions.assertFalse(filter.test(new ItemStack(Material.STONE)), "A corrupted filter-type must deny everything");
    }

    @Test
    @DisplayName("A valid blacklist still allows unlisted items")
    void testValidBlacklistUnchanged() {
        Block b = placeNode(20, 20);
        BlockStorage.getInventory(b).replaceExistingItem(19, new ItemStack(Material.STONE));
        BlockStorage.addBlockInfo(b.getLocation(), "filter-type", "blacklist", false);

        ItemFilter filter = new ItemFilter(b);

        Assertions.assertTrue(filter.test(new ItemStack(Material.DIRT)), "A blacklist must allow unlisted items");
        Assertions.assertFalse(filter.test(new ItemStack(Material.STONE)), "A blacklist must reject listed items");
    }

    @Test
    @DisplayName("A valid whitelist still only allows listed items")
    void testValidWhitelistUnchanged() {
        Block b = placeNode(30, 30);
        BlockStorage.getInventory(b).replaceExistingItem(19, new ItemStack(Material.STONE));
        BlockStorage.addBlockInfo(b.getLocation(), "filter-type", "whitelist", false);

        ItemFilter filter = new ItemFilter(b);

        Assertions.assertTrue(filter.test(new ItemStack(Material.STONE)), "A whitelist must allow listed items");
        Assertions.assertFalse(filter.test(new ItemStack(Material.DIRT)), "A whitelist must reject unlisted items");
    }

    @Test
    @DisplayName("A missing filter-type keeps the historical blacklist default")
    void testMissingFilterTypeDefaultsToBlacklist() {
        Block b = placeNode(40, 40);
        BlockStorage.getInventory(b).replaceExistingItem(19, new ItemStack(Material.STONE));

        ItemFilter filter = new ItemFilter(b);

        Assertions.assertTrue(filter.test(new ItemStack(Material.DIRT)), "A missing filter-type must keep the blacklist default");
    }
}

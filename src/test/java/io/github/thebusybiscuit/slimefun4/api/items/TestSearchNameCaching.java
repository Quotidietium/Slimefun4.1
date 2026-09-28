package io.github.thebusybiscuit.slimefun4.api.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Discriminant coverage for the round-12 guide search name caching:
 * the compute-once display-name cache behind {@link SlimefunItem#getItemName()}
 * and the normalized {@link SlimefunItem#getSearchableName()} that the guide
 * search matches against.
 *
 * <p>
 * Both caches rest on {@code itemStackTemplate} being {@code private final}
 * and never replaced after construction - the same immutability that makes
 * them invalidation-free.
 * </p>
 *
 * @author Zurker
 */
class TestSearchNameCaching {

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
    @DisplayName("getItemName returns the template's display name, stably across calls")
    void testItemNameCache() {
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "SEARCH_NAME_ITEM", CustomItemStack.create(Material.PAPER, "&b颜色名字"));

        // The cached value must be exactly what the uncached computation yields.
        Assertions.assertEquals(ItemUtils.getItemName(item.getItem()), item.getItemName());

        // Repeated calls are served from the cache and stay correct.
        Assertions.assertEquals(item.getItemName(), item.getItemName());
    }

    @Test
    @DisplayName("getSearchableName is the color-stripped, lower-cased display name")
    void testSearchableName() {
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "SEARCHABLE_NAME_ITEM", CustomItemStack.create(Material.PAPER, "&bCoLoReD &cNaMe"));

        String expected = ChatColor.stripColor(ItemUtils.getItemName(item.getItem())).toLowerCase(java.util.Locale.ROOT);
        Assertions.assertEquals(expected, item.getSearchableName());
        Assertions.assertFalse(item.getSearchableName().contains(String.valueOf(ChatColor.COLOR_CHAR)), "No color codes may survive the normalization");
        Assertions.assertEquals(expected, item.getSearchableName(), "Repeated calls stay stable");
    }

    @Test
    @DisplayName("The guide search finds sparse matches and reports the page through the inventory")
    void testGuideSearchWalk() throws InterruptedException {
        Player searchPlayer = server.addPlayer();
        Slimefun.getWorldSettingsService().setEnabled(searchPlayer.getWorld(), true);

        ItemGroup group = new ItemGroup(new org.bukkit.NamespacedKey(plugin, "search_test_group"), new ItemStack(Material.CHEST));
        List<SlimefunItem> needles = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            String name = (i % 10 == 0 ? "&bNeedle Widget " : "&7Plain Widget ") + i;
            SlimefunItem item = new SlimefunItem(group, new SlimefunItemStack("SEARCH_WALK_ITEM_" + i, Material.PAPER, name), io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.NULL, new ItemStack[9]);
            item.register(plugin);
            item.load();

            if (i % 10 == 0) {
                needles.add(item);
            }
        }

        Player player = searchPlayer;
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        SlimefunGuide.openSearch(profile, "needle", SlimefunGuideMode.SURVIVAL_MODE, false);

        ItemStack[] contents = player.getOpenInventory().getTopInventory().getContents();
        int found = 0;

        for (ItemStack stack : contents) {
            // The displayed name goes through the localization layer, which reports
            // "Error: No language present" in the unit-test environment (pre-existing,
            // upstream behavior) - so match by result-item type instead.
            if (stack != null && stack.getType() == Material.PAPER) {
                found++;
            }
        }

        Assertions.assertEquals(needles.size(), found, "Every sparse needle must be found (and only those)");
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.guide;

import org.bukkit.Material;
import org.bukkit.World;
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

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunGuideSearchFilterEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the guide API expansion: {@link SlimefunGuideSearchFilterEvent},
 * exercised by driving the real {@link SurvivalSlimefunGuide#openSearch(PlayerProfile, String, boolean)}
 * against a registry holding a single searchable item.
 * <p>
 * The event lets addons override the built-in name matching per item: force-including an
 * item whose name does not contain the term, or force-hiding one whose name does.
 *
 * @author Zurker
 */
class TestSlimefunGuideSearchFilterEvent {

    private static final int FIRST_RESULT_SLOT = 9;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SurvivalSlimefunGuide guide;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "guide_filter_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_FILTERABLE_GADGET", Material.DIRT, "&fFiltertoken Gadget");
        Slimefun.getItemCfg().setValue("_TEST_FILTERABLE_GADGET.enabled", true);
        new SlimefunItem(itemGroup, stack, RecipeType.NULL, new ItemStack[9]).register(plugin);

        guide = new SurvivalSlimefunGuide(false, false);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private PlayerProfile search(Player player, String term) throws InterruptedException {
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        guide.openSearch(profile, term, true);
        return profile;
    }

    private ItemStack firstResult(Player player) {
        return player.getOpenInventory().getTopInventory().getItem(FIRST_RESULT_SLOT);
    }

    @Test
    @DisplayName("SlimefunGuideSearchFilterEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        SlimefunItem item = SlimefunItem.getById("_TEST_FILTERABLE_GADGET");

        SlimefunGuideSearchFilterEvent event = new SlimefunGuideSearchFilterEvent(player, item, "filtertoken", true);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals("filtertoken", event.getSearchTerm());
        Assertions.assertTrue(event.isMatching(), "The event must carry the guide's own verdict");

        event.setMatching(false);
        Assertions.assertFalse(event.isMatching());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunGuideSearchFilterEvent(player, null, "filtertoken", true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunGuideSearchFilterEvent(player, item, null, true));
    }

    @Test
    @DisplayName("The event carries the item and the processed (lowercased) search term")
    void testEventCarriesItemAndProcessedTerm() throws InterruptedException {
        Player player = server.addPlayer();
        SlimefunItem item = SlimefunItem.getById("_TEST_FILTERABLE_GADGET");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFilter(SlimefunGuideSearchFilterEvent event) {
                if (event.getItem().equals(item)) {
                    seen[0] = true;
                    Assertions.assertEquals("filtertoken", event.getSearchTerm(), "The term must be stripped and lowercased");
                    Assertions.assertTrue(event.isMatching(), "The built-in matching must have matched the item's name");
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            search(player, "FILTERTOKEN");

            Assertions.assertTrue(seen[0], "SlimefunGuideSearchFilterEvent was not fired for the gadget");
            Assertions.assertNotNull(firstResult(player), "The matching item must be shown");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("setMatching(true) force-includes an item whose name does not match the term")
    void testForceInclude() throws InterruptedException {
        Player player = server.addPlayer();

        Listener including = new Listener() {
            @EventHandler
            public void onFilter(SlimefunGuideSearchFilterEvent event) {
                if (event.getItem().getId().equals("_TEST_FILTERABLE_GADGET")) {
                    Assertions.assertFalse(event.isMatching(), "The built-in matching must not match 'nomatchxyz'");
                    event.setMatching(true);
                }
            }
        };
        server.getPluginManager().registerEvents(including, plugin);

        try {
            search(player, "nomatchxyz");

            Assertions.assertNotNull(firstResult(player), "The force-included item must appear in the results");
        } finally {
            HandlerList.unregisterAll(including);
        }
    }

    @Test
    @DisplayName("setMatching(false) force-hides an item whose name matches the term")
    void testForceHide() throws InterruptedException {
        Player player = server.addPlayer();

        Listener hiding = new Listener() {
            @EventHandler
            public void onFilter(SlimefunGuideSearchFilterEvent event) {
                if (event.getItem().getId().equals("_TEST_FILTERABLE_GADGET")) {
                    event.setMatching(false);
                }
            }
        };
        server.getPluginManager().registerEvents(hiding, plugin);

        try {
            search(player, "filtertoken");

            Assertions.assertNull(firstResult(player), "The force-hidden item must not appear in the results");
        } finally {
            HandlerList.unregisterAll(hiding);
        }
    }

    @Test
    @DisplayName("An untouched verdict reproduces the built-in matching, preserving the old behavior")
    void testUntouchedVerdictKeepsBuiltinMatching() throws InterruptedException {
        Player player = server.addPlayer();

        Listener watcher = new Listener() {
            @EventHandler
            public void onFilter(SlimefunGuideSearchFilterEvent event) {
                // Only observe, do not touch the verdict
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            search(player, "filtertoken");

            Assertions.assertNotNull(firstResult(player), "An untouched verdict must keep the built-in match");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

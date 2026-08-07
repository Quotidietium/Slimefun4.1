package io.github.thebusybiscuit.slimefun4.implementation.guide;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunGuideSearchEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the guide API expansion: {@link SlimefunGuideSearchEvent}, exercised
 * by driving the real {@link SurvivalSlimefunGuide#openSearch(PlayerProfile, String, boolean)}
 * against a registry holding a single searchable item.
 * <p>
 * The results menu opens as a real {@link org.bukkit.inventory.InventoryView}, so tests assert
 * it end-to-end: a matching search leaves a result in the first result slot, a redirected
 * search term matches what the addon rewrote it to, and a cancelled search opens nothing and
 * records no history entry.
 *
 * @author Zurker
 */
class TestSlimefunGuideSearchEvent {

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

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "guide_search_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SEARCHABLE_GADGET", Material.DIRT, "&fTestsearchtoken Gadget");
        Slimefun.getItemCfg().setValue("_TEST_SEARCHABLE_GADGET.enabled", true);
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

    /**
     * Runs a real guide search for the given term (fresh profile per player).
     */
    private PlayerProfile search(Player player, String term) throws InterruptedException {
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        guide.openSearch(profile, term, true);
        return profile;
    }

    private ItemStack firstResult(Player player) {
        return player.getOpenInventory().getTopInventory().getItem(FIRST_RESULT_SLOT);
    }

    @Test
    @DisplayName("SlimefunGuideSearchEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        SlimefunGuideSearchEvent event = new SlimefunGuideSearchEvent(player, "testsearchtoken");

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals("testsearchtoken", event.getSearchTerm());
        Assertions.assertFalse(event.isCancelled());

        event.setSearchTerm("other");
        Assertions.assertEquals("other", event.getSearchTerm());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunGuideSearchEvent(player, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSearchTerm(null));
    }

    @Test
    @DisplayName("A matching search fires the event and shows the result")
    void testSearchFiresEventAndShowsResult() throws InterruptedException {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSearch(SlimefunGuideSearchEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals("testsearchtoken", event.getSearchTerm());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            search(player, "testsearchtoken");

            Assertions.assertTrue(seen[0], "SlimefunGuideSearchEvent was not fired");
            Assertions.assertNotNull(firstResult(player), "The matching item must be shown in the first result slot");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Replacing the search term via setSearchTerm redirects the search")
    void testSetSearchTermRedirectsSearch() throws InterruptedException {
        Player player = server.addPlayer();

        Listener redirecting = new Listener() {
            @EventHandler
            public void onSearch(SlimefunGuideSearchEvent event) {
                if (event.getSearchTerm().equals("nomatchxyz")) {
                    event.setSearchTerm("testsearchtoken");
                }
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            PlayerProfile profile = search(player, "nomatchxyz");

            Assertions.assertNotNull(firstResult(player), "The redirected search term must have matched the item");
            Assertions.assertTrue(profile.getGuideHistory().size() > 0, "The search must have been recorded in the history");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunGuideSearchEvent opens no menu and records no history")
    void testCancelOpensNothingAndRecordsNothing() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        int historySize = profile.getGuideHistory().size();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSearch(SlimefunGuideSearchEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            guide.openSearch(profile, "testsearchtoken", true);

            Assertions.assertNotEquals(InventoryType.CHEST, player.getOpenInventory().getType(), "A cancelled search must not open a chest menu");
            Assertions.assertEquals(historySize, profile.getGuideHistory().size(), "A cancelled search must not be recorded in the history");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Searching without listeners still shows results, preserving the old behavior")
    void testSearchWithoutListenersStillShowsResult() throws InterruptedException {
        Player player = server.addPlayer();

        search(player, "testsearchtoken");

        Assertions.assertNotNull(firstResult(player), "The matching item must be shown in the first result slot");
    }

    @Test
    @DisplayName("A term that matches nothing shows no result")
    void testNonMatchingTermShowsNoResult() throws InterruptedException {
        Player player = server.addPlayer();

        search(player, "nomatchxyz");

        Assertions.assertEquals(InventoryType.CHEST, player.getOpenInventory().getType(), "An empty search still opens the results menu");
        Assertions.assertNull(firstResult(player), "A non-matching search must leave the first result slot empty");
    }
}

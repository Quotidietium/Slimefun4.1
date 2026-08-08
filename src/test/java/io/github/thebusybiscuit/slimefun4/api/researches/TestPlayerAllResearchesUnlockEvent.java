package io.github.thebusybiscuit.slimefun4.api.researches;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

import io.github.thebusybiscuit.slimefun4.api.events.PlayerAllResearchesUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the research milestone API expansion:
 * {@link PlayerAllResearchesUnlockEvent}, exercised by pre-unlocking all but one
 * research, then triggering the last unlock via the real {@link PlayerResearchTask}.
 *
 * @author Zurker
 */
class TestPlayerAllResearchesUnlockEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static Research research1;
    private static Research research2;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        Slimefun.getRegistry().setResearchingEnabled(true);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "all_research_test");

        SlimefunItem item1 = TestUtilities.mockSlimefunItem(plugin, "ALL_RESEARCH_ITEM_1", new ItemStack(Material.PAPER));
        item1.register(plugin);
        research1 = new Research(new NamespacedKey(plugin, "all_research_1"), 1, "Test A", 10);
        research1.register();
        research1.addItems(item1);

        SlimefunItem item2 = TestUtilities.mockSlimefunItem(plugin, "ALL_RESEARCH_ITEM_2", new ItemStack(Material.PAPER));
        item2.register(plugin);
        research2 = new Research(new NamespacedKey(plugin, "all_research_2"), 2, "Test B", 10);
        research2.register();
        research2.addItems(item2);
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
    @DisplayName("PlayerAllResearchesUnlockEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerAllResearchesUnlockEvent event = new PlayerAllResearchesUnlockEvent(player, profile, 2);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(profile, event.getProfile());
        Assertions.assertEquals(2, event.getTotalResearches());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PlayerAllResearchesUnlockEvent(null, profile, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PlayerAllResearchesUnlockEvent(player, null, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PlayerAllResearchesUnlockEvent(player, profile, 0));
    }

    @Test
    @DisplayName("Unlocking the last research fires PlayerAllResearchesUnlockEvent")
    void testLastUnlockFiresEvent() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        // Pre-unlock research 1 so only research 2 remains
        profile.setResearched(research1, true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAllUnlock(PlayerAllResearchesUnlockEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertTrue(event.getTotalResearches() >= 2, "The total must include at least our 2 test researches");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // Trigger unlock of research 2 via the real PlayerResearchTask
            PlayerResearchTask task = new PlayerResearchTask(research2, true, pl -> {});
            task.accept(profile);

            Assertions.assertTrue(seen[0], "PlayerAllResearchesUnlockEvent was not fired");
            Assertions.assertTrue(profile.hasUnlocked(research1));
            Assertions.assertTrue(profile.hasUnlocked(research2));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Unlocking when not all researches are done does not fire the event")
    void testPartialUnlockDoesNotFire() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        // Neither research is pre-unlocked — unlocking research 1 leaves research 2 undone
        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAllUnlock(PlayerAllResearchesUnlockEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerResearchTask task = new PlayerResearchTask(research1, true, pl -> {});
            task.accept(profile);

            Assertions.assertFalse(seen[0], "PlayerAllResearchesUnlockEvent must not fire when researches remain");
            Assertions.assertTrue(profile.hasUnlocked(research1));
            Assertions.assertFalse(profile.hasUnlocked(research2));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Without listeners the unlock still completes, preserving the old behavior")
    void testUnlockWithoutListenersCompletes() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        profile.setResearched(research1, true);

        PlayerResearchTask task = new PlayerResearchTask(research2, true, pl -> {});
        task.accept(profile);

        Assertions.assertTrue(profile.hasUnlocked(research2), "The research must have been unlocked");
    }
}

package io.github.thebusybiscuit.slimefun4.api.researches;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerResearchRankChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.events.ResearchProgressEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

/**
 * Discriminant coverage for the round-10 research bookkeeping optimization:
 * the compute-once {@link Research#hasEnabledItems()} cache and the live-set
 * counting inside {@link PlayerProfile}.
 *
 * <p>
 * The cache is invalidated by every state-changing path this suite exercises:
 * unbinding / re-binding an item via {@code SlimefunItem#setResearch} (which
 * {@code Research#addItems} routes through) and the belt-and-braces hook in
 * {@code SlimefunItem#register()} for the non-canonical bind-before-register
 * order. The live-set counting must be observably identical to counting the
 * defensive copy that the public API still returns.
 * </p>
 *
 * @author Zurker
 */
class TestResearchProgressCaching {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        Slimefun.getRegistry().setResearchingEnabled(true);
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
    @DisplayName("hasEnabledItems: unbind and rebind invalidate the compute-once cache")
    void testCacheInvalidationOnUnbindAndRebind() {
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "RESEARCH_CACHE_ITEM", CustomItemStack.create(Material.TORCH, "&b缓存测试"));
        item.register(plugin);

        Research research = new Research(new NamespacedKey(plugin, "research_cache"), 9600, "缓存测试", 1);
        research.register();
        research.addItems(item);

        Assertions.assertTrue(research.hasEnabledItems(), "A research bound to an enabled item must report hasEnabledItems()");

        // Cache was just computed as true - unbinding must invalidate it, not serve the stale true.
        item.setResearch(null);
        Assertions.assertFalse(research.hasEnabledItems(), "Unbinding the only item must flip hasEnabledItems() to false");

        // ...and re-binding must invalidate the cached false again.
        research.addItems(item);
        Assertions.assertTrue(research.hasEnabledItems(), "Re-binding the item must flip hasEnabledItems() back to true");
    }

    @Test
    @DisplayName("hasEnabledItems: bind-before-register is covered by the register() invalidation hook")
    void testBindBeforeRegister() {
        // Deliberately NOT registered yet: state is UNREGISTERED at bind time.
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "RESEARCH_CACHE_LATE_ITEM", CustomItemStack.create(Material.TORCH, "&b迟到注册"));

        Research research = new Research(new NamespacedKey(plugin, "research_cache_late"), 9601, "迟到注册", 1);
        research.register();
        research.addItems(item);

        // Non-ENABLED item states count as empty: the cache computed here is false...
        Assertions.assertFalse(research.hasEnabledItems(), "An UNREGISTERED bound item must not count as enabled");

        // ...and registering the item afterwards must invalidate that false even though
        // the canonical order is bind-after-register.
        item.register(plugin);
        Assertions.assertTrue(research.hasEnabledItems(), "Registering a previously-bound item must invalidate the cache");
    }

    @Test
    @DisplayName("getTitle walks the rank ladder using live-set counting and fires rank-change events on boundaries")
    void testTitleProgressionAndRankChangeEvents() throws InterruptedException {
        List<Research> originalResearches = new ArrayList<>(Slimefun.getRegistry().getResearches());
        List<String> originalRanks = new ArrayList<>(Slimefun.getRegistry().getResearchRanks());

        try {
            Slimefun.getRegistry().getResearches().clear();
            Slimefun.getRegistry().getResearchRanks().clear();
            Slimefun.getRegistry().getResearchRanks().addAll(List.of("新人", "学徒", "大师"));

            List<Research> researches = makeEnabledResearches("cache_rank_", 9610, 3);

            Player player = server.addPlayer();
            PlayerProfile profile = TestUtilities.awaitProfile(player);

            RankListener listener = new RankListener();
            server.getPluginManager().registerEvents(listener, plugin);

            try {
                // 1/3 unlocked: (int)(0.333 * (3 - 1)) = 0 -> 新人
                profile.setResearched(researches.get(0), true);
                Assertions.assertEquals("新人", profile.getTitle());
                Assertions.assertEquals(0, listener.events.size(), "No rank boundary crossed yet");

                // 2/3 unlocked: (int)(0.666 * 2) = 1 -> 学徒 (boundary crossing must fire)
                profile.setResearched(researches.get(1), true);
                Assertions.assertEquals("学徒", profile.getTitle());
                Assertions.assertEquals(1, listener.events.size());
                Assertions.assertEquals("新人", listener.events.get(0).getPreviousTitle());
                Assertions.assertEquals("学徒", listener.events.get(0).getNewTitle());

                // 3/3 unlocked: (int)(1.0 * 2) = 2 -> 大师
                profile.setResearched(researches.get(2), true);
                Assertions.assertEquals("大师", profile.getTitle());
                Assertions.assertEquals(2, listener.events.size());
            } finally {
                HandlerList.unregisterAll(listener);
            }
        } finally {
            Slimefun.getRegistry().getResearches().clear();
            Slimefun.getRegistry().getResearches().addAll(originalResearches);
            Slimefun.getRegistry().getResearchRanks().clear();
            Slimefun.getRegistry().getResearchRanks().addAll(originalRanks);
        }
    }

    @Test
    @DisplayName("Progress counts over the live set are identical to counts over the public defensive copy")
    void testLiveSetCountsMatchDefensiveCopy() throws InterruptedException {
        List<Research> originalResearches = new ArrayList<>(Slimefun.getRegistry().getResearches());
        List<String> originalRanks = new ArrayList<>(Slimefun.getRegistry().getResearchRanks());

        try {
            Slimefun.getRegistry().getResearches().clear();
            Slimefun.getRegistry().getResearchRanks().clear();

            List<Research> researches = makeEnabledResearches("cache_copy_", 9620, 4);

            Player player = server.addPlayer();
            PlayerProfile profile = TestUtilities.awaitProfile(player);

            ProgressListener listener = new ProgressListener();
            server.getPluginManager().registerEvents(listener, plugin);

            try {
                profile.setResearched(researches.get(0), true);
                profile.setResearched(researches.get(1), true);
                profile.setResearched(researches.get(2), true);

                Assertions.assertEquals(3, listener.events.size());
                ResearchProgressEvent last = listener.events.get(2);

                // The invariant of the optimization: counting data.getResearches() (live set)
                // yields exactly what counting the public ImmutableSet copy would yield.
                long expectedUnlocked = profile.getResearches().stream().filter(Research::hasEnabledItems).count();
                long expectedTotal = Slimefun.getRegistry().getResearches().stream().filter(Research::hasEnabledItems).count();

                Assertions.assertEquals(expectedUnlocked, last.getNewCount(), "newCount must match the defensive-copy computation");
                Assertions.assertEquals(expectedTotal, last.getTotalResearches(), "totalResearches must match the defensive-copy computation");
                Assertions.assertEquals(3, last.getNewCount());
                Assertions.assertEquals(4, last.getTotalResearches());
            } finally {
                HandlerList.unregisterAll(listener);
            }
        } finally {
            Slimefun.getRegistry().getResearches().clear();
            Slimefun.getRegistry().getResearches().addAll(originalResearches);
            Slimefun.getRegistry().getResearchRanks().clear();
            Slimefun.getRegistry().getResearchRanks().addAll(originalRanks);
        }
    }

    /**
     * Builds {@code count} distinct, registered {@link Research Researches}, each bound to a
     * freshly registered enabled {@link SlimefunItem} so that
     * {@code countNonEmptyResearches} counts them.
     */
    private List<Research> makeEnabledResearches(String keyPrefix, int startId, int count) {
        List<Research> researches = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, keyPrefix.toUpperCase() + "ITEM_" + i, CustomItemStack.create(Material.TORCH, "&b" + keyPrefix + i));
            item.register(plugin);

            Research research = new Research(new NamespacedKey(plugin, keyPrefix + i), startId + i, keyPrefix + i, 1);
            research.addItems(item);
            research.register();
            researches.add(research);
        }

        return researches;
    }

    private static class RankListener implements Listener {

        private final List<PlayerResearchRankChangeEvent> events = new ArrayList<>();

        @EventHandler
        public void onRankChange(PlayerResearchRankChangeEvent event) {
            events.add(event);
        }
    }

    private static class ProgressListener implements Listener {

        private final List<ResearchProgressEvent> events = new ArrayList<>();

        @EventHandler
        public void onProgress(ResearchProgressEvent event) {
            events.add(event);
        }
    }
}

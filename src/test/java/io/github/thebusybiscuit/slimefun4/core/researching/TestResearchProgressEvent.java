package io.github.thebusybiscuit.slimefun4.core.researching;

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
import io.github.thebusybiscuit.slimefun4.api.events.ResearchProgressEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

/**
 * Regression coverage for the fine-grained progression API expansion:
 * {@link ResearchProgressEvent}, fired by {@link PlayerProfile#setResearched(Research, boolean)}.
 *
 * @author Zurker
 */
class TestResearchProgressEvent {

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
    @DisplayName("ResearchProgressEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() throws InterruptedException {
        Research research = new Research(new NamespacedKey(plugin, "progress_fields"), 8950, "Fields", 1);

        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        ResearchProgressEvent event = new ResearchProgressEvent(profile, research, true, 0, 1, 3);

        Assertions.assertEquals(profile, event.getProfile());
        Assertions.assertEquals(player.getUniqueId(), event.getUUID());
        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(research, event.getResearch());
        Assertions.assertTrue(event.isUnlocked());
        Assertions.assertEquals(0, event.getPreviousCount());
        Assertions.assertEquals(1, event.getNewCount());
        Assertions.assertEquals(3, event.getTotalResearches());
        Assertions.assertEquals(1, event.getDelta());
        Assertions.assertEquals(1.0F / 3.0F, event.getProgressFraction(), 0.0001F);
        Assertions.assertFalse(event.isFullyUnlocked());

        // Reaching the total reports full progress.
        ResearchProgressEvent full = new ResearchProgressEvent(profile, research, true, 2, 3, 3);
        Assertions.assertTrue(full.isFullyUnlocked());
        Assertions.assertEquals(1.0F, full.getProgressFraction(), 0.0001F);

        // A world with no non-empty researches reports zero progress rather than dividing by zero.
        ResearchProgressEvent empty = new ResearchProgressEvent(profile, research, true, 0, 0, 0);
        Assertions.assertEquals(0.0F, empty.getProgressFraction(), 0.0001F);
        Assertions.assertFalse(empty.isFullyUnlocked());

        // Constructor validation.
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchProgressEvent(null, research, true, 0, 1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchProgressEvent(profile, null, true, 0, 1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchProgressEvent(profile, research, true, -1, 1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchProgressEvent(profile, research, true, 0, -1, 3));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchProgressEvent(profile, research, true, 0, 1, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchProgressEvent(profile, research, true, 0, 4, 3));
    }

    @Test
    @DisplayName("ResearchProgressEvent fires on genuine ownership flips and stays silent on no-ops, independent of rank config")
    void testProgressFiresOnOwnershipFlip() throws InterruptedException {
        // Snapshot the registry so the deterministic setup below cannot leak into other suites.
        List<Research> originalResearches = new ArrayList<>(Slimefun.getRegistry().getResearches());
        List<String> originalRanks = new ArrayList<>(Slimefun.getRegistry().getResearchRanks());

        try {
            Slimefun.getRegistry().getResearches().clear();
            // No ranks configured -> PlayerResearchRankChangeEvent would never fire, but the
            // fine-grained ResearchProgressEvent still must.
            Slimefun.getRegistry().getResearchRanks().clear();

            List<Research> researches = makeEnabledResearches("progress_flip_", 8900, 3);
            Assertions.assertEquals(3, researches.size());

            Player player = server.addPlayer();
            PlayerProfile profile = TestUtilities.awaitProfile(player);

            ProgressListener listener = new ProgressListener();
            server.getPluginManager().registerEvents(listener, plugin);

            try {
                // Unlock the first research: 0 -> 1.
                profile.setResearched(researches.get(0), true);
                Assertions.assertEquals(1, listener.events.size());
                ResearchProgressEvent first = listener.events.get(0);

                Assertions.assertEquals(researches.get(0), first.getResearch());
                Assertions.assertTrue(first.isUnlocked());
                Assertions.assertEquals(0, first.getPreviousCount());
                Assertions.assertEquals(1, first.getNewCount());
                Assertions.assertEquals(3, first.getTotalResearches());
                Assertions.assertEquals(1, first.getDelta());
                Assertions.assertEquals(1.0F / 3.0F, first.getProgressFraction(), 0.0001F);
                Assertions.assertFalse(first.isFullyUnlocked());

                // Re-unlocking the same research is a no-op: must NOT fire.
                profile.setResearched(researches.get(0), true);
                Assertions.assertEquals(1, listener.events.size(), "Re-unlocking an already-unlocked research must not fire");

                // Locking a never-unlocked research is a no-op: must NOT fire.
                profile.setResearched(researches.get(1), false);
                Assertions.assertEquals(1, listener.events.size(), "Locking a never-unlocked research must not fire");

                // Unlock the remaining two; the last one reaches the total.
                profile.setResearched(researches.get(1), true);
                Assertions.assertEquals(2, listener.events.size());

                profile.setResearched(researches.get(2), true);
                Assertions.assertEquals(3, listener.events.size());
                ResearchProgressEvent completing = listener.events.get(2);
                Assertions.assertEquals(3, completing.getNewCount());
                Assertions.assertTrue(completing.isFullyUnlocked());
                Assertions.assertEquals(1.0F, completing.getProgressFraction(), 0.0001F);

                // Re-lock the last research: 3 -> 2.
                profile.setResearched(researches.get(2), false);
                Assertions.assertEquals(4, listener.events.size());
                ResearchProgressEvent relock = listener.events.get(3);

                Assertions.assertFalse(relock.isUnlocked());
                Assertions.assertEquals(3, relock.getPreviousCount());
                Assertions.assertEquals(2, relock.getNewCount());
                Assertions.assertEquals(-1, relock.getDelta());
                Assertions.assertFalse(relock.isFullyUnlocked());
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

    /**
     * Collects every fired {@link ResearchProgressEvent} in order.
     */
    private static class ProgressListener implements Listener {

        private final List<ResearchProgressEvent> events = new ArrayList<>();

        @EventHandler
        public void onProgress(ResearchProgressEvent event) {
            events.add(event);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.core.researching;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerResearchRankChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.events.ResearchLockEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemState;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.api.researches.ResearchBuilder;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

/**
 * Regression coverage for the expanded progression / research API
 * ({@link ResearchBuilder}, {@link Research#addDependency(Research)},
 * {@link ResearchLockEvent}, {@link PlayerResearchRankChangeEvent}).
 *
 * @author Zurker
 */
class TestResearchProgressionApi {

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

    @Test
    @DisplayName("Test ResearchBuilder builds, binds items and dependencies, and registers")
    void testResearchBuilder() {
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "BUILDER_ITEM", CustomItemStack.create(Material.TORCH, "&bBuilder"));
        item.register(plugin);

        Research prerequisite = new Research(new NamespacedKey(plugin, "builder_prereq"), 8801, "Prereq", 3);
        prerequisite.register();

        NamespacedKey key = new NamespacedKey(plugin, "built_research");
        Research built = new ResearchBuilder().key(key).name("Built Research").cost(7).addItem(item).addDependency(prerequisite).register();

        Assertions.assertEquals(key, built.getKey());
        Assertions.assertEquals("Built Research", built.getUnlocalizedName());
        Assertions.assertEquals(7, built.getCost());
        Assertions.assertTrue(built.hasDependencies());
        Assertions.assertTrue(built.getDependencies().contains(prerequisite));
        Assertions.assertEquals(built, item.getResearch());
        Assertions.assertTrue(Slimefun.getRegistry().getResearches().contains(built));
    }

    @Test
    @DisplayName("Test ResearchBuilder rejects missing required fields")
    void testResearchBuilderValidation() {
        // org.apache.commons.lang.Validate.notNull(...) throws IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchBuilder().name("No Key").build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchBuilder().key(new NamespacedKey(plugin, "no_name")).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchBuilder().cost(-5));
    }

    @Test
    @DisplayName("Test ResearchBuilder derives a stable non-zero default legacy id from the key")
    void testBuilderDerivedDefaultId() {
        // The numeric id is the persistence key for unlock state. The historical default
        // of 0 collided with the built-in "walking_sticks" research (id 0) and with every
        // other default-id research, silently corrupting unlock state.
        NamespacedKey key = new NamespacedKey(plugin, "derived_default_id");
        Research first = new ResearchBuilder().key(key).name("Derived A").build();
        Research second = new ResearchBuilder().key(key).name("Derived B").build();
        Research other = new ResearchBuilder().key(new NamespacedKey(plugin, "derived_other_key")).name("Derived C").build();

        Assertions.assertNotEquals(0, first.getID(), "derived id must not fall back to the legacy default 0");
        Assertions.assertEquals(first.getID(), second.getID(), "same key must derive the same id (stable across restarts)");
        Assertions.assertNotEquals(first.getID(), other.getID(), "different keys must derive different ids");
    }

    @Test
    @DisplayName("Test ResearchBuilder preserves an explicitly provided legacy id")
    void testBuilderExplicitIdPreserved() {
        Research research = new ResearchBuilder().key(new NamespacedKey(plugin, "explicit_id_research")).name("Explicit").id(987654).build();
        Assertions.assertEquals(987654, research.getID());
    }

    @Test
    @DisplayName("Test addDependency / removeDependency / getDependencies")
    void testDependencyManagement() {
        Research a = new Research(new NamespacedKey(plugin, "dep_a"), 8802, "A", 1);
        Research b = new Research(new NamespacedKey(plugin, "dep_b"), 8803, "B", 1);
        a.register();
        b.register();

        Assertions.assertFalse(a.hasDependencies());
        Assertions.assertTrue(a.getDependencies().isEmpty());

        a.addDependency(b);
        Assertions.assertTrue(a.hasDependencies());
        Assertions.assertEquals(1, a.getDependencies().size());
        Assertions.assertTrue(a.getDependencies().contains(b));

        // The returned set is unmodifiable
        Assertions.assertThrows(UnsupportedOperationException.class, () -> a.getDependencies().add(b));

        a.removeDependency(b);
        Assertions.assertFalse(a.hasDependencies());
    }

    @Test
    @DisplayName("Test circular dependency detection")
    void testCycleDetection() {
        Research a = new Research(new NamespacedKey(plugin, "cycle_a"), 8804, "A", 1);
        Research b = new Research(new NamespacedKey(plugin, "cycle_b"), 8805, "B", 1);
        Research c = new Research(new NamespacedKey(plugin, "cycle_c"), 8806, "C", 1);
        a.register();
        b.register();
        c.register();

        // a -> b -> c is fine
        b.addDependency(c);
        a.addDependency(b);
        Assertions.assertTrue(a.getDependencies().contains(b));

        // c -> a would form a -> b -> c -> a cycle
        Assertions.assertThrows(IllegalArgumentException.class, () -> c.addDependency(a));

        // direct cycle: a -> a
        Assertions.assertThrows(IllegalArgumentException.class, () -> a.addDependency(a));

        // duplicates are not stored twice
        a.addDependency(b);
        Assertions.assertEquals(1, a.getDependencies().size());
    }

    @Test
    @DisplayName("Test meetsDependencies and getFirstMissingDependency against a PlayerProfile")
    void testMeetsDependencies() throws InterruptedException {
        Research prerequisite = new Research(new NamespacedKey(plugin, "meet_prereq"), 8807, "Prereq", 1);
        Research advanced = new Research(new NamespacedKey(plugin, "meet_advanced"), 8808, "Advanced", 1);
        prerequisite.register();
        advanced.register();
        advanced.addDependency(prerequisite);

        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        // Prerequisite not yet unlocked -> not satisfied
        Assertions.assertFalse(advanced.meetsDependencies(profile));
        Assertions.assertFalse(advanced.meetsDependencies(player));
        Assertions.assertEquals(prerequisite, advanced.getFirstMissingDependency(profile).get());

        // Unlock the prerequisite -> satisfied
        profile.setResearched(prerequisite, true);
        Assertions.assertTrue(advanced.meetsDependencies(profile));
        Assertions.assertFalse(advanced.getFirstMissingDependency(profile).isPresent());

        // A research without dependencies is always satisfied
        Assertions.assertTrue(prerequisite.meetsDependencies(profile));
    }

    @Test
    @DisplayName("Test research.unlock() enforces dependencies on every path, not only in the guide")
    void testUnlockEnforcesDependencies() throws InterruptedException {
        Research prereq = new Research(new NamespacedKey(plugin, "enforce_prereq"), 8815, "Enforce Prereq", 1);
        Research dependent = new Research(new NamespacedKey(plugin, "enforce_dependent"), 8816, "Enforce Dependent", 1);
        prereq.register();
        dependent.register();
        dependent.addDependency(prereq);

        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        // Direct unlock() calls (KnowledgeTome shares, addons) must not skip the tree
        dependent.unlock(player, true);
        Assertions.assertFalse(profile.hasUnlocked(dependent), "A research with unmet prerequisites must not unlock");

        prereq.unlock(player, true);
        Assertions.assertTrue(profile.hasUnlocked(prereq));

        dependent.unlock(player, true);
        Assertions.assertTrue(profile.hasUnlocked(dependent), "A research must unlock once its prerequisites are unlocked");
    }

    @Test
    @DisplayName("Test a dependency refusal runs the cancelHandler so callers can compensate a taken cost")
    void testDependencyRefusalRunsCancelHandler() throws InterruptedException {
        Research prereq = new Research(new NamespacedKey(plugin, "refund_prereq"), 8817, "Refund Prereq", 1);
        Research dependent = new Research(new NamespacedKey(plugin, "refund_dependent"), 8818, "Refund Dependent", 1);
        prereq.register();
        dependent.register();
        dependent.addDependency(prereq);

        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        boolean[] compensated = { false };
        dependent.unlock(player, true, null, null, () -> compensated[0] = true);

        Assertions.assertTrue(compensated[0], "The cancelHandler must run when prerequisites are unmet");
        Assertions.assertFalse(profile.hasUnlocked(dependent));
    }

    @Test
    @DisplayName("Test ResearchLockEvent fires when a research is re-locked")
    void testResearchLockEvent() throws InterruptedException {
        Research research = new Research(new NamespacedKey(plugin, "lock_event_research"), 8809, "Lockable", 1);
        research.register();

        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        profile.setResearched(research, true);
        Assertions.assertTrue(profile.hasUnlocked(research));

        profile.setResearched(research, false);

        server.getPluginManager().assertEventFired(ResearchLockEvent.class, event -> {
            Assertions.assertEquals(profile, event.getProfile());
            Assertions.assertEquals(research, event.getResearch());
            Assertions.assertEquals(player.getUniqueId(), event.getUUID());
            Assertions.assertFalse(event.isCancelled());
            return true;
        });

        Assertions.assertFalse(profile.hasUnlocked(research));
    }

    @Test
    @DisplayName("Test ResearchLockEvent is cancellable and prevents the removal")
    void testResearchLockEventCancellable() throws InterruptedException {
        Research research = new Research(new NamespacedKey(plugin, "lock_cancel_research"), 8810, "Protected", 1);
        research.register();

        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        profile.setResearched(research, true);

        CancellingLockListener listener = new CancellingLockListener(research);
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            profile.setResearched(research, false);

            // The cancelled event must have prevented the removal.
            Assertions.assertTrue(profile.hasUnlocked(research));
            Assertions.assertTrue(listener.wasFired);
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    @Test
    @DisplayName("Test PlayerResearchRankChangeEvent fires when crossing a rank boundary")
    void testRankChangeEvent() throws InterruptedException {
        // Back up the registry's researches/ranks and replace them for a deterministic setup
        // (a single enabled research across a 2-rank ladder: Beginner -> Master).
        List<Research> originalResearches = new ArrayList<>(Slimefun.getRegistry().getResearches());
        List<String> originalRanks = new ArrayList<>(Slimefun.getRegistry().getResearchRanks());

        try {
            Slimefun.getRegistry().getResearches().clear();
            Slimefun.getRegistry().getResearchRanks().clear();
            Slimefun.getRegistry().getResearchRanks().addAll(Arrays.asList("Beginner", "Master"));

            SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "RANK_RESEARCH_ITEM", CustomItemStack.create(Material.TORCH, "&bRank"));
            item.register(plugin);
            Assertions.assertEquals(ItemState.ENABLED, item.getState());

            Research research = new Research(new NamespacedKey(plugin, "rank_research"), 8811, "Rank", 1);
            research.addItems(item);
            research.register();

            Player player = server.addPlayer();
            PlayerProfile profile = TestUtilities.awaitProfile(player);

            // Unlocking the only research crosses the Beginner -> Master boundary.
            profile.setResearched(research, true);

            server.getPluginManager().assertEventFired(PlayerResearchRankChangeEvent.class, event -> {
                Assertions.assertEquals(profile, event.getProfile());
                Assertions.assertEquals("Beginner", event.getPreviousTitle());
                Assertions.assertEquals("Master", event.getNewTitle());
                Assertions.assertTrue(event.isPromotion());
                return true;
            });
        } finally {
            Slimefun.getRegistry().getResearches().clear();
            Slimefun.getRegistry().getResearches().addAll(originalResearches);
            Slimefun.getRegistry().getResearchRanks().clear();
            Slimefun.getRegistry().getResearchRanks().addAll(originalRanks);
        }
    }

    /**
     * Simple listener that cancels the {@link ResearchLockEvent} for a specific {@link Research}.
     */
    private static class CancellingLockListener implements Listener {

        private final Research protectedResearch;
        private boolean wasFired = false;

        CancellingLockListener(Research protectedResearch) {
            this.protectedResearch = protectedResearch;
        }

        @EventHandler
        public void onLock(ResearchLockEvent event) {
            if (event.getResearch().equals(protectedResearch)) {
                wasFired = true;
                event.setCancelled(true);
            }
        }
    }
}

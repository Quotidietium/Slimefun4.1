package io.github.thebusybiscuit.slimefun4.core.guide.options;

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

import io.github.thebusybiscuit.slimefun4.api.events.GuideModeChangeEvent;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the guide settings API expansion: {@link GuideModeChangeEvent},
 * exercised by driving the real {@link GuideModeOption#applyModeChange} switch logic.
 * <p>
 * The tests drive the extracted switch method directly instead of {@code onClick}:
 * reopening the settings menu afterwards touches the GitHub service, which cannot run
 * under MockBukkit. The mode itself is read back through the option's own
 * {@code getSelectedOption}, so tests assert the outcome end-to-end.
 *
 * @author Zurker
 */
class TestGuideModeChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static GuideModeOption option;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        option = new GuideModeOption();
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
     * A fresh survival guide; the static guide item must never be mutated by a test.
     */
    private ItemStack survivalGuide() {
        return SlimefunGuide.getItem(SlimefunGuideMode.SURVIVAL_MODE).clone();
    }

    private SlimefunGuideMode modeOf(Player p, ItemStack guide) {
        return option.getSelectedOption(p, guide).orElse(null);
    }

    @Test
    @DisplayName("GuideModeChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack guide = survivalGuide();

        GuideModeChangeEvent event = new GuideModeChangeEvent(player, guide, SlimefunGuideMode.SURVIVAL_MODE, SlimefunGuideMode.CHEAT_MODE);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(guide, event.getGuide());
        Assertions.assertEquals(SlimefunGuideMode.SURVIVAL_MODE, event.getPreviousMode());
        Assertions.assertEquals(SlimefunGuideMode.CHEAT_MODE, event.getNewMode());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GuideModeChangeEvent(player, null, SlimefunGuideMode.SURVIVAL_MODE, SlimefunGuideMode.CHEAT_MODE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GuideModeChangeEvent(player, guide, null, SlimefunGuideMode.CHEAT_MODE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GuideModeChangeEvent(player, guide, SlimefunGuideMode.SURVIVAL_MODE, null));
    }

    @Test
    @DisplayName("Switching modes fires the event and rewrites the guide")
    void testChangeFiresEventAndApplies() {
        Player player = server.addPlayer();
        ItemStack guide = survivalGuide();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChange(GuideModeChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(guide, event.getGuide());
                Assertions.assertEquals(SlimefunGuideMode.SURVIVAL_MODE, event.getPreviousMode());
                Assertions.assertEquals(SlimefunGuideMode.CHEAT_MODE, event.getNewMode());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            option.applyModeChange(player, guide, SlimefunGuideMode.SURVIVAL_MODE, SlimefunGuideMode.CHEAT_MODE);

            Assertions.assertTrue(seen[0], "GuideModeChangeEvent was not fired");
            Assertions.assertEquals(SlimefunGuideMode.CHEAT_MODE, modeOf(player, guide), "The guide must have switched to cheat mode");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling GuideModeChangeEvent keeps the current mode")
    void testCancelKeepsMode() {
        Player player = server.addPlayer();
        ItemStack guide = survivalGuide();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onChange(GuideModeChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            option.applyModeChange(player, guide, SlimefunGuideMode.SURVIVAL_MODE, SlimefunGuideMode.CHEAT_MODE);

            Assertions.assertEquals(SlimefunGuideMode.SURVIVAL_MODE, modeOf(player, guide), "A vetoed change must keep the survival mode");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Switching without listeners still applies, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        ItemStack guide = survivalGuide();

        option.applyModeChange(player, guide, SlimefunGuideMode.SURVIVAL_MODE, SlimefunGuideMode.CHEAT_MODE);

        Assertions.assertEquals(SlimefunGuideMode.CHEAT_MODE, modeOf(player, guide), "The guide must have switched to cheat mode");
    }

    @Test
    @DisplayName("Resolving to the current mode fires no event")
    void testSameModeFiresNothing() {
        Player player = server.addPlayer();
        ItemStack guide = survivalGuide();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChange(GuideModeChangeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            option.applyModeChange(player, guide, SlimefunGuideMode.SURVIVAL_MODE, SlimefunGuideMode.SURVIVAL_MODE);

            Assertions.assertFalse(seen[0], "No event must be fired when the mode does not change");
            Assertions.assertEquals(SlimefunGuideMode.SURVIVAL_MODE, modeOf(player, guide), "The guide must stay in survival mode");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Switching back from cheat to survival fires with the reversed modes")
    void testReverseChangeFiresEvent() {
        Player player = server.addPlayer();
        ItemStack guide = survivalGuide();
        option.applyModeChange(player, guide, SlimefunGuideMode.SURVIVAL_MODE, SlimefunGuideMode.CHEAT_MODE);
        Assertions.assertEquals(SlimefunGuideMode.CHEAT_MODE, modeOf(player, guide));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onChange(GuideModeChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(SlimefunGuideMode.CHEAT_MODE, event.getPreviousMode());
                Assertions.assertEquals(SlimefunGuideMode.SURVIVAL_MODE, event.getNewMode());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            option.applyModeChange(player, guide, SlimefunGuideMode.CHEAT_MODE, SlimefunGuideMode.SURVIVAL_MODE);

            Assertions.assertTrue(seen[0], "GuideModeChangeEvent was not fired for the reverse switch");
            Assertions.assertEquals(SlimefunGuideMode.SURVIVAL_MODE, modeOf(player, guide), "The guide must have switched back to survival mode");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

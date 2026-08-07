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

import io.github.thebusybiscuit.slimefun4.api.events.PlayerGuideOptionChangeEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the guide settings API expansion:
 * {@link PlayerGuideOptionChangeEvent}, exercised by driving the real
 * {@link FireworksOption#applyOptionChange} and
 * {@link LearningAnimationOption#applyOptionChange} switch logic directly.
 * <p>
 * The tests drive the extracted switch methods instead of {@code onClick}: reopening the
 * settings menu touches services that cannot run under MockBukkit. The value itself is read
 * back through the option's own {@code getSelectedOption}, so tests assert the outcome
 * end-to-end.
 *
 * @author Zurker
 */
class TestPlayerGuideOptionChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static FireworksOption fireworksOption;
    private static LearningAnimationOption learningOption;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        fireworksOption = new FireworksOption();
        learningOption = new LearningAnimationOption();
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private boolean fireworksEnabled(Player p) {
        return fireworksOption.getSelectedOption(p, null).orElse(true);
    }

    private boolean learningEnabled(Player p) {
        return learningOption.getSelectedOption(p, null).orElse(true);
    }

    @Test
    @DisplayName("PlayerGuideOptionChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        PlayerGuideOptionChangeEvent event = new PlayerGuideOptionChangeEvent(player, PlayerGuideOptionChangeEvent.Reason.RESEARCH_FIREWORKS, true, false);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(PlayerGuideOptionChangeEvent.Reason.RESEARCH_FIREWORKS, event.getReason());
        Assertions.assertTrue(event.getPreviousValue());
        Assertions.assertFalse(event.getNewValue());
        Assertions.assertFalse(event.isCancelled());

        event.setNewValue(true);
        Assertions.assertTrue(event.getNewValue());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PlayerGuideOptionChangeEvent(player, null, true, false));
    }

    @Test
    @DisplayName("Toggling the fireworks setting fires the event and stores the new value")
    void testFireworksToggleFiresAndStores() {
        Player player = server.addPlayer();
        Assertions.assertTrue(fireworksEnabled(player), "The default fireworks value must be true");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOptionChange(PlayerGuideOptionChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(PlayerGuideOptionChangeEvent.Reason.RESEARCH_FIREWORKS, event.getReason());
                Assertions.assertTrue(event.getPreviousValue());
                Assertions.assertFalse(event.getNewValue());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            fireworksOption.applyOptionChange(player, ItemStack.empty(), false);

            Assertions.assertTrue(seen[0], "PlayerGuideOptionChangeEvent was not fired");
            Assertions.assertFalse(fireworksEnabled(player), "The new value must have been stored");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Toggling the learning animation setting fires the event and stores the new value")
    void testLearningAnimationToggleFiresAndStores() {
        Player player = server.addPlayer();
        Assertions.assertTrue(learningEnabled(player), "The default learning value must be true");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOptionChange(PlayerGuideOptionChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(PlayerGuideOptionChangeEvent.Reason.LEARNING_ANIMATION, event.getReason());
                Assertions.assertTrue(event.getPreviousValue());
                Assertions.assertFalse(event.getNewValue());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            learningOption.applyOptionChange(player, ItemStack.empty(), false);

            Assertions.assertTrue(seen[0], "PlayerGuideOptionChangeEvent was not fired");
            Assertions.assertFalse(learningEnabled(player), "The new value must have been stored");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling PlayerGuideOptionChangeEvent keeps the stored value")
    void testCancelKeepsStoredValue() {
        Player player = server.addPlayer();
        Assertions.assertTrue(fireworksEnabled(player));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onOptionChange(PlayerGuideOptionChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            fireworksOption.applyOptionChange(player, ItemStack.empty(), false);

            Assertions.assertTrue(fireworksEnabled(player), "A vetoed change must keep the old value");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the value via setNewValue stores the override")
    void testSetNewValueOverride() {
        Player player = server.addPlayer();
        Assertions.assertTrue(learningEnabled(player));

        Listener overriding = new Listener() {
            @EventHandler
            public void onOptionChange(PlayerGuideOptionChangeEvent event) {
                // Force it back to enabled even though the toggle wanted disabled
                event.setNewValue(true);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            learningOption.applyOptionChange(player, ItemStack.empty(), false);

            Assertions.assertTrue(learningEnabled(player), "The overridden value must have been stored");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Toggling without listeners still stores it, preserving the old behavior")
    void testToggleWithoutListenersApplies() {
        Player player = server.addPlayer();
        Assertions.assertTrue(fireworksEnabled(player));

        fireworksOption.applyOptionChange(player, ItemStack.empty(), false);

        Assertions.assertFalse(fireworksEnabled(player), "The new value must have been stored");
    }

    @Test
    @DisplayName("Toggling to the already-stored value fires no event")
    void testSameValueFiresNothing() {
        Player player = server.addPlayer();
        Assertions.assertTrue(fireworksEnabled(player));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOptionChange(PlayerGuideOptionChangeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // Toggling to true while already true: no change, no event
            fireworksOption.applyOptionChange(player, ItemStack.empty(), true);

            Assertions.assertFalse(seen[0], "No event must be fired when the value does not change");
            Assertions.assertTrue(fireworksEnabled(player));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

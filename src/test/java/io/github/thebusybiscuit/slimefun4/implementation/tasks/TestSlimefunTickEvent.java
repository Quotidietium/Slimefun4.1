package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunTickEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the tick monitoring API expansion:
 * {@link SlimefunTickEvent}, exercised by calling the real
 * {@link io.github.thebusybiscuit.slimefun4.implementation.tasks.TickerTask#run()}
 * which fires the event in its finally block.
 *
 * @author Zurker
 */
class TestSlimefunTickEvent {

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
    @DisplayName("SlimefunTickEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        SlimefunTickEvent event = new SlimefunTickEvent(1_500_000);

        Assertions.assertEquals(1_500_000L, event.getTickDurationNanos());
        Assertions.assertEquals(1.5, event.getTickDurationMillis(), 0.001);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunTickEvent(-1));
    }

    @Test
    @DisplayName("Running the ticker fires SlimefunTickEvent")
    void testTickFiresEvent() {
        boolean[] seen = { false };
        long[] duration = { 0 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTick(SlimefunTickEvent event) {
                seen[0] = true;
                duration[0] = event.getTickDurationNanos();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Slimefun.getTickerTask().run();

            Assertions.assertTrue(seen[0], "SlimefunTickEvent was not fired");
            Assertions.assertTrue(duration[0] >= 0, "The tick duration must be non-negative");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Running without listeners still completes, preserving the old behavior")
    void testTickWithoutListenersCompletes() {
        // Should not throw; the event is simply never allocated.
        Assertions.assertDoesNotThrow(() -> Slimefun.getTickerTask().run());
    }
}

package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the grappling hook API expansion: {@link GrapplingHookPullEvent}.
 *
 * @author Zurker
 */
class TestGrapplingHookPullEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = server.addSimpleWorld("grappling");
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
    @DisplayName("GrapplingHookPullEvent exposes arrow, target and cancellation")
    void testEventFieldsAndCancellation() {
        Player player = server.addPlayer();
        Arrow arrow = Mockito.mock(Arrow.class);
        Location target = new Location(world, 10, 70, 10);

        GrapplingHookPullEvent event = new GrapplingHookPullEvent(player, arrow, target);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(arrow, event.getArrow());
        Assertions.assertEquals(target, event.getTarget());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GrapplingHookPullEvent(player, null, target));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GrapplingHookPullEvent(player, arrow, null));
    }

    @Test
    @DisplayName("GrapplingHookPullEvent is dispatchable to listeners")
    void testEventDispatch() {
        Player player = server.addPlayer();
        Arrow arrow = Mockito.mock(Arrow.class);
        Location target = new Location(world, 5, 65, 5);

        PullListener listener = new PullListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            GrapplingHookPullEvent event = new GrapplingHookPullEvent(player, arrow, target);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(listener.seen);
            Assertions.assertEquals(target, listener.seenTarget);
            Assertions.assertTrue(event.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    private static class PullListener implements Listener {
        boolean seen;
        Location seenTarget;

        @EventHandler
        public void onPull(GrapplingHookPullEvent event) {
            seen = true;
            seenTarget = event.getTarget();
            event.setCancelled(true);
        }
    }
}

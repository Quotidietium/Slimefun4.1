package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.RadiationResetEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * Regression coverage for the radiation API expansion: {@link RadiationResetEvent},
 * exercised by driving the real {@link RadioactivityListener#onPlayerDeath} death
 * handling path with a stubbed {@link PlayerDeathEvent}.
 * <p>
 * The exposure lives in the Player's persistent data container, which is fully
 * functional under MockBukkit, so the reset is asserted end-to-end through
 * {@link RadiationUtils#getExposure(Player)}. The death event is a Mockito stub
 * because only its entity is ever read.
 *
 * @author Zurker
 */
class TestRadiationResetEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static RadioactivityListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        listener = new RadioactivityListener(plugin);
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
     * A death event stub: the listener only ever reads the entity.
     */
    private PlayerDeathEvent deathEventOf(Player player) {
        PlayerDeathEvent deathEvent = Mockito.mock(PlayerDeathEvent.class);
        Mockito.when(deathEvent.getEntity()).thenReturn(player);
        return deathEvent;
    }

    @Test
    @DisplayName("RadiationResetEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        PlayerDeathEvent deathEvent = deathEventOf(player);

        RadiationResetEvent event = new RadiationResetEvent(player, 50, deathEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(50, event.getExposureBefore());
        Assertions.assertSame(deathEvent, event.getDeathEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationResetEvent(player, 0, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationResetEvent(player, -1, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationResetEvent(player, 50, null));
    }

    @Test
    @DisplayName("Dying with exposure fires the event and clears the exposure")
    void testDeathWithExposureFiresAndClears() {
        Player player = server.addPlayer();
        RadiationUtils.addExposure(player, 50);
        PlayerDeathEvent deathEvent = deathEventOf(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRadiationReset(RadiationResetEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(50, event.getExposureBefore());
                Assertions.assertSame(deathEvent, event.getDeathEvent());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onPlayerDeath(deathEvent);

            Assertions.assertTrue(seen[0], "RadiationResetEvent was not fired");
            Assertions.assertEquals(0, RadiationUtils.getExposure(player), "The exposure must have been cleared on death");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling RadiationResetEvent keeps the exposure through death")
    void testCancelKeepsExposure() {
        Player player = server.addPlayer();
        RadiationUtils.addExposure(player, 50);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRadiationReset(RadiationResetEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            listener.onPlayerDeath(deathEventOf(player));

            Assertions.assertEquals(50, RadiationUtils.getExposure(player), "A vetoed reset must keep the exposure");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Dying without listeners still clears the exposure, preserving the old behavior")
    void testDeathWithoutListenersClears() {
        Player player = server.addPlayer();
        RadiationUtils.addExposure(player, 50);

        listener.onPlayerDeath(deathEventOf(player));

        Assertions.assertEquals(0, RadiationUtils.getExposure(player), "The exposure must have been cleared on death");
    }

    @Test
    @DisplayName("Dying at zero exposure fires no event")
    void testDeathAtZeroExposureFiresNothing() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRadiationReset(RadiationResetEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onPlayerDeath(deathEventOf(player));

            Assertions.assertFalse(seen[0], "No event must be fired when there is nothing to reset");
            Assertions.assertEquals(0, RadiationUtils.getExposure(player));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

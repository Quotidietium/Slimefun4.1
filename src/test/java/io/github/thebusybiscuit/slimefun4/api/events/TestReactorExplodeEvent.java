package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Location;
import org.bukkit.World;
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

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;

/**
 * Regression coverage for {@link ReactorExplodeEvent} (API contract: fields, null-validation,
 * non-cancellable, listener dispatch). Previously untested despite being audited in r17/r62.
 *
 * @author Zurker
 */
class TestReactorExplodeEvent {

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
    @DisplayName("ReactorExplodeEvent exposes location/reactor, rejects nulls and is not cancellable")
    void testContract() {
        World world = Mockito.mock(World.class);
        Location location = new Location(world, 1, 64, -2);
        Reactor reactor = Mockito.mock(Reactor.class);

        ReactorExplodeEvent event = new ReactorExplodeEvent(location, reactor);

        Assertions.assertEquals(location, event.getLocation());
        Assertions.assertEquals(reactor, event.getReactor());
        Assertions.assertFalse(event instanceof org.bukkit.event.Cancellable, "ReactorExplodeEvent is informational (the explosion already happened)");
        // On the main unit-test thread the adaptive async flag reports false.
        Assertions.assertFalse(event.isAsynchronous());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorExplodeEvent(null, reactor));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorExplodeEvent(location, null));
    }

    @Test
    @DisplayName("ReactorExplodeEvent is dispatchable to listeners")
    void testDispatch() {
        World world = Mockito.mock(World.class);
        Reactor reactor = Mockito.mock(Reactor.class);
        ReactorExplodeEvent event = new ReactorExplodeEvent(new Location(world, 0, 0, 0), reactor);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onExplode(ReactorExplodeEvent e) {
                seen[0] = true;
                Assertions.assertEquals(reactor, e.getReactor());
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            server.getPluginManager().callEvent(event);
            Assertions.assertTrue(seen[0]);
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}

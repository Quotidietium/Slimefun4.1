package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.meta.FireworkMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.CowMock;
import be.seeseemelk.mockbukkit.entity.FireworkMock;

import io.github.thebusybiscuit.slimefun4.api.events.ResearchFireworkDamageEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the research firework API expansion:
 * {@link ResearchFireworkDamageEvent}, exercised through the real {@link FireworksListener}
 * damage-nullification path.
 *
 * @author Zurker
 */
class TestResearchFireworkDamageEvent {

    private static final String RESEARCH_NAME = ChatColor.GREEN + "Slimefun Research";

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Unit test startups do not register the listeners, register it manually
        new FireworksListener(plugin);
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
     * Creates a firework whose meta carries the given display name, the marker the
     * listener recognizes research fireworks by.
     */
    private Firework createFirework(String displayName) {
        FireworkMeta meta = (FireworkMeta) Bukkit.getItemFactory().getItemMeta(Material.FIREWORK_ROCKET);
        meta.setDisplayName(displayName);
        return new FireworkMock(server, UUID.randomUUID(), meta);
    }

    /**
     * Lets the firework explode onto the victim for five damage through the real event
     * pipeline and returns the damage event for assertions.
     */
    private EntityDamageByEntityEvent detonate(Firework firework, Entity victim) {
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(firework, victim, DamageCause.ENTITY_EXPLOSION, 5.0);
        server.getPluginManager().callEvent(damageEvent);
        return damageEvent;
    }

    @Test
    @DisplayName("ResearchFireworkDamageEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Firework firework = createFirework(RESEARCH_NAME);
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(firework, player, DamageCause.ENTITY_EXPLOSION, 5.0);

        ResearchFireworkDamageEvent event = new ResearchFireworkDamageEvent(firework, player, damageEvent);

        Assertions.assertEquals(firework, event.getFirework());
        Assertions.assertEquals(player, event.getVictim());
        Assertions.assertEquals(damageEvent, event.getDamageEvent());
        Assertions.assertEquals(5.0, event.getDamage());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchFireworkDamageEvent(null, player, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchFireworkDamageEvent(firework, null, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchFireworkDamageEvent(firework, player, null));
    }

    @Test
    @DisplayName("A research firework fires the event and its damage is nullified")
    void testResearchFireworkFiresAndNullifies() {
        Player player = server.addPlayer();
        Firework firework = createFirework(RESEARCH_NAME);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDamage(ResearchFireworkDamageEvent event) {
                seen[0] = true;
                Assertions.assertEquals(firework, event.getFirework());
                Assertions.assertEquals(player, event.getVictim());
                Assertions.assertEquals(5.0, event.getDamage());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = detonate(firework, player);

            Assertions.assertTrue(seen[0], "ResearchFireworkDamageEvent was not fired");
            Assertions.assertTrue(damageEvent.isCancelled(), "The research firework's damage must have been nullified");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ResearchFireworkDamageEvent lets the damage through")
    void testEventCancellationLetsDamageThrough() {
        Player player = server.addPlayer();
        Firework firework = createFirework(RESEARCH_NAME);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onDamage(ResearchFireworkDamageEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = detonate(firework, player);

            Assertions.assertFalse(damageEvent.isCancelled(), "A cancelled nullification must let the damage through");
            Assertions.assertEquals(5.0, damageEvent.getDamage());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Nullification without listeners still applies, preserving the old behavior")
    void testResearchFireworkWithoutListenersStillNullifies() {
        Player player = server.addPlayer();
        Firework firework = createFirework(RESEARCH_NAME);

        EntityDamageByEntityEvent damageEvent = detonate(firework, player);

        Assertions.assertTrue(damageEvent.isCancelled(), "The research firework's damage must have been nullified");
    }

    @Test
    @DisplayName("A research firework damaging a non-player entity fires the event too")
    void testResearchFireworkProtectsNonPlayerVictim() {
        CowMock cow = new CowMock(server, UUID.randomUUID());
        Firework firework = createFirework(RESEARCH_NAME);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDamage(ResearchFireworkDamageEvent event) {
                seen[0] = true;
                Assertions.assertEquals(cow, event.getVictim());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = detonate(firework, cow);

            Assertions.assertTrue(seen[0], "ResearchFireworkDamageEvent was not fired");
            Assertions.assertTrue(damageEvent.isCancelled(), "The research firework's damage must have been nullified");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A firework without a display name fires no event and damages normally")
    void testNamelessFireworkFiresNothing() {
        Player player = server.addPlayer();
        FireworkMeta meta = (FireworkMeta) Bukkit.getItemFactory().getItemMeta(Material.FIREWORK_ROCKET);
        Firework firework = new FireworkMock(server, UUID.randomUUID(), meta);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDamage(ResearchFireworkDamageEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = detonate(firework, player);

            Assertions.assertFalse(seen[0], "No event must be fired for a regular firework");
            Assertions.assertFalse(damageEvent.isCancelled(), "A regular firework must damage normally");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A firework with a different display name fires no event and damages normally")
    void testOtherFireworkFiresNothing() {
        Player player = server.addPlayer();
        Firework firework = createFirework(ChatColor.RED + "Some other firework");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDamage(ResearchFireworkDamageEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = detonate(firework, player);

            Assertions.assertFalse(seen[0], "No event must be fired for a differently named firework");
            Assertions.assertFalse(damageEvent.isCancelled(), "A differently named firework must damage normally");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

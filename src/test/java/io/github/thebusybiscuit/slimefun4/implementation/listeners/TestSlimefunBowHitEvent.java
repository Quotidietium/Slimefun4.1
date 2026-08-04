package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBowHitEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SlimefunBow;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the bow API expansion: {@link SlimefunBowHitEvent}, exercised
 * through the real {@link SlimefunBowListener} shoot-and-hit dispatch path.
 *
 * @author Zurker
 */
class TestSlimefunBowHitEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static SlimefunBowListener bowListener;

    private static SlimefunBow bow;

    private static final AtomicBoolean hitHandlerCalled = new AtomicBoolean(false);

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        bowListener = new SlimefunBowListener();
        bowListener.register(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "bow_hit_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_HIT_BOW", Material.BOW, "&7Test Hit Bow");

        bow = new SlimefunBow(itemGroup, stack, new ItemStack[9]) {
            @Nonnull
            @Override
            public BowShootHandler onShoot() {
                return (e, n) -> hitHandlerCalled.set(true);
            }
        };
        bow.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
        hitHandlerCalled.set(false);
        bowListener.getProjectileData().clear();
    }

    /**
     * Creates a tracked-arrow stand-in. MockBukkit's {@code ArrowMock} implements neither
     * {@code getShooter()} nor {@code setShooter(...)}, so a Mockito mock is used instead.
     */
    private Arrow mockArrow(PlayerMock shooter) {
        Arrow arrow = Mockito.mock(Arrow.class);
        Mockito.when(arrow.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(arrow.getShooter()).thenReturn(shooter);
        return arrow;
    }

    /**
     * Fires a tracked arrow from the test bow at the given target, running the real
     * shoot-and-hit path of the {@link SlimefunBowListener}.
     */
    private EntityDamageByEntityEvent shootAndHit(PlayerMock shooter, PlayerMock target) {
        ItemStack bowItem = bow.getItem().clone();
        Arrow arrow = mockArrow(shooter);

        EntityShootBowEvent shootEvent = new EntityShootBowEvent(shooter, bowItem, new ItemStack(Material.ARROW), arrow, EquipmentSlot.HAND, 1.0F, false);
        server.getPluginManager().callEvent(shootEvent);

        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(arrow, target, EntityDamageEvent.DamageCause.PROJECTILE, 6.0);
        server.getPluginManager().callEvent(damageEvent);

        return damageEvent;
    }

    @Test
    @DisplayName("SlimefunBowHitEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        PlayerMock shooter = server.addPlayer();
        PlayerMock target = server.addPlayer();
        Arrow arrow = mockArrow(shooter);
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(arrow, target, EntityDamageEvent.DamageCause.PROJECTILE, 6.0);

        SlimefunBowHitEvent event = new SlimefunBowHitEvent(shooter, bow, arrow, target, damageEvent);

        Assertions.assertEquals(shooter, event.getPlayer());
        Assertions.assertEquals(bow, event.getBow());
        Assertions.assertEquals(arrow, event.getArrow());
        Assertions.assertEquals(target, event.getTarget());
        Assertions.assertEquals(damageEvent, event.getEntityDamageByEntityEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBowHitEvent(shooter, null, arrow, target, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBowHitEvent(shooter, bow, null, target, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBowHitEvent(shooter, bow, arrow, null, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBowHitEvent(shooter, bow, arrow, target, null));
    }

    @Test
    @DisplayName("A tracked arrow hit fires SlimefunBowHitEvent and runs the BowShootHandler")
    void testHitFiresEventAndRunsHandler() {
        PlayerMock shooter = server.addPlayer();
        PlayerMock target = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBowHit(SlimefunBowHitEvent event) {
                seen[0] = true;
                Assertions.assertEquals(shooter, event.getPlayer());
                Assertions.assertEquals(bow, event.getBow());
                Assertions.assertEquals(target, event.getTarget());
                Assertions.assertNotNull(event.getArrow());
                Assertions.assertNotNull(event.getEntityDamageByEntityEvent());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = shootAndHit(shooter, target);

            Assertions.assertTrue(seen[0], "SlimefunBowHitEvent was not fired");
            Assertions.assertTrue(hitHandlerCalled.get(), "The BowShootHandler was not called");
            Assertions.assertFalse(damageEvent.isCancelled(), "The underlying damage event must stay untouched");
            Assertions.assertTrue(bowListener.getProjectileData().isEmpty(), "The arrow must be untracked after the hit");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBowHitEvent skips the BowShootHandler without cancelling the damage")
    void testHitCancellationSkipsHandler() {
        PlayerMock shooter = server.addPlayer();
        PlayerMock target = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBowHit(SlimefunBowHitEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = shootAndHit(shooter, target);

            Assertions.assertFalse(hitHandlerCalled.get(), "A cancelled bow hit must not call the BowShootHandler");
            Assertions.assertFalse(damageEvent.isCancelled(), "A cancelled bow hit must not cancel the underlying damage");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("An untracked arrow hit fires no event and runs no handler")
    void testUntrackedArrowFiresNothing() {
        PlayerMock target = server.addPlayer();
        Arrow arrow = mockArrow(null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBowHit(SlimefunBowHitEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(arrow, target, EntityDamageEvent.DamageCause.PROJECTILE, 6.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for an untracked arrow");
            Assertions.assertFalse(hitHandlerCalled.get(), "No handler must run for an untracked arrow");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A cancelled damage event fires no SlimefunBowHitEvent")
    void testCancelledDamageEventFiresNothing() {
        PlayerMock shooter = server.addPlayer();
        PlayerMock target = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBowHit(SlimefunBowHitEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack bowItem = bow.getItem().clone();
            Arrow arrow = mockArrow(shooter);
            server.getPluginManager().callEvent(new EntityShootBowEvent(shooter, bowItem, new ItemStack(Material.ARROW), arrow, EquipmentSlot.HAND, 1.0F, false));

            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(arrow, target, EntityDamageEvent.DamageCause.PROJECTILE, 6.0);
            damageEvent.setCancelled(true);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired when the damage event is cancelled");
            Assertions.assertFalse(hitHandlerCalled.get(), "No handler must run when the damage event is cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

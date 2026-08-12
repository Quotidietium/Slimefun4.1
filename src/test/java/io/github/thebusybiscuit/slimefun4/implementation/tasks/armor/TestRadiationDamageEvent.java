package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.RadiationDamageEvent;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * Regression coverage for the radiation API expansion: {@link RadiationDamageEvent},
 * exercised by driving a real {@link RadiationTask} player tick against players with a
 * stored exposure level. The task's damage stage runs synchronously in unit tests, so
 * the applied {@link RadiationDamageEvent#getExposure() effective exposure} is directly
 * observable through the player's potion effects.
 *
 * @author Zurker
 */
class TestRadiationDamageEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static RadiationTask task;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        task = new RadiationTask();
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
     * Loads the {@link PlayerProfile} for the given {@link Player}, waiting for the
     * asynchronous load to finish.
     */
    private static PlayerProfile profileOf(Player p) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PlayerProfile> ref = new AtomicReference<>();
        PlayerProfile.get(p, profile -> {
            ref.set(profile);
            latch.countDown();
        });
        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "The PlayerProfile did not load in time");
        return ref.get();
    }

    /**
     * Runs one player tick of a real {@link RadiationTask}. The task lives in the
     * same package, so the protected tick method is directly reachable here.
     */
    private void tick(Player p, PlayerProfile profile) {
        task.onPlayerTick(p, profile);
    }

    @Test
    @DisplayName("RadiationDamageEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player p = server.addPlayer();

        RadiationDamageEvent event = new RadiationDamageEvent(p, 30);

        Assertions.assertEquals(p, event.getPlayer());
        Assertions.assertEquals(30, event.getExposure());
        Assertions.assertFalse(event.isCancelled());

        event.setExposure(0);
        Assertions.assertEquals(0, event.getExposure());

        event.setExposure(100);
        Assertions.assertEquals(100, event.getExposure());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationDamageEvent(null, 30));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationDamageEvent(p, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setExposure(-1));
    }

    @Test
    @DisplayName("A player with 30 stored exposure receives the matching symptoms")
    void testSymptomsAppliedWithoutListeners() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        // No radioactive items: the level decays by one to 29 before the damage stage
        RadiationUtils.addExposure(p, 30);

        tick(p, profile);

        Assertions.assertEquals(29, RadiationUtils.getExposure(p));
        Assertions.assertNotNull(p.getPotionEffect(PotionEffectType.SLOWNESS), "SLOW must apply at 29 exposure");
        Assertions.assertNotNull(p.getPotionEffect(PotionEffectType.WITHER), "WITHER_LOW must apply at 29 exposure");
        Assertions.assertNull(p.getPotionEffect(PotionEffectType.BLINDNESS), "BLINDNESS must not apply below 50 exposure");
    }

    @Test
    @DisplayName("Scaling the effective exposure down spares the player from symptoms")
    void testExposureScaledDownSkipsSymptoms() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        RadiationUtils.addExposure(p, 30);

        Listener scaling = new Listener() {
            @EventHandler
            public void onDamage(RadiationDamageEvent event) {
                event.setExposure(0);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            tick(p, profile);

            Assertions.assertNull(p.getPotionEffect(PotionEffectType.SLOWNESS), "No symptoms must apply at an effective exposure of 0");
            Assertions.assertNull(p.getPotionEffect(PotionEffectType.WITHER));
            Assertions.assertEquals(29, RadiationUtils.getExposure(p), "The stored exposure must be untouched");
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("Scaling the effective exposure up applies harsher symptoms")
    void testExposureScaledUpAppliesHarsherSymptoms() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        RadiationUtils.addExposure(p, 5);

        Listener scaling = new Listener() {
            @EventHandler
            public void onDamage(RadiationDamageEvent event) {
                event.setExposure(50);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            tick(p, profile);

            Assertions.assertNotNull(p.getPotionEffect(PotionEffectType.BLINDNESS), "BLINDNESS must apply at an effective exposure of 50");
            Assertions.assertEquals(4, RadiationUtils.getExposure(p), "The stored exposure must be untouched");
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("Cancelling RadiationDamageEvent skips all symptoms")
    void testCancellationSkipsSymptoms() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        RadiationUtils.addExposure(p, 30);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onDamage(RadiationDamageEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(p, profile);

            Assertions.assertNull(p.getPotionEffect(PotionEffectType.SLOWNESS), "A cancelled event must not apply symptoms");
            Assertions.assertNull(p.getPotionEffect(PotionEffectType.WITHER));
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }
}

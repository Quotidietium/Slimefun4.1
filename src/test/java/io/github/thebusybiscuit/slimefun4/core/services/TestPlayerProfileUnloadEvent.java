package io.github.thebusybiscuit.slimefun4.core.services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

import io.github.thebusybiscuit.slimefun4.api.events.PlayerProfileUnloadEvent;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the profile lifecycle API expansion:
 * {@link PlayerProfileUnloadEvent}, exercised by creating a profile for an offline
 * player, marking it for deletion, then calling the private
 * {@code AutoSavingService.saveAllPlayers()} via reflection.
 *
 * @author Zurker
 */
class TestPlayerProfileUnloadEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static AutoSavingService service;

    @BeforeAll
    public static void load() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        Field field = Slimefun.class.getDeclaredField("autoSavingService");
        field.setAccessible(true);
        service = (AutoSavingService) field.get(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private void runSaveAllPlayers() throws Exception {
        Method method = AutoSavingService.class.getDeclaredMethod("saveAllPlayers");
        method.setAccessible(true);
        method.invoke(service);
    }

    /**
     * Creates a profile for an offline player (never on this server) and marks it for
     * deletion. The next saveAllPlayers cycle will fire the unload event.
     */
    private OfflinePlayer createMarkedProfile() throws InterruptedException {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.randomUUID());
        CountDownLatch latch = new CountDownLatch(1);

        PlayerProfile.get(offline, profile -> {
            profile.markForDeletion();
            latch.countDown();
        });

        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "Profile creation timed out");
        return offline;
    }

    @Test
    @DisplayName("PlayerProfileUnloadEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() throws InterruptedException {
        OfflinePlayer offline = createMarkedProfile();
        PlayerProfile profile = PlayerProfile.find(offline).orElseThrow();

        PlayerProfileUnloadEvent event = new PlayerProfileUnloadEvent(profile);

        Assertions.assertEquals(profile, event.getProfile());
        Assertions.assertEquals(offline.getUniqueId(), event.getUUID());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PlayerProfileUnloadEvent(null));
    }

    @Test
    @DisplayName("A marked-for-deletion offline profile fires the unload event on save")
    void testUnloadFiresEvent() throws Exception {
        OfflinePlayer offline = createMarkedProfile();

        UUID[] captured = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUnload(PlayerProfileUnloadEvent event) {
                captured[0] = event.getUUID();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runSaveAllPlayers();

            Assertions.assertEquals(offline.getUniqueId(), captured[0], "PlayerProfileUnloadEvent was not fired for the marked profile");
            Assertions.assertFalse(PlayerProfile.find(offline).isPresent(), "The profile must have been removed from memory");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A profile for an online player does not fire the unload event")
    void testOnlinePlayerNotUnloaded() throws Exception {
        // Online players' profiles must never be unloaded even if marked for deletion,
        // because getPlayer() returns a non-null value.
        var player = server.addPlayer();
        OfflinePlayer offline = player;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PlayerProfile> ref = new AtomicReference<>();
        PlayerProfile.get(offline, profile -> {
            profile.markForDeletion();
            ref.set(profile);
            latch.countDown();
        });
        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS));
        Assertions.assertNotNull(ref.get().getPlayer(), "An online player's profile must have a non-null getPlayer()");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUnload(PlayerProfileUnloadEvent event) {
                if (event.getUUID().equals(offline.getUniqueId())) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runSaveAllPlayers();

            Assertions.assertFalse(seen[0], "An online player's profile must not fire the unload event");
            Assertions.assertTrue(PlayerProfile.find(offline).isPresent(), "An online player's profile must stay in memory");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Without listeners the save still completes, preserving the old behavior")
    void testSaveWithoutListenersCompletes() throws Exception {
        OfflinePlayer offline = createMarkedProfile();

        Assertions.assertDoesNotThrow(() -> runSaveAllPlayers());

        Assertions.assertFalse(PlayerProfile.find(offline).isPresent(), "The profile must have been removed even without listeners");
    }
}

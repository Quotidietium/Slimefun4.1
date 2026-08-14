package io.github.thebusybiscuit.slimefun4.api.events;

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

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.core.services.localization.Language;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the player language API expansion:
 * {@link PlayerLanguageChangeEvent} becoming vetoable and redirectable.
 * <p>
 * The applying path lives inside the guide's language {@code ChestMenu} click handlers,
 * which is too heavy to drive through a GUI click simulation here (the same trade-off as
 * the elevator tests), so this class pins the event contract: fields, the redirect via
 * {@link PlayerLanguageChangeEvent#setNewLanguage(Language)} and the veto.
 *
 * @author Zurker
 */
class TestPlayerLanguageChangeEvent {

    // The texture hash the LocalizationService itself uses for the default language
    private static final String TEXTURE = "11b3188fd44902f72602bd7c2141f5a70673a411adb3d81862c69e536166b";

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
    @DisplayName("PlayerLanguageChangeEvent exposes its fields and starts uncancelled")
    void testEventFields() {
        Player player = server.addPlayer();
        // A non-null Language instance: in the unit-test environment getDefaultLanguage()
        // returns null (languages are not loaded), while the production firing point
        // (PlayerLanguageOption) always passes the player's loaded, non-null language.
        Language from = new Language("en", TEXTURE);
        Language to = new Language("de", TEXTURE);

        PlayerLanguageChangeEvent event = new PlayerLanguageChangeEvent(player, from, to);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(from, event.getPreviousLanguage());
        Assertions.assertEquals(to, event.getNewLanguage());
        Assertions.assertFalse(event.isCancelled(), "The event must start uncancelled");
    }

    @Test
    @DisplayName("setNewLanguage redirects the pending language change")
    void testSetNewLanguageRedirects() {
        Player player = server.addPlayer();
        // A non-null Language instance: in the unit-test environment getDefaultLanguage()
        // returns null (languages are not loaded), while the production firing point
        // (PlayerLanguageOption) always passes the player's loaded, non-null language.
        Language from = new Language("en", TEXTURE);
        Language picked = new Language("de", TEXTURE);
        Language forced = new Language("fr", TEXTURE);

        PlayerLanguageChangeEvent event = new PlayerLanguageChangeEvent(player, from, picked);
        Assertions.assertEquals(picked, event.getNewLanguage());

        event.setNewLanguage(forced);
        Assertions.assertEquals(forced, event.getNewLanguage(), "The redirect must replace the picked language");
        Assertions.assertEquals(from, event.getPreviousLanguage(), "The previous language must be untouched by a redirect");

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNewLanguage(null));
    }

    @Test
    @DisplayName("PlayerLanguageChangeEvent is cancellable")
    void testCancellation() {
        Player player = server.addPlayer();
        // A non-null Language instance: in the unit-test environment getDefaultLanguage()
        // returns null (languages are not loaded), while the production firing point
        // (PlayerLanguageOption) always passes the player's loaded, non-null language.
        Language from = new Language("en", TEXTURE);
        Language to = new Language("de", TEXTURE);

        PlayerLanguageChangeEvent event = new PlayerLanguageChangeEvent(player, from, to);
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        event.setCancelled(false);
        Assertions.assertFalse(event.isCancelled());
    }

    @Test
    @DisplayName("A registered listener sees the dispatched event")
    void testDispatchReachesListeners() {
        Player player = server.addPlayer();
        // A non-null Language instance: in the unit-test environment getDefaultLanguage()
        // returns null (languages are not loaded), while the production firing point
        // (PlayerLanguageOption) always passes the player's loaded, non-null language.
        Language from = new Language("en", TEXTURE);
        Language to = new Language("de", TEXTURE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onLanguageChange(PlayerLanguageChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(to, event.getNewLanguage());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            server.getPluginManager().callEvent(new PlayerLanguageChangeEvent(player, from, to));

            Assertions.assertTrue(seen[0], "The listener did not receive the dispatched event");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

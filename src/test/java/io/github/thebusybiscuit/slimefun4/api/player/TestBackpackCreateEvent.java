package io.github.thebusybiscuit.slimefun4.api.player;

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

import io.github.thebusybiscuit.slimefun4.api.events.BackpackCreateEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the backpack API expansion: {@link BackpackCreateEvent},
 * exercised by driving the real {@link PlayerProfile#createBackpack(int)} creation path.
 * <p>
 * A listener may adjust the initial size of the newly created backpack (e.g. a larger
 * backpack for certain players) without touching the item's configured size. The event
 * is deliberately not cancellable, because the creating caller expects a backpack to
 * come into existence - opening is vetoed through {@code PlayerBackpackOpenEvent}.
 *
 * @author Zurker
 */
class TestBackpackCreateEvent {

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

    private PlayerProfile newProfile() throws InterruptedException {
        Player player = server.addPlayer();
        return TestUtilities.awaitProfile(player);
    }

    @Test
    @DisplayName("BackpackCreateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() throws InterruptedException {
        PlayerProfile profile = newProfile();

        BackpackCreateEvent event = new BackpackCreateEvent(profile, 3, 18);

        Assertions.assertEquals(profile, event.getProfile());
        Assertions.assertEquals(3, event.getId());
        Assertions.assertEquals(18, event.getSize());

        event.setSize(54);
        Assertions.assertEquals(54, event.getSize());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSize(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSize(8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSize(12));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSize(55));

        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackCreateEvent(null, 0, 9));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackCreateEvent(profile, -1, 9));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackCreateEvent(profile, 0, 12));
    }

    @Test
    @DisplayName("Creating a backpack fires the event with the assigned id and requested size")
    void testCreateFiresEvent() throws InterruptedException {
        PlayerProfile profile = newProfile();
        profile.createBackpack(9);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCreate(BackpackCreateEvent event) {
                seen[0] = true;
                Assertions.assertEquals(profile, event.getProfile());
                Assertions.assertEquals(1, event.getId(), "The event must carry the id that will be assigned");
                Assertions.assertEquals(27, event.getSize(), "The event must default to the requested size");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerBackpack backpack = profile.createBackpack(27);

            Assertions.assertTrue(seen[0], "BackpackCreateEvent was not fired");
            Assertions.assertEquals(1, backpack.getId());
            Assertions.assertEquals(27, backpack.getSize());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A modified size is applied to the newly created backpack")
    void testModifiedSizeApplied() throws InterruptedException {
        PlayerProfile profile = newProfile();

        Listener enlarging = new Listener() {
            @EventHandler
            public void onCreate(BackpackCreateEvent event) {
                event.setSize(54);
            }
        };
        server.getPluginManager().registerEvents(enlarging, plugin);

        try {
            PlayerBackpack backpack = profile.createBackpack(9);

            Assertions.assertEquals(54, backpack.getSize(), "The modified size must have been applied");
            Assertions.assertEquals(54, backpack.getInventory().getSize());
        } finally {
            HandlerList.unregisterAll(enlarging);
        }
    }

    @Test
    @DisplayName("An untouched event keeps the requested size, preserving the old behavior")
    void testUntouchedEventKeepsRequestedSize() throws InterruptedException {
        PlayerProfile profile = newProfile();

        Listener watcher = new Listener() {
            @EventHandler
            public void onCreate(BackpackCreateEvent event) {
                // Only observe, do not touch the size
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerBackpack backpack = profile.createBackpack(18);

            Assertions.assertEquals(18, backpack.getSize(), "An untouched event must reproduce the requested size");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Creating without listeners works as before")
    void testCreateWithoutListeners() throws InterruptedException {
        PlayerProfile profile = newProfile();

        PlayerBackpack backpack = profile.createBackpack(36);

        Assertions.assertEquals(36, backpack.getSize());
        Assertions.assertEquals(0, backpack.getId());
    }

    @Test
    @DisplayName("An out-of-range requested size still throws with listeners registered")
    void testInvalidSizeStillThrows() throws InterruptedException {
        PlayerProfile profile = newProfile();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCreate(BackpackCreateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertThrows(IllegalArgumentException.class, () -> profile.createBackpack(12));
            Assertions.assertThrows(IllegalArgumentException.class, () -> profile.createBackpack(-9));
            Assertions.assertFalse(seen[0], "No event must be fired for an invalid size");
            Assertions.assertFalse(profile.getBackpack(0).isPresent(), "No backpack must have been created");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

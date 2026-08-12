package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.RadiationExposureEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * Regression coverage for the radiation API expansion: {@link RadiationExposureEvent},
 * exercised by driving a real {@link RadiationTask} player tick against players
 * whose exposure is about to change.
 * <p>
 * An accumulation is fully observable: a player carrying a {@link Radioactive} item
 * sees the event with a positive change and their exposure level rises. A vetoed
 * accumulation keeps the level at zero. The same holds for the decay paths, both
 * while unprotected and while in creative mode, where a decay that would not
 * change anything (level already zero) stays silent.
 *
 * @author Zurker
 */
class TestRadiationExposureEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static RadiationTask task;
    private static SlimefunItem radioactiveItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        task = new RadiationTask();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "radiation_exposure_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_RADIOACTIVE_CHUNK", Material.SLIME_BALL, "&aTest Radioactive Chunk");
        Slimefun.getItemCfg().setValue("_TEST_RADIOACTIVE_CHUNK.enabled", true);
        radioactiveItem = new TestRadioactiveItem(itemGroup, stack);
        radioactiveItem.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private static class TestRadioactiveItem extends SlimefunItem implements Radioactive {

        TestRadioactiveItem(ItemGroup itemGroup, SlimefunItemStack item) {
            super(itemGroup, item, RecipeType.NULL, new ItemStack[9]);
        }

        @Override
        @Nonnull
        public Radioactivity getRadioactivity() {
            // An exposure modifier of 3 per item and tick
            return Radioactivity.HIGH;
        }
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

    /**
     * Registers a listener capturing the next {@link RadiationExposureEvent} and
     * returns the capture. Unregister with {@link HandlerList#unregisterAll(Listener)}.
     */
    private Listener watch(AtomicReference<RadiationExposureEvent> capture) {
        Listener watcher = new Listener() {
            @EventHandler
            public void onExposure(RadiationExposureEvent event) {
                capture.set(event);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);
        return watcher;
    }

    @Test
    @DisplayName("RadiationExposureEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player p = server.addPlayer();

        RadiationExposureEvent event = new RadiationExposureEvent(p, 3, 4);

        Assertions.assertEquals(p, event.getPlayer());
        Assertions.assertEquals(3, event.getExposureBefore());
        Assertions.assertEquals(4, event.getExposureChange());
        Assertions.assertEquals(7, event.getExposureAfter());
        Assertions.assertFalse(event.isCancelled());
        Assertions.assertFalse(event.isAsynchronous(), "Constructed on the main thread, the event must be synchronous");

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        // The change can be scaled while keeping its sign, the after-value follows
        event.setExposureChange(10);
        Assertions.assertEquals(10, event.getExposureChange());
        Assertions.assertEquals(13, event.getExposureAfter());

        RadiationExposureEvent decay = new RadiationExposureEvent(p, 5, -1);
        decay.setExposureChange(-3);
        Assertions.assertEquals(-3, decay.getExposureChange());
        Assertions.assertEquals(2, decay.getExposureAfter());

        // Zero and sign flips are rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setExposureChange(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setExposureChange(-2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> decay.setExposureChange(2));

        // The after-value mirrors the clamping of RadiationUtils (0 to 100)
        Assertions.assertEquals(100, new RadiationExposureEvent(p, 99, 5).getExposureAfter());
        Assertions.assertEquals(0, new RadiationExposureEvent(p, 1, -1).getExposureAfter());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationExposureEvent(null, 3, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationExposureEvent(p, -1, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RadiationExposureEvent(p, 3, 0));
    }

    @Test
    @DisplayName("Carrying a radioactive item fires the event and raises the exposure level")
    void testAccumulationFiresEvent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        p.getInventory().setItem(0, radioactiveItem.getItem());

        AtomicReference<RadiationExposureEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);

            RadiationExposureEvent event = seen.get();
            Assertions.assertNotNull(event, "RadiationExposureEvent was not fired");
            Assertions.assertEquals(p, event.getPlayer());
            Assertions.assertEquals(0, event.getExposureBefore());
            Assertions.assertEquals(3, event.getExposureChange(), "One HIGH item must add 3 exposure per tick");
            Assertions.assertEquals(3, event.getExposureAfter());
            Assertions.assertEquals(3, RadiationUtils.getExposure(p), "The exposure must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling RadiationExposureEvent vetoes the accumulation")
    void testVetoAccumulationKeepsExposureAtZero() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        p.getInventory().setItem(0, radioactiveItem.getItem());

        AtomicReference<RadiationExposureEvent> seen = new AtomicReference<>();
        Listener cancelling = new Listener() {
            @EventHandler
            public void onExposure(RadiationExposureEvent event) {
                seen.set(event);
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(p, profile);

            Assertions.assertNotNull(seen.get(), "RadiationExposureEvent was not fired");
            Assertions.assertEquals(0, RadiationUtils.getExposure(p), "A vetoed accumulation must keep the exposure at zero");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Accumulating without listeners still raises the exposure, preserving the old behavior")
    void testAccumulationWithoutListenersApplies() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        p.getInventory().setItem(0, radioactiveItem.getItem());

        tick(p, profile);

        Assertions.assertEquals(3, RadiationUtils.getExposure(p), "The exposure must have been applied");
    }

    @Test
    @DisplayName("An unprotected player without radioactive items sees their exposure decay")
    void testDecayFiresEvent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        RadiationUtils.addExposure(p, 5);

        AtomicReference<RadiationExposureEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);

            RadiationExposureEvent event = seen.get();
            Assertions.assertNotNull(event, "RadiationExposureEvent was not fired");
            Assertions.assertEquals(p, event.getPlayer());
            Assertions.assertEquals(5, event.getExposureBefore());
            Assertions.assertEquals(-1, event.getExposureChange(), "The decay must be a change of -1");
            Assertions.assertEquals(4, event.getExposureAfter());
            Assertions.assertEquals(4, RadiationUtils.getExposure(p), "The decay must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling RadiationExposureEvent vetoes the decay")
    void testVetoDecayKeepsExposure() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        RadiationUtils.addExposure(p, 5);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onExposure(RadiationExposureEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(p, profile);

            Assertions.assertEquals(5, RadiationUtils.getExposure(p), "A vetoed decay must keep the exposure untouched");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A creative mode player's exposure decay fires the event")
    void testCreativeDecayFiresEvent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        p.setGameMode(GameMode.CREATIVE);
        RadiationUtils.addExposure(p, 3);

        AtomicReference<RadiationExposureEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);

            RadiationExposureEvent event = seen.get();
            Assertions.assertNotNull(event, "RadiationExposureEvent was not fired");
            Assertions.assertEquals(3, event.getExposureBefore());
            Assertions.assertEquals(-1, event.getExposureChange());
            Assertions.assertEquals(2, RadiationUtils.getExposure(p), "The decay must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Scaling the exposure change adjusts the applied accumulation")
    void testSetExposureChangeScalesAccumulation() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        p.getInventory().setItem(0, radioactiveItem.getItem());

        Listener scaling = new Listener() {
            @EventHandler
            public void onExposure(RadiationExposureEvent event) {
                Assertions.assertEquals(3, event.getExposureChange(), "One HIGH item must default to 3 exposure per tick");
                event.setExposureChange(1);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            tick(p, profile);

            Assertions.assertEquals(1, RadiationUtils.getExposure(p), "The scaled accumulation must have been applied");
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("Scaling the exposure change adjusts the applied decay")
    void testSetExposureChangeScalesDecay() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        RadiationUtils.addExposure(p, 5);

        Listener scaling = new Listener() {
            @EventHandler
            public void onExposure(RadiationExposureEvent event) {
                Assertions.assertEquals(-1, event.getExposureChange(), "The decay must default to -1");
                event.setExposureChange(-3);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            tick(p, profile);

            Assertions.assertEquals(2, RadiationUtils.getExposure(p), "The scaled decay must have been applied");
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("A decay that would not change anything fires no event")
    void testCreativeDecaySilentAtZero() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        p.setGameMode(GameMode.CREATIVE);

        AtomicReference<RadiationExposureEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);

            Assertions.assertNull(seen.get(), "No event must be fired when the exposure is already zero");
            Assertions.assertEquals(0, RadiationUtils.getExposure(p), "The exposure must stay at zero");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

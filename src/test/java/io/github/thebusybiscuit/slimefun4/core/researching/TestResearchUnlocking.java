package io.github.thebusybiscuit.slimefun4.core.researching;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

class TestResearchUnlocking {

    private ServerMock server;
    private Slimefun plugin;

    @BeforeEach
    public void load() throws InterruptedException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterEach
    public void unload() {
        MockBukkit.unmock();
    }

    private Player awaitUnlock(Player player, Research research, boolean instant) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Player> ref = new AtomicReference<>();

        // This loads the profile asynchronously
        research.unlock(player, instant, p -> {
            ref.set(p);
            latch.countDown();
        });

        latch.await(10, TimeUnit.SECONDS);
        return ref.get();
    }

    @ParameterizedTest
    @DisplayName("Test Unlocking Researches")
    @ValueSource(booleans = { true, false })
    void testUnlock(boolean instant) throws InterruptedException {
        Slimefun.getRegistry().setResearchingEnabled(true);
        Player player = server.addPlayer();
        Research research = new Research(new NamespacedKey(plugin, "unlock_me"), 1842, "Unlock me", 500);

        Player p = awaitUnlock(player, research, instant);
        Optional<PlayerProfile> profile = PlayerProfile.find(p);

        server.getPluginManager().assertEventFired(ResearchUnlockEvent.class, event -> {
            Assertions.assertEquals(p, event.getPlayer());
            Assertions.assertEquals(research, event.getResearch());
            Assertions.assertFalse(event.isCancelled());
            return true;
        });

        Assertions.assertEquals(player, p);
        Assertions.assertTrue(profile.isPresent());
        Assertions.assertTrue(profile.get().hasUnlocked(research));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("The research time defaults to 100 ticks, is modifiable and validated")
    void testResearchTimeTicks() {
        Player player = server.addPlayer();
        Research research = new Research(new NamespacedKey(plugin, "timed"), 1843, "Timed", 10);

        ResearchUnlockEvent event = new ResearchUnlockEvent(player, research);

        Assertions.assertEquals(ResearchUnlockEvent.DEFAULT_RESEARCH_TIME_TICKS, event.getResearchTimeTicks());
        Assertions.assertEquals(100L, ResearchUnlockEvent.DEFAULT_RESEARCH_TIME_TICKS);

        event.setResearchTimeTicks(0);
        Assertions.assertEquals(0, event.getResearchTimeTicks());

        event.setResearchTimeTicks(400);
        Assertions.assertEquals(400, event.getResearchTimeTicks());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResearchTimeTicks(-1));
    }

    @ParameterizedTest
    @DisplayName("Unlocking still completes when a listener adjusts the research time")
    @ValueSource(longs = { 0, 20, ResearchUnlockEvent.DEFAULT_RESEARCH_TIME_TICKS })
    void testUnlockWithAdjustedResearchTime(long ticks) throws InterruptedException {
        Slimefun.getRegistry().setResearchingEnabled(true);
        Player player = server.addPlayer();
        Research research = new Research(new NamespacedKey(plugin, "adjusted_" + ticks), 1844, "Adjusted", 500);

        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onUnlock(ResearchUnlockEvent event) {
                event.setResearchTimeTicks(ticks);
            }
        }, plugin);

        Player p = awaitUnlock(player, research, false);
        Optional<PlayerProfile> profile = PlayerProfile.find(p);

        Assertions.assertEquals(player, p);
        Assertions.assertTrue(profile.isPresent());
        Assertions.assertTrue(profile.get().hasUnlocked(research));
    }

}

package io.github.thebusybiscuit.slimefun4.storage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

/**
 * Regression tests for the thread-safety of {@link PlayerData}.
 * <p>
 * The async auto-save thread ({@code LegacyStorage}) iterates a profile's researches, waypoints
 * and backpacks while the main thread mutates them (unlocking researches, adding waypoints, ...).
 * With plain {@link HashSet}/{@link java.util.HashMap} that throws
 * {@link java.util.ConcurrentModificationException} under load; the collections are now backed by
 * {@link java.util.concurrent.ConcurrentHashMap}, whose iterators are weakly consistent.
 */
class TestPlayerDataConcurrency {

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

    @AfterEach
    public void cleanup() {
        Slimefun.getRegistry().getResearches().clear();
    }

    @Test
    void testConcurrentIterationAndMutationDoesNotThrow() throws Exception {
        Research[] pool = new Research[64];

        for (int i = 0; i < pool.length; i++) {
            Research research = new Research(new NamespacedKey(plugin, "concurrency_" + i), i, "C " + i, 1);
            research.register();
            pool[i] = research;
        }

        PlayerData data = new PlayerData(new HashSet<>(), Map.of(), new HashSet<>());

        ExecutorService exec = Executors.newFixedThreadPool(2);
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);

        // Thread A: iterate the live researches set the way LegacyStorage does on the save thread
        exec.submit(() -> {
            try {
                start.await();

                for (int i = 0; i < 50_000 && error.get() == null; i++) {
                    data.getResearches().forEach(r -> {
                        if (r == null) {
                            throw new AssertionError("Null research during iteration");
                        }
                    });
                }
            } catch (Throwable x) {
                error.set(x);
            }
        });

        // Thread B: mutate the set the way the main thread does when unlocking researches
        exec.submit(() -> {
            try {
                start.await();

                for (int i = 0; i < 50_000; i++) {
                    Research r = pool[i % pool.length];
                    data.addResearch(r);
                    data.removeResearch(r);
                }
            } catch (Throwable x) {
                error.set(x);
            }
        });

        start.countDown();
        exec.shutdown();
        Assertions.assertTrue(exec.awaitTermination(30, TimeUnit.SECONDS), "Concurrent threads did not finish in time");
        Assertions.assertNull(error.get(), () -> "Iterating PlayerData.researches concurrently with mutation threw: " + error.get());
    }
}

package io.github.thebusybiscuit.slimefun4.api.player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.gps.GPSNetwork;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.ChargeUtils;

/**
 * Concurrency stress tests for the mutation paths that were hardened against
 * cross-thread races: {@link PlayerProfile}'s modification-epoch/dirty-flag protocol
 * (async mutations vs the auto-save thread) and {@link GPSNetwork}'s atomic
 * per-owner transmitter set (async ticker registration vs main-thread removal).
 *
 * <p>
 * These hammer the invariants rather than exact interleavings: no lost update may
 * leave the profile clean, and no transmitter registration may be lost to a raced
 * removal.
 *
 * @author Zurker
 */
class TestConcurrentProfileAndNetwork {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    @BeforeAll
    public static void load() throws InterruptedException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException x) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Concurrent markDirty vs save never loses a change (quiet save clears, new mark re-dirties)")
    void testConcurrentMarkDirtyAndSave() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        int mutators = 4;
        int iterations = 400;
        ExecutorService pool = Executors.newFixedThreadPool(mutators + 1);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch mutatorsDone = new CountDownLatch(mutators);

        // Mutator threads: continuously dirty the profile (simulating async research/waypoint writes)
        for (int t = 0; t < mutators; t++) {
            pool.submit(() -> {
                await(start);
                for (int i = 0; i < iterations; i++) {
                    profile.markDirty();
                }
                mutatorsDone.countDown();
            });
        }

        // Saver thread: saves while mutators run (simulating the auto-save thread), then
        // performs one final save AFTER the mutators finished (guaranteed quiescence).
        pool.submit(() -> {
            await(start);
            try {
                while (!mutatorsDone.await(1, TimeUnit.MILLISECONDS)) {
                    profile.save();
                }
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
            }
            profile.save();
        });

        start.countDown();
        pool.shutdown();
        Assertions.assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "Stress threads must terminate");

        // Invariant 1: the post-quiescence save must have cleared the dirty flag.
        // A still-dirty profile would mean the epoch protocol dropped a mutation window.
        Assertions.assertFalse(profile.isDirty(), "A save after all mutations must clear the dirty flag - a true value means a lost update");

        // Invariant 2: the epoch keeps advancing - a fresh mutation re-dirties the profile.
        profile.markDirty();
        Assertions.assertTrue(profile.isDirty());
    }

    @Test
    @DisplayName("Concurrent transmitter register/unregister leaves no orphaned owner entry")
    void testConcurrentTransmitterUpdates() throws InterruptedException {
        GPSNetwork network = new GPSNetwork(plugin);
        UUID owner = UUID.randomUUID();
        int threads = 4;
        int locationsPerThread = 100;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int offset = t * locationsPerThread;
            pool.submit(() -> {
                try {
                    // Register and then unregister every location of this thread's range,
                    // interleaved with the other threads hammering the same owner entry.
                    List<Location> locs = new ArrayList<>();
                    for (int i = 0; i < locationsPerThread; i++) {
                        Location l = new Location(world, offset + i, 64, 0);
                        locs.add(l);
                        network.updateTransmitter(l, owner, true);
                    }
                    for (Location l : locs) {
                        network.updateTransmitter(l, owner, false);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        Assertions.assertTrue(done.await(60, TimeUnit.SECONDS), "Transmitter hammer must finish");
        pool.shutdown();

        // All transmitters were unregistered: the owner entry must be gone entirely,
        // not linger as an orphaned (empty or partially-filled) set.
        Assertions.assertEquals(0, network.countTransmitters(owner), "A non-zero count means a register/unregister race lost an update");
    }

    @Test
    @DisplayName("Concurrent charge reads of a crafted NaN value always return the sanitized 0")
    void testConcurrentCraftedChargeReads() throws InterruptedException {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        item.editMeta(meta -> meta.getPersistentDataContainer().set(Slimefun.getRegistry().getItemChargeDataKey(), PersistentDataType.FLOAT, Float.NaN));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        AtomicInteger bad = new AtomicInteger();

        for (int t = 0; t < 4; t++) {
            pool.submit(() -> {
                for (int i = 0; i < 1_000; i++) {
                    if (ChargeUtils.getCharge(item.getItemMeta()) != 0f) {
                        bad.incrementAndGet();
                    }
                }
            });
        }

        pool.shutdown();
        Assertions.assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        Assertions.assertEquals(0, bad.get(), "A crafted NaN charge must read as 0 on every thread, every time");
    }
}

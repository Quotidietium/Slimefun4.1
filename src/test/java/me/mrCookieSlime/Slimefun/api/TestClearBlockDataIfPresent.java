package me.mrCookieSlime.Slimefun.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for {@link BlockStorage#clearBlockDataIfPresent(Location)}: the
 * thread-safe "clear if present" primitive used by the WorldEdit integration. WorldEdit
 * replaces any block at a position it writes to - if that was a Slimefun block, its data
 * must not survive onto the new block (ghost data). The removal is deferred through the
 * deletion queue, so the assertions drain the queue via {@code TickerTask#drainQueues}.
 *
 * @author Zurker
 */
class TestClearBlockDataIfPresent {

    private static ServerMock server;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        server.getPluginManager().clearEvents();
    }

    private Block placeSfBlock(int x, int z) {
        Block b = world.getBlockAt(x, 64, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", "TEST_CLEAR_IF_PRESENT", true);
        Assertions.assertTrue(BlockStorage.hasBlockInfo(b), "Sanity: block data was registered");
        return b;
    }

    @Test
    @DisplayName("Data present: the primitive queues a removal that survives the queue drain")
    void testClearsPresentData() {
        Block b = placeSfBlock(10, 10);

        Assertions.assertTrue(BlockStorage.clearBlockDataIfPresent(b.getLocation()), "The primitive must report that data was present");

        // The removal is queued, not immediate - simulate the ticker draining the queue
        Slimefun.getTickerTask().drainQueues(world);

        Assertions.assertFalse(BlockStorage.hasBlockInfo(b), "The block data must be gone after the queue was drained");
    }

    @Test
    @DisplayName("No data present: the primitive is a no-op and queues nothing")
    void testNoOpWithoutData() {
        Block b = world.getBlockAt(20, 64, 20);
        b.setType(Material.STONE);

        Assertions.assertFalse(BlockStorage.clearBlockDataIfPresent(b.getLocation()), "The primitive must report that no data was present");

        Slimefun.getTickerTask().drainQueues(world);

        Assertions.assertFalse(BlockStorage.hasBlockInfo(b), "No data must have appeared");
    }

    @Test
    @DisplayName("The primitive is safe to call from a foreign thread")
    void testCallableFromForeignThread() throws InterruptedException {
        Block b = placeSfBlock(30, 30);
        Location l = b.getLocation();

        Thread foreign = new Thread(() -> Assertions.assertTrue(BlockStorage.clearBlockDataIfPresent(l)));
        foreign.start();
        foreign.join(5000);

        Assertions.assertFalse(foreign.isAlive(), "The foreign-thread call must not hang");
        Slimefun.getTickerTask().drainQueues(world);
        Assertions.assertFalse(BlockStorage.hasBlockInfo(b), "The block data must be gone after the queue was drained");
    }
}

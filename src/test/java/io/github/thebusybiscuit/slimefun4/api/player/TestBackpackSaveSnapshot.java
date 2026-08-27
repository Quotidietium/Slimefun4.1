package io.github.thebusybiscuit.slimefun4.api.player;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the backpack persistence path in {@link PlayerProfile#save()}:
 * backpack contents must be captured on the main thread (the async auto-save must never
 * serialize a live Inventory that a player could be editing), and
 * {@link PlayerBackpack#markDirty()} must still resolve the owning profile for backpacks
 * constructed via the current storage paths (load/newBackpack), which carry no profile
 * reference - otherwise edits made right before logging off would never be saved.
 *
 * @author Zurker
 */
class TestBackpackSaveSnapshot {

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

    @Test
    @DisplayName("markDirty on a storage-constructed backpack re-dirties the owning profile")
    void testMarkDirtyOnLoadedBackpack() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpack = profile.createBackpack(9);

        // Flush the dirty flag that createBackpack raised
        profile.save();
        Assertions.assertFalse(profile.isDirty(), "Sanity: saving cleared the dirty flag");

        // Backpacks built via load()/newBackpack() have no profile reference - markDirty
        // used to be a silent no-op for them, losing every edit made after this point
        backpack.markDirty();

        Assertions.assertTrue(profile.isDirty(), "markDirty on a storage-constructed backpack must re-dirty the profile");
    }

    @Test
    @DisplayName("The storage backend serializes backpacks from the snapshot, not the live inventory")
    void testStorageSerializesFromSnapshot() throws Exception {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpack = profile.createBackpack(9);

        // The live inventory holds a diamond, the main-thread snapshot holds an emerald -
        // the snapshot is what was on the backpack when the save cycle started
        backpack.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 3));
        ItemStack[] snapshot = new ItemStack[9];
        snapshot[0] = new ItemStack(Material.EMERALD, 2);
        Map<Integer, ItemStack[]> snapshots = Map.of(backpack.getId(), snapshot);

        Slimefun.getPlayerStorage().savePlayerData(profile.getUUID(), profile.getPlayerData(), snapshots);

        String persisted = readPersistedFile(profile.getUUID());
        Assertions.assertTrue(persisted.contains("type: EMERALD"), "The persisted content must come from the snapshot, not the live inventory");
        Assertions.assertFalse(persisted.contains("type: DIAMOND"), "The live inventory must not leak into the persisted file");
    }

    @Test
    @DisplayName("An async save persists the current backpack contents and clears the dirty flag")
    void testAsyncSaveCompletes() throws Exception {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpack = profile.createBackpack(9);
        backpack.getInventory().setItem(1, new ItemStack(Material.EMERALD, 2));

        // Save from another thread (like the async auto-save does). First write, so the
        // on-disk config does not need to deserialize any ItemStacks on reload.
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread saver = new Thread(() -> {
            try {
                profile.save();
            } catch (Throwable x) {
                failure.set(x);
            }
        });
        saver.start();
        saver.join(10_000);

        Assertions.assertNull(failure.get(), "The async save must not fail");
        Assertions.assertFalse(profile.isDirty(), "The async save cleared the dirty flag");
        Assertions.assertTrue(readPersistedFile(profile.getUUID()).contains("type: EMERALD"), "The change must be persisted");
    }

    /**
     * Reads the raw YAML file as text. ItemStack (de)serialization does not reliably work
     * in a MockBukkit environment, so the assertions match on the plain "type:" scalar
     * instead of going through any configuration API.
     */
    private static String readPersistedFile(UUID ownerId) throws Exception {
        return Files.readString(Path.of("data-storage/Slimefun/Players/" + ownerId + ".yml"), StandardCharsets.UTF_8);
    }
}

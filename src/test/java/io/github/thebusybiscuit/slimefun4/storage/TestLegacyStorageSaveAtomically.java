package io.github.thebusybiscuit.slimefun4.storage;

import java.io.File;
import java.nio.file.Files;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression for {@code LegacyStorage#saveAtomically}: the Players/ and waypoints/
 * parent directories may be missing (external cleanup, fresh session), and
 * {@code Files.write} does not create parents - every save cycle would then fail
 * forever with a retried SEVERE log while the profile stays dirty.
 *
 * @author Zurker
 */
class TestLegacyStorageSaveAtomically {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() throws InterruptedException {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("A profile save succeeds even when the Players/ and waypoints/ directories are missing")
    void testSaveWithMissingParentDirectories() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        // Simulate the directories being gone (external cleanup mid-session)
        File playersDir = new File("data-storage/Slimefun/Players");
        File waypointsDir = new File("data-storage/Slimefun/waypoints");
        deleteRecursively(playersDir);
        deleteRecursively(waypointsDir);
        Assertions.assertFalse(playersDir.exists(), "Precondition: Players/ must be gone");
        Assertions.assertFalse(waypointsDir.exists(), "Precondition: waypoints/ must be gone");

        profile.markDirty();

        // Before the fix this threw UncheckedIOException on every attempt
        Assertions.assertDoesNotThrow(profile::save, "save() must recreate missing parent directories instead of failing forever");
        Assertions.assertFalse(profile.isDirty(), "The save must have succeeded and cleared the dirty flag");
        Assertions.assertTrue(new File(playersDir, player.getUniqueId() + ".yml").exists(), "The player file must have been written");
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }

        try {
            Files.deleteIfExists(file.toPath());
        } catch (java.io.IOException ignored) {
            // best effort - the precondition assertion will surface a failure
        }
    }
}

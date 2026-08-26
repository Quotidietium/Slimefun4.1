package io.github.thebusybiscuit.slimefun4.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import be.seeseemelk.mockbukkit.MockBukkit;

/**
 * Regression coverage for the crash-consistency of {@link BackupService}: the zip used to be
 * written directly to its final timestamp name, so a failure mid-zip (or a process kill during
 * the shutdown in which backups run) left a truncated "backup" that still matched the rotation
 * filter and silently occupied one of the MAX_BACKUPS slots with unrestorable data. The zip is
 * now built under a temporary name and atomically moved into place - a failed run leaves no
 * trace of a final backup behind.
 *
 * <p>
 * The service reads the hard-coded {@code data-storage/Slimefun/...} structure of the working
 * directory (the same one MockBukkit's {@code BlockStorage} tests write into), so the failure
 * path is driven by planting a nested directory inside {@code stored-blocks/}: opening a
 * {@code FileInputStream} on a directory throws mid-zip on every platform.
 *
 * @author Zurker
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestBackupService {

    private static final File BACKUP_DIR = new File("data-storage/Slimefun/block-backups");
    private static final File STORED_BLOCKS = new File("data-storage/Slimefun/stored-blocks");
    private static final File POISON = new File(STORED_BLOCKS, "_test_backup_poison_world/nested-dir");

    private Set<File> preExistingFiles;

    @BeforeEach
    void setUp() throws IOException {
        MockBukkit.mock();
        MockBukkit.load(Slimefun.class);

        BACKUP_DIR.mkdirs();
        STORED_BLOCKS.mkdirs();
        preExistingFiles = new HashSet<>();

        File[] existing = BACKUP_DIR.listFiles();

        if (existing != null) {
            for (File file : existing) {
                preExistingFiles.add(file);
            }
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Remove the artifacts this test created (zips and tmps), keep everything pre-existing
        File[] current = BACKUP_DIR.listFiles();

        if (current != null) {
            for (File file : current) {
                if (!preExistingFiles.contains(file)) {
                    Files.deleteIfExists(file.toPath());
                }
            }
        }

        deleteRecursively(POISON.getParentFile());
        MockBukkit.unmock();
    }

    private void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }

        File[] children = file.listFiles();

        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }

        Files.deleteIfExists(file.toPath());
    }

    private Set<File> newFiles() {
        Set<File> created = new HashSet<>();
        File[] current = BACKUP_DIR.listFiles();

        if (current != null) {
            for (File file : current) {
                if (!preExistingFiles.contains(file)) {
                    created.add(file);
                }
            }
        }

        return created;
    }

    private static boolean isZip(File file) {
        return file.getName().endsWith(".zip");
    }

    private static final Predicate<File> isTmp = file -> file.getName().endsWith(".tmp");

    @Test
    @Order(1)
    @DisplayName("A mid-zip failure leaves no truncated final backup and no tmp behind")
    void testFailedBackupLeavesNoTrace() throws IOException {
        POISON.mkdirs();
        new File(POISON, "inner.txt").createNewFile();

        new BackupService().run();

        Set<File> created = newFiles();
        assertTrue(created.stream().noneMatch(TestBackupService::isZip), "A failed backup run must not leave a (truncated) final zip behind: " + created);
        assertTrue(created.stream().noneMatch(isTmp), "A failed backup run must clean up its temporary file: " + created);
    }

    @Test
    @Order(2)
    @DisplayName("A normal run produces exactly one complete zip and no leftover tmp")
    void testNormalRunProducesSingleZip() {
        new BackupService().run();

        Set<File> created = newFiles();
        assertEquals(1, created.stream().filter(TestBackupService::isZip).count(), "Exactly one backup zip should have been created: " + created);
        assertTrue(created.stream().noneMatch(isTmp), "A successful backup must not leave a tmp file behind: " + created);

        File zip = created.stream().filter(TestBackupService::isZip).findFirst().orElseThrow();
        assertTrue(zip.length() > 0, "The backup zip must not be empty");
        assertFalse(zip.getName().startsWith("."), "Sanity");
    }
}

package io.github.thebusybiscuit.slimefun4.core.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This Service creates a Backup of your Slimefun world data on every server shutdown.
 * 
 * @author TheBusyBiscuit
 *
 */
public class BackupService implements Runnable {

    /**
     * The maximum amount of backups to maintain
     */
    private static final int MAX_BACKUPS = 20;

    /**
     * Our {@link DateTimeFormatter} for formatting file names.
     */
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ROOT);

    /**
     * The directory in which to create the backups
     */
    private final File directory = new File("data-storage/Slimefun/block-backups");

    @Override
    public void run() {
        // Make sure that the directory exists.
        if (directory.exists()) {
            File[] files = directory.listFiles();

            if (files == null) {
                // directory exists but is not a directory (a regular file) - nothing to back up into.
                return;
            }

            List<File> backups = Arrays.asList(files);

            if (backups.size() > MAX_BACKUPS) {
                try {
                    purgeBackups(backups);
                } catch (Exception e) {
                    // Broadened from IOException: a stray non-backup file or a failed delete must
                    // not abort the shutdown (this runs inside onDisable, and an escaped exception
                    // would skip the cleanup scheduled after it).
                    Slimefun.logger().log(Level.WARNING, "Could not delete old backups", e);
                }
            }

            File file = new File(directory, format.format(LocalDateTime.now()) + ".zip");

            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file))) {
                            createBackup(output);
                        }

                        Slimefun.logger().log(Level.INFO, "Backed up Slimefun data to: {0}", file.getName());
                    } else {
                        Slimefun.logger().log(Level.WARNING, "Could not create backup-file: {0}", file.getName());
                    }
                } catch (IOException x) {
                    Slimefun.logger().log(Level.SEVERE, x, () -> "An Exception occurred while creating a backup for Slimefun " + Slimefun.getVersion());
                }
            }
        }
    }

    private void createBackup(@Nonnull ZipOutputStream output) throws IOException {
        Validate.notNull(output, "The Output Stream cannot be null!");

        File[] blockFolders = new File("data-storage/Slimefun/stored-blocks/").listFiles();

        if (blockFolders != null) {
            for (File folder : blockFolders) {
                addDirectory(output, folder, "stored-blocks/" + folder.getName());
            }
        }

        addDirectory(output, new File("data-storage/Slimefun/universal-inventories/"), "universal-inventories");
        addDirectory(output, new File("data-storage/Slimefun/stored-inventories/"), "stored-inventories");

        File chunks = new File("data-storage/Slimefun/stored-chunks/chunks.sfc");

        if (chunks.exists()) {
            byte[] buffer = new byte[1024];
            ZipEntry entry = new ZipEntry("stored-chunks/chunks.sfc");
            output.putNextEntry(entry);

            try (FileInputStream input = new FileInputStream(chunks)) {
                int length;

                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }

            output.closeEntry();
        }
    }

    private void addDirectory(@Nonnull ZipOutputStream output, @Nonnull File directory, @Nonnull String zipPath) throws IOException {
        byte[] buffer = new byte[1024];
        File[] files = directory.listFiles();

        if (files == null) {
            // The directory does not exist (fresh install / removed externally) - nothing to add.
            return;
        }

        for (File file : files) {
            ZipEntry entry = new ZipEntry(zipPath + '/' + file.getName());
            output.putNextEntry(entry);

            try (FileInputStream input = new FileInputStream(file)) {
                int length;

                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }

            output.closeEntry();
        }
    }

    /**
     * This method will delete old backups.
     * 
     * @param backups
     *            The {@link List} of all backups
     * 
     * @throws IOException
     *             An {@link IOException} is thrown if a {@link File} could not be deleted
     */
    private void purgeBackups(@Nonnull List<File> backups) throws IOException {
        // Only consider files whose name matches our timestamp format - a stray file in the backup
        // folder must not abort the purge with a DateTimeParseException.
        List<File> valid = new ArrayList<>();

        for (File backup : backups) {
            if (backup.isFile() && matchesBackupName(backup.getName())) {
                valid.add(backup);
            }
        }

        // Sort newest-first so the oldest backups end up at the tail.
        valid.sort((a, b) -> parseBackupName(b.getName()).compareTo(parseBackupName(a.getName())));

        // Keep the newest MAX_BACKUPS (the head), delete the rest (the oldest at the tail).
        // The previous loop deleted from the wrong end, so old backups were never purged while
        // recent ones were discarded instead.
        for (int i = valid.size() - 1; i >= MAX_BACKUPS; i--) {
            Files.delete(valid.get(i).toPath());
        }
    }

    private boolean matchesBackupName(@Nonnull String name) {
        if (!name.endsWith(".zip")) {
            return false;
        }

        try {
            parseBackupName(name);
            return true;
        } catch (Exception x) {
            return false;
        }
    }

    @Nonnull
    private LocalDateTime parseBackupName(@Nonnull String name) {
        // Strip the ".zip" suffix.
        return LocalDateTime.parse(name.substring(0, name.length() - 4), format);
    }

}

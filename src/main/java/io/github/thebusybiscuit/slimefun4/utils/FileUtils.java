package io.github.thebusybiscuit.slimefun4.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.annotation.Nonnull;

public class FileUtils {

    public static boolean deleteDirectory(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    // Recursive call to delete files and subfolders
                    if (!deleteDirectory(file)) {
                        return false;
                    }
                }
            }
        }

        // Delete the folder itself
        return folder.delete();
    }

    /**
     * Writes the given data to the target file via a temporary file and an atomic move
     * (with a plain replace as fallback for filesystems that do not support atomic moves).
     * A crash or I/O failure mid-write can only ever leave the temporary file truncated -
     * the previous contents of the target file survive untouched.
     *
     * @param target
     *            The {@link File} to write to
     * @param data
     *            The file contents (UTF-8)
     *
     * @return Whether the file was successfully written
     */
    public static boolean saveAtomically(@Nonnull File target, @Nonnull String data) {
        File parent = target.getParentFile();
        File tmpFile = parent != null ? new File(parent, target.getName() + ".tmp") : new File(target.getName() + ".tmp");

        try {
            if (parent != null) {
                // The directory may legitimately not exist yet (a file that was never written,
                // or external cleanup of its folder) - Files.write does not create parents
                Files.createDirectories(parent.toPath());
            }

            /*
             * Files.write (not a swallowed-exception writer): a half-written tmp (disk full,
             * I/O error) must abort here, so it can never be moved over a perfectly good file.
             */
            Files.write(tmpFile.toPath(), data.getBytes(StandardCharsets.UTF_8));
        } catch (IOException x) {
            deleteTmp(tmpFile);
            return false;
        }

        try {
            Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException x) {
            try {
                // Some filesystems do not support atomic moves - fall back to a plain replace
                Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException x2) {
                deleteTmp(tmpFile);
                return false;
            }
        }
    }

    private static void deleteTmp(@Nonnull File tmpFile) {
        try {
            Files.deleteIfExists(tmpFile.toPath());
        } catch (IOException ignored) {
            // The tmp is already gone or undeletable - either way there is nothing to move
        }
    }
}

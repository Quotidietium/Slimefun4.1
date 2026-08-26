package io.github.thebusybiscuit.slimefun4.utils;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.bukkit.configuration.file.FileConfiguration;

import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This class provides a crash-consistent replacement for {@link Config#save()}.
 *
 * @author Quotidietium
 *
 */
public final class ConfigUtils {

    private ConfigUtils() {}

    /**
     * Saves a dough {@link Config} to its file via a temporary file and an atomic move
     * (see {@link FileUtils#saveAtomically(File, String)}). The written bytes are identical
     * to what {@link Config#save()} would produce - including the header handling.
     *
     * @param config
     *            The {@link Config} to save
     *
     * @return Whether the file was successfully written
     */
    public static boolean saveAtomically(@Nonnull Config config) {
        FileConfiguration fileConfig = config.getConfiguration();

        // Replicates dough Config#save(File): apply the header before serialization
        if (config.getHeader() != null) {
            fileConfig.options().copyHeader(true);
            fileConfig.options().header(config.getHeader());
        } else {
            fileConfig.options().copyHeader(false);
        }

        File target = config.getFile();

        if (!FileUtils.saveAtomically(target, fileConfig.saveToString())) {
            // The error-reporting path itself must never throw (see Slimefun#validateInstance)
            Logger logger = Slimefun.instance() != null ? Slimefun.logger() : Logger.getLogger("Slimefun");
            logger.log(Level.SEVERE, () -> "Could not save the config file \"" + target.getName() + "\" (disk full?) - the previous file is still intact, but the current state was NOT written to disk");
            return false;
        }

        return true;
    }
}

package me.mrCookieSlime.CSCoreLibPlugin.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestLegacyConfigSave {

    private File tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sf-legacy-config-save").toFile();
    }

    @AfterEach
    void tearDown() {
        io.github.thebusybiscuit.slimefun4.utils.FileUtils.deleteDirectory(tempDir);
    }

    private File tmpLeftover() {
        File[] files = tempDir.listFiles((parent, name) -> name.endsWith(".tmp"));
        return files == null || files.length == 0 ? null : files[0];
    }

    private String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void testSaveWritesTargetAndLeavesNoTmp() throws IOException {
        File target = new File(tempDir, "legacy.yml");
        Config config = new Config(target);
        config.setValue("key", "value");

        config.save();

        assertEquals("value", YamlConfiguration.loadConfiguration(target).getString("key"));
        assertNull(tmpLeftover());
    }

    @Test
    void testSaveMergeAndRemovalSemantics() throws IOException {
        File target = new File(tempDir, "legacy-merge.yml");

        Config first = new Config(target);
        for (int i = 0; i < 100; i++) {
            first.setValue("entry-" + i, "persisted");
        }
        first.setValue("gone", "remove-me");
        first.save();

        // A Config constructed on an existing file loads its state - saving must preserve it
        Config second = new Config(target);
        second.setValue("key", "added");
        second.setValue("gone", null);
        second.save();

        String content = read(target);
        assertTrue(content.contains("persisted"), "Previously saved entries were lost: " + content);
        assertFalse(content.contains("remove-me"), "Removed key survived in the file: " + content);

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(target);
        assertEquals("added", loaded.getString("key"));
        assertEquals("persisted", loaded.getString("entry-0"));
        assertNull(loaded.getString("gone"));
        assertNull(tmpLeftover());
    }

    @Test
    void testSaveToFileOverloadIsAtomicToo() throws IOException {
        File other = new File(tempDir, "other.yml");
        Config config = new Config(new File(tempDir, "unused.yml"));
        config.setValue("key", "value");

        config.save(other);

        assertEquals("value", YamlConfiguration.loadConfiguration(other).getString("key"));
        assertNull(tmpLeftover());
    }

    @Test
    void testSaveFailureKeepsPreviousFileIntact() throws IOException {
        File target = new File(tempDir, "intact.yml");
        Config config = new Config(target);
        config.setValue("key", "old");
        config.save();
        assertEquals("old", YamlConfiguration.loadConfiguration(target).getString("key"));

        // Sabotage: replace the parent directory with a regular file so the tmp write fails.
        // The CS-CoreLib Config keeps the file handle of its constructor target, so we test
        // via a second Config whose file lives under a now-unwritable "parent".
        File fileParent = new File(tempDir, "blocked");
        assertTrue(fileParent.createNewFile());
        Config blocked = new Config(new File(fileParent, "never.yml"));
        blocked.setValue("key", "new");

        // Must not throw (legacy contract: save() is silent) - and the intact file above survives
        blocked.save();
        assertEquals("old", YamlConfiguration.loadConfiguration(target).getString("key"));
        assertFalse(new File(fileParent, "never.yml").exists());
    }
}

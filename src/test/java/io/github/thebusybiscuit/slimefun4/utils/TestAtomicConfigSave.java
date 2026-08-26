package io.github.thebusybiscuit.slimefun4.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

class TestAtomicConfigSave {

    private File tempDir;

    @BeforeAll
    public static void load() {
        MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sf-atomic-config-save").toFile();
    }

    @AfterEach
    void tearDown() {
        FileUtils.deleteDirectory(tempDir);
    }

    private File tmpLeftover(File dir) {
        File[] files = dir.listFiles((parent, name) -> name.endsWith(".tmp"));
        return files == null || files.length == 0 ? null : files[0];
    }

    private String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    void testAtomicSaveWritesTargetAndLeavesNoTmp() throws IOException {
        File target = new File(tempDir, "plain.yml");

        assertTrue(FileUtils.saveAtomically(target, "key: value\n"));
        assertEquals("key: value\n", read(target));
        assertNull(tmpLeftover(tempDir));
    }

    @Test
    void testAtomicSaveCreatesMissingParentDirectories() throws IOException {
        File target = new File(tempDir, "deep/nested/dir/plain.yml");

        assertTrue(FileUtils.saveAtomically(target, "key: value\n"));
        assertEquals("key: value\n", read(target));
        assertNull(tmpLeftover(target.getParentFile()));
    }

    @Test
    void testAtomicSaveFullyReplacesPreviousContent() throws IOException {
        File target = new File(tempDir, "replace.yml");

        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longContent.append("line-").append(i).append(": ").append("x".repeat(50)).append('\n');
        }

        assertTrue(FileUtils.saveAtomically(target, longContent.toString()));
        assertTrue(FileUtils.saveAtomically(target, "key: short\n"));

        // The second save must fully replace the (much longer) first state - no residue
        assertEquals("key: short\n", read(target));
        assertNull(tmpLeftover(tempDir));
    }

    @Test
    void testAtomicSaveFailsWhenParentIsAFile() throws IOException {
        File fileParent = new File(tempDir, "not-a-directory");
        assertTrue(fileParent.createNewFile());

        File target = new File(fileParent, "plain.yml");
        assertFalse(FileUtils.saveAtomically(target, "key: value\n"));

        // The failure must not have turned the parent file into a directory or left a tmp behind
        assertTrue(fileParent.isFile());
        assertNull(tmpLeftover(tempDir));
    }

    @Test
    void testDoughConfigSavedAtomically() throws IOException {
        Config config = new Config(new File(tempDir, "dough.yml"));
        config.setValue("key", "value");

        assertTrue(ConfigUtils.saveAtomically(config));

        File target = new File(tempDir, "dough.yml");
        assertEquals("value", YamlConfiguration.loadConfiguration(target).getString("key"));
        assertNull(tmpLeftover(tempDir));
    }

    @Test
    void testDoughConfigMergeAndRemovalSemantics() throws IOException {
        File target = new File(tempDir, "dough-merge.yml");

        Config first = new Config(target);
        for (int i = 0; i < 100; i++) {
            first.setValue("entry-" + i, "persisted");
        }
        first.setValue("gone", "remove-me");
        assertTrue(ConfigUtils.saveAtomically(first));

        // A Config constructed on an existing file loads its state - saving must preserve it
        Config second = new Config(target);
        second.setValue("key", "added");
        second.setValue("gone", null);
        assertTrue(ConfigUtils.saveAtomically(second));

        String content = read(target);
        assertTrue(content.contains("persisted"), "Previously saved entries were lost: " + content);
        assertFalse(content.contains("remove-me"), "Removed key survived in the file: " + content);

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(target);
        assertEquals("added", loaded.getString("key"));
        assertEquals("persisted", loaded.getString("entry-0"));
        assertNull(loaded.getString("gone"));
        assertNull(tmpLeftover(tempDir));
    }

    @Test
    void testDoughConfigHeaderPreserved() throws IOException {
        Config config = new Config(new File(tempDir, "header.yml"));
        config.setHeader("This is a custom header");
        config.setValue("key", "value");

        assertTrue(ConfigUtils.saveAtomically(config));

        String content = read(new File(tempDir, "header.yml"));
        assertTrue(content.contains("This is a custom header"), "Header was not written: " + content);
        assertTrue(content.contains("key: value"));
        assertNull(tmpLeftover(tempDir));
    }
}

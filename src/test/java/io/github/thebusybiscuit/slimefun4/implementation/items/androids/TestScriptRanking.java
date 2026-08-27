package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the script ranking in {@link Script#getUploadedScripts(AndroidType)}:
 * the old comparator key reduced to 1 - (up + down), i.e. it ranked by TOTAL vote count,
 * so a heavily downvoted script would appear above a small, well-received one.
 *
 * @author Zurker
 */
class TestScriptRanking {

    private static ServerMock server;
    private static Slimefun plugin;
    private static final File SCRIPT_DIR = new File("plugins/Slimefun/scripts/NONE");

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @AfterEach
    public void cleanUp() {
        File[] files = SCRIPT_DIR.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("ranking_test_")) {
                    file.delete();
                }
            }
        }
    }

    private static void writeScript(String suffix, String name, int upvotes, int downvotes) {
        Config config = new Config(new File(SCRIPT_DIR, "ranking_test_" + suffix + ".sfs"));
        config.setValue("author", UUID.randomUUID().toString());
        config.setValue("name", name);
        config.setValue("code", "START");
        config.setValue("downloads", 0);
        config.setValue("android", "NONE");

        String[] up = new String[upvotes];
        Arrays.fill(up, UUID.randomUUID().toString());
        String[] down = new String[downvotes];
        Arrays.fill(down, UUID.randomUUID().toString());
        config.setValue("rating.positive", Arrays.asList(up));
        config.setValue("rating.negative", Arrays.asList(down));
        config.save();
    }

    @Test
    @DisplayName("Scripts are ranked by net upvotes, not by total vote count")
    void testRankingByNetVotes() throws InterruptedException {
        // Wait for the async config write to flush
        Thread.sleep(200);

        // A small well-received script (net +2) and a heavily contested one (net 0)
        writeScript("good", "Good Script", 3, 1);
        writeScript("contested", "Contested Script", 10, 10);
        Thread.sleep(200);

        List<Script> scripts = Script.getUploadedScripts(AndroidType.FARMER);

        Script first = scripts.get(0);
        Assertions.assertEquals("Good Script", first.getName(), "The script with the higher net score must rank first, regardless of total votes");
        Assertions.assertEquals("Contested Script", scripts.get(1).getName());
    }
}

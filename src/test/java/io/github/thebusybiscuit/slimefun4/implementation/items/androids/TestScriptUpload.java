package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.FileUtils;

/**
 * Regression coverage for {@link Script#upload} filename hardening and
 * {@link Script#rate} duplicate-vote guard.
 * <p>
 * The upload filename previously embedded the raw player name: on offline-mode
 * servers a crafted login name ("../x") escaped the scripts directory (path
 * traversal), and two players sharing a name across renames overwrote each
 * other's scripts. The filename is now keyed by the player's {@link java.util.UUID}.
 *
 * @author Zurker
 */
class TestScriptUpload {

    private static ServerMock server;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() throws IOException {
        MockBukkit.unmock();
        FileUtils.deleteDirectory(new File("plugins/Slimefun/scripts"));
    }

    @Test
    @DisplayName("Uploaded scripts are keyed by UUID, not by the raw player name")
    void testUploadFilenameUsesUUID() {
        // A name that would escape the scripts directory if embedded verbatim
        Player player = server.addPlayer(".._.._traversal");

        Script.upload(player, AndroidType.NONE, 1, "Test Script", "START-REPEAT");

        File directory = new File("plugins/Slimefun/scripts/NONE");
        File[] files = directory.listFiles();

        Assertions.assertNotNull(files, "The scripts directory must exist");
        Assertions.assertEquals(1, files.length, "Exactly one script file must have been written");
        Assertions.assertTrue(files[0].getName().startsWith(player.getUniqueId().toString()), "The filename must be keyed by UUID, not by the raw player name");
        Assertions.assertTrue(files[0].getParentFile().getAbsolutePath().endsWith("NONE"), "The file must not have escaped the scripts directory");

        // The script must still load through the downloader path
        List<Script> scripts = Script.getUploadedScripts(AndroidType.NONE);
        Assertions.assertEquals(1, scripts.size());
        Assertions.assertEquals("Test Script", scripts.get(0).getName());
        Assertions.assertTrue(scripts.get(0).isAuthor(player));
    }

    @Test
    @DisplayName("A second rating by the same player is rejected at write time")
    void testDuplicateRatingRejected() {
        Player author = server.addPlayer();
        Player voter = server.addPlayer();

        Script.upload(author, AndroidType.NONE, 7, "Rated Script", "START-REPEAT");
        Script script = Script.getUploadedScripts(AndroidType.NONE).stream().filter(s -> s.getName().equals("Rated Script")).findFirst().orElseThrow();

        script.rate(voter, true);
        script.rate(voter, true);

        Assertions.assertEquals(1, script.getUpvotes(), "The duplicate vote must have been rejected");

        // A vote on the opposite side by the same player is also rejected
        script.rate(voter, false);
        Assertions.assertEquals(0, script.getDownvotes(), "A player who already voted must not vote again on the other side");
    }
}

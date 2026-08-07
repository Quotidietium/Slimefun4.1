package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.AndroidScriptChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the android API expansion: {@link AndroidScriptChangeEvent},
 * exercised by driving the real {@link ProgrammableAndroid#applyScriptChange} script
 * application path that every script editor write (add, delete, duplicate, download)
 * delegates to.
 * <p>
 * The editor clicks cannot be simulated under MockBukkit, so the tests drive the
 * extracted application method directly. The outcome is asserted end-to-end through
 * the script stored in {@link BlockStorage}.
 *
 * @author Zurker
 */
class TestAndroidScriptChangeEvent {

    private static final String DEFAULT_SCRIPT = "START-TURN_LEFT-REPEAT";
    private static final String NEW_SCRIPT = "START-TURN_RIGHT-REPEAT";

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ProgrammableAndroid android;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_script_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SCRIPT_ANDROID", HeadTexture.PROGRAMMABLE_ANDROID, "&7Test Script Android");
        Slimefun.getItemCfg().setValue("_TEST_SCRIPT_ANDROID.enabled", true);
        android = new ProgrammableAndroid(itemGroup, 1, stack, RecipeType.NULL, new ItemStack[9]);
        android.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    /**
     * Places an android block backed by {@link BlockStorage} with the given script
     * (or none at all when null).
     */
    private Block placeAndroid(int x, int z, String script) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_SCRIPT_ANDROID");

        if (script != null) {
            BlockStorage.addBlockInfo(b, "script", script);
        }

        return b;
    }

    private String storedScript(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "script");
    }

    @Test
    @DisplayName("AndroidScriptChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeAndroid(1, 1, DEFAULT_SCRIPT);

        AndroidScriptChangeEvent event = new AndroidScriptChangeEvent(player, android, b, DEFAULT_SCRIPT, NEW_SCRIPT);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(android, event.getAndroid());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(DEFAULT_SCRIPT, event.getOldScript());
        Assertions.assertEquals(NEW_SCRIPT, event.getNewScript());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidScriptChangeEvent(player, null, b, DEFAULT_SCRIPT, NEW_SCRIPT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidScriptChangeEvent(player, android, null, DEFAULT_SCRIPT, NEW_SCRIPT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidScriptChangeEvent(player, android, b, null, NEW_SCRIPT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidScriptChangeEvent(player, android, b, DEFAULT_SCRIPT, null));
    }

    @Test
    @DisplayName("Changing the script fires the event and stores the new script")
    void testChangeFiresEventAndStoresScript() {
        Player player = server.addPlayer();
        Block b = placeAndroid(10, 10, DEFAULT_SCRIPT);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onScriptChange(AndroidScriptChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(android, event.getAndroid());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(DEFAULT_SCRIPT, event.getOldScript());
                Assertions.assertEquals(NEW_SCRIPT, event.getNewScript());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = android.applyScriptChange(player, b, NEW_SCRIPT);

            Assertions.assertTrue(applied, "The change must have been applied");
            Assertions.assertTrue(seen[0], "AndroidScriptChangeEvent was not fired");
            Assertions.assertEquals(NEW_SCRIPT, storedScript(b), "The stored script must have been updated");
            Assertions.assertEquals(NEW_SCRIPT, android.getScript(b.getLocation()), "getScript must read back the new script");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AndroidScriptChangeEvent keeps the stored script")
    void testCancelKeepsOldScript() {
        Player player = server.addPlayer();
        Block b = placeAndroid(20, 20, DEFAULT_SCRIPT);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onScriptChange(AndroidScriptChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean applied = android.applyScriptChange(player, b, NEW_SCRIPT);

            Assertions.assertFalse(applied, "A vetoed change must not be applied");
            Assertions.assertEquals(DEFAULT_SCRIPT, storedScript(b), "A vetoed change must keep the old script");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Changing the script without listeners still stores it, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeAndroid(30, 30, DEFAULT_SCRIPT);

        boolean applied = android.applyScriptChange(player, b, NEW_SCRIPT);

        Assertions.assertTrue(applied);
        Assertions.assertEquals(NEW_SCRIPT, storedScript(b), "The stored script must have been updated");
    }

    @Test
    @DisplayName("A fresh android without a stored script reports the default script as the old one")
    void testFreshAndroidDefaultsOldScript() {
        Player player = server.addPlayer();
        Block b = placeAndroid(40, 40, null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onScriptChange(AndroidScriptChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(DEFAULT_SCRIPT, event.getOldScript(), "A missing script must read as the default script");
                Assertions.assertEquals(NEW_SCRIPT, event.getNewScript());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean applied = android.applyScriptChange(player, b, NEW_SCRIPT);

            Assertions.assertTrue(applied);
            Assertions.assertTrue(seen[0], "AndroidScriptChangeEvent was not fired");
            Assertions.assertEquals(NEW_SCRIPT, storedScript(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A malformed script is still rejected by the setScript validation")
    void testMalformedScriptRejected() {
        Player player = server.addPlayer();
        Block b = placeAndroid(50, 50, DEFAULT_SCRIPT);

        // A script must begin with a 'START' token and end with a 'REPEAT' token
        Assertions.assertThrows(IllegalArgumentException.class, () -> android.applyScriptChange(player, b, "TURN_LEFT-TURN_RIGHT"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> android.applyScriptChange(player, b, null));
        Assertions.assertEquals(DEFAULT_SCRIPT, storedScript(b), "A rejected script must not be stored");
    }

    @Test
    @DisplayName("Consecutive edits chain old and new scripts correctly")
    void testConsecutiveChangesChain() {
        Player player = server.addPlayer();
        Block b = placeAndroid(60, 60, DEFAULT_SCRIPT);

        String[] observedOld = { null };
        String[] observedNew = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onScriptChange(AndroidScriptChangeEvent event) {
                observedOld[0] = event.getOldScript();
                observedNew[0] = event.getNewScript();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        String secondScript = "START-TURN_RIGHT-TURN_RIGHT-REPEAT";

        try {
            Assertions.assertTrue(android.applyScriptChange(player, b, NEW_SCRIPT));
            Assertions.assertEquals(DEFAULT_SCRIPT, observedOld[0]);
            Assertions.assertEquals(NEW_SCRIPT, observedNew[0]);

            Assertions.assertTrue(android.applyScriptChange(player, b, secondScript));
            Assertions.assertEquals(NEW_SCRIPT, observedOld[0], "The second edit must see the first edit's script as the old one");
            Assertions.assertEquals(secondScript, observedNew[0]);
            Assertions.assertEquals(secondScript, storedScript(b));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

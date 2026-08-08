package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunMachineCrashEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the machine-crash API expansion:
 * {@link SlimefunMachineCrashEvent}, exercised by calling the real
 * {@link TickerTask#reportErrors} four times via reflection on the live
 * {@link Slimefun#getTickerTask()}.
 * <p>
 * The fourth call triggers the crash threshold (errors == 4) and fires the event. A
 * cancelled event spares the machine (BlockStorage data preserved); without listeners the
 * machine is terminated as before (BlockStorage data deleted).
 *
 * @author Zurker
 */
class TestSlimefunMachineCrashEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SlimefunItem testItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // NetworkManager and ProtectionManager are created when integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "machine_crash_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CRASH_ITEM", Material.DISPENSER, "&fTest Crash Item");
        Slimefun.getItemCfg().setValue("_TEST_CRASH_ITEM.enabled", true);
        testItem = new SlimefunItem(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        testItem.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Location placeBlock(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", testItem.getId());
        return b.getLocation();
    }

    /**
     * Calls the private {@code reportErrors} four times, triggering the crash threshold
     * on the fourth call.
     */
    private void triggerCrash(Location loc) throws Exception {
        Method reportErrors = TickerTask.class.getDeclaredMethod("reportErrors", Location.class, SlimefunItem.class, Throwable.class);
        reportErrors.setAccessible(true);

        for (int i = 0; i < 4; i++) {
            reportErrors.invoke(Slimefun.getTickerTask(), loc, testItem, new RuntimeException("test " + i));
        }
    }

    @Test
    @DisplayName("SlimefunMachineCrashEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location loc = placeBlock(1, 1);

        SlimefunMachineCrashEvent event = new SlimefunMachineCrashEvent(loc, testItem);

        Assertions.assertEquals(loc, event.getLocation());
        Assertions.assertEquals(testItem, event.getItem());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunMachineCrashEvent(null, testItem));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunMachineCrashEvent(loc, null));
    }

    @Test
    @DisplayName("Four consecutive tick errors fire SlimefunMachineCrashEvent")
    void testCrashFiresEvent() throws Exception {
        Location loc = placeBlock(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCrash(SlimefunMachineCrashEvent event) {
                seen[0] = true;
                Assertions.assertEquals(loc, event.getLocation());
                Assertions.assertEquals(testItem, event.getItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            triggerCrash(loc);

            Assertions.assertTrue(seen[0], "SlimefunMachineCrashEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunMachineCrashEvent spares the machine's BlockStorage data")
    void testCancelSpareMachine() throws Exception {
        Location loc = placeBlock(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCrash(SlimefunMachineCrashEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            triggerCrash(loc);

            Assertions.assertTrue(BlockStorage.hasBlockInfo(loc), "A vetoed crash must preserve the BlockStorage data");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Without listeners the crash terminates the machine, preserving the old behavior")
    void testCrashWithoutListenersTerminates() throws Exception {
        Location loc = placeBlock(30, 30);

        triggerCrash(loc);

        Assertions.assertFalse(BlockStorage.hasBlockInfo(loc), "The BlockStorage data must have been wiped");
    }
}

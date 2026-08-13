package io.github.thebusybiscuit.slimefun4.api.player;

import org.bukkit.Material;
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

import io.github.thebusybiscuit.slimefun4.api.events.BackpackResizeEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the backpack API expansion: {@link BackpackResizeEvent},
 * exercised by driving the real {@link PlayerBackpack#setSize(int)} resize path.
 * <p>
 * The backpack inventory is fully functional under MockBukkit, so the resize is
 * asserted end-to-end through {@link PlayerBackpack#getSize()} and the surviving
 * contents. A vetoed resize keeps the original size; an invalid size or a
 * destructive shrink still throws without firing the event.
 *
 * @author Zurker
 */
class TestBackpackResizeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private PlayerBackpack newBackpack(int size) throws InterruptedException {
        Player player = server.addPlayer();
        return TestUtilities.awaitProfile(player).createBackpack(size);
    }

    @Test
    @DisplayName("BackpackResizeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() throws InterruptedException {
        PlayerBackpack backpack = newBackpack(9);

        BackpackResizeEvent event = new BackpackResizeEvent(backpack, 9, 18);

        Assertions.assertEquals(backpack, event.getBackpack());
        Assertions.assertEquals(9, event.getOldSize());
        Assertions.assertEquals(18, event.getNewSize());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackResizeEvent(null, 9, 18));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackResizeEvent(backpack, 0, 18));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BackpackResizeEvent(backpack, 9, 0));
    }

    @Test
    @DisplayName("Resizing a backpack fires the event and applies the new size")
    void testResizeFiresEventAndApplies() throws InterruptedException {
        PlayerBackpack backpack = newBackpack(9);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onResize(BackpackResizeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(backpack, event.getBackpack());
                Assertions.assertEquals(9, event.getOldSize());
                Assertions.assertEquals(18, event.getNewSize());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            backpack.setSize(18);

            Assertions.assertTrue(seen[0], "BackpackResizeEvent was not fired");
            Assertions.assertEquals(18, backpack.getSize(), "The new size must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling BackpackResizeEvent keeps the original size")
    void testCancelKeepsOriginalSize() throws InterruptedException {
        PlayerBackpack backpack = newBackpack(9);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onResize(BackpackResizeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            backpack.setSize(18);

            Assertions.assertEquals(9, backpack.getSize(), "A vetoed resize must keep the original size");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Resizing without listeners still applies, preserving the old behavior")
    void testResizeWithoutListenersApplies() throws InterruptedException {
        PlayerBackpack backpack = newBackpack(9);

        backpack.setSize(27);

        Assertions.assertEquals(27, backpack.getSize(), "The new size must have been applied");
    }

    @Test
    @DisplayName("Resizing preserves the existing contents")
    void testResizePreservesContents() throws InterruptedException {
        PlayerBackpack backpack = newBackpack(9);
        backpack.getInventory().setItem(0, new ItemStack(Material.DIRT, 32));

        backpack.setSize(18);

        Assertions.assertEquals(18, backpack.getSize());
        Assertions.assertEquals(Material.DIRT, backpack.getInventory().getItem(0).getType());
        Assertions.assertEquals(32, backpack.getInventory().getItem(0).getAmount(), "The contents must survive the resize");
    }

    @Test
    @DisplayName("Resizing while a viewer has the backpack open moves the viewer onto the new inventory")
    void testResizeMovesViewerOntoNewInventory() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerBackpack backpack = TestUtilities.awaitProfile(player).createBackpack(9);
        backpack.getInventory().setItem(0, new ItemStack(Material.DIRT, 32));

        backpack.open(player);
        Assertions.assertSame(backpack.getInventory(), player.getOpenInventory().getTopInventory(), "The viewer must be looking at the backpack inventory");

        backpack.setSize(18);

        /*
         * Without re-seating, the viewer would keep editing the discarded old Inventory
         * and every change would be lost on save.
         */
        Assertions.assertSame(backpack.getInventory(), player.getOpenInventory().getTopInventory(), "The viewer must have been moved onto the resized inventory");
        Assertions.assertEquals(18, player.getOpenInventory().getTopInventory().getSize());
        Assertions.assertEquals(Material.DIRT, player.getOpenInventory().getTopInventory().getItem(0).getType(), "The contents must be visible in the re-seated view");
    }

    @Test
    @DisplayName("An out-of-range size throws without firing the event")
    void testInvalidSizeThrowsWithoutEvent() throws InterruptedException {
        PlayerBackpack backpack = newBackpack(9);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onResize(BackpackResizeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertThrows(IllegalArgumentException.class, () -> backpack.setSize(8));
            Assertions.assertThrows(IllegalArgumentException.class, () -> backpack.setSize(55));
            Assertions.assertFalse(seen[0], "No event must be fired for an invalid size");
            Assertions.assertEquals(9, backpack.getSize());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A destructive shrink throws without firing the event")
    void testDestructiveShrinkThrowsWithoutEvent() throws InterruptedException {
        PlayerBackpack backpack = newBackpack(18);
        // Put an item in a slot that would be cut off by shrinking to 9
        backpack.getInventory().setItem(15, new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onResize(BackpackResizeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertThrows(IllegalStateException.class, () -> backpack.setSize(9));
            Assertions.assertFalse(seen[0], "No event must be fired for a destructive shrink");
            Assertions.assertEquals(18, backpack.getSize());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerBackpackCloseEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerBackpackOpenEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the backpack API expansion: {@link PlayerBackpackOpenEvent}
 * and {@link PlayerBackpackCloseEvent}, exercised through the real
 * {@link BackpackListener} open/close paths.
 *
 * @author Zurker
 */
class TestBackpackEvents {

    private static final int BACKPACK_SIZE = 27;
    private static ServerMock server;
    private static Slimefun plugin;
    private static BackpackListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        listener = new BackpackListener();
        listener.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private ItemStack createBackpackItem(Player player, String id) throws InterruptedException {
        SlimefunItemStack item = new SlimefunItemStack(id, Material.CHEST, "&4Mock Backpack", "", "&7Size: &e" + BACKPACK_SIZE, "&7ID: <ID>", "", "&7&eRight Click&7 to open");
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpack = profile.createBackpack(BACKPACK_SIZE);

        // SlimefunItemStack.item() returns a fresh copy each call - hold on to ONE
        // instance so the resolved id lore and the event's item line up.
        ItemStack stack = item.item();
        listener.setBackpackId(player, stack, 2, backpack.getId());

        ItemGroup itemGroup = new ItemGroup(new NamespacedKey(plugin, "test_backpack_events"), CustomItemStack.create(Material.CHEST, "&4Test Backpacks"));
        SlimefunBackpack slimefunBackpack = new SlimefunBackpack(BACKPACK_SIZE, itemGroup, item, RecipeType.NULL, new ItemStack[9]);
        slimefunBackpack.register(plugin);

        listener.openBackpack(player, stack, slimefunBackpack);
        return stack;
    }

    @Test
    @DisplayName("PlayerBackpackOpenEvent is fired through the real open path with the right context")
    void testOpenEventFired() throws InterruptedException {
        Player player = server.addPlayer();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PlayerBackpackOpenEvent> ref = new AtomicReference<>();

        Listener openListener = new Listener() {
            @EventHandler
            public void onOpen(PlayerBackpackOpenEvent event) {
                ref.set(event);
                latch.countDown();
            }
        };
        server.getPluginManager().registerEvents(openListener, plugin);

        try {
            ItemStack item = createBackpackItem(player, "OPEN_EVENT_BACKPACK");

            Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "PlayerBackpackOpenEvent was not fired");
            PlayerBackpackOpenEvent event = ref.get();

            Assertions.assertEquals(player, event.getPlayer());
            Assertions.assertEquals(item, event.getItem());
            Assertions.assertNotNull(event.getBackpack());
            Assertions.assertEquals(player.getUniqueId(), event.getBackpack().getOwnerId());
        } finally {
            HandlerList.unregisterAll(openListener);
        }
    }

    @Test
    @DisplayName("Cancelling PlayerBackpackOpenEvent prevents the backpack view from opening")
    void testOpenEventCancellation() throws InterruptedException {
        Player player = server.addPlayer();

        Listener cancellingListener = new Listener() {
            @EventHandler
            public void onOpen(PlayerBackpackOpenEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancellingListener, plugin);

        try {
            ItemStack item = createBackpackItem(player, "CANCELLED_OPEN_BACKPACK");

            // Give the asynchronous backpack-loading chain a moment to (not) complete
            Thread.sleep(300);

            /*
             * Observable behavior: while a backpack view is open, dropping the backpack
             * item is denied. A cancelled open never registers the view, so the drop
             * passes through untouched.
             */
            Item drop = new ItemEntityMock(server, UUID.randomUUID(), item);
            PlayerDropItemEvent dropEvent = new PlayerDropItemEvent(player, drop);
            listener.onItemDrop(dropEvent);

            Assertions.assertFalse(dropEvent.isCancelled());
        } finally {
            HandlerList.unregisterAll(cancellingListener);
        }
    }

    @Test
    @DisplayName("PlayerBackpackCloseEvent is fired when an open backpack view is closed")
    void testCloseEventFired() throws InterruptedException {
        Player player = server.addPlayer();
        ItemStack item = createBackpackItem(player, "CLOSE_EVENT_BACKPACK");

        // Wait until the view is actually registered (drop protection active)
        Thread.sleep(300);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PlayerBackpackCloseEvent> ref = new AtomicReference<>();

        Listener closeListener = new Listener() {
            @EventHandler
            public void onClose(PlayerBackpackCloseEvent event) {
                ref.set(event);
                latch.countDown();
            }
        };
        server.getPluginManager().registerEvents(closeListener, plugin);

        try {
            listener.onClose(new InventoryCloseEvent(player.getOpenInventory()));

            Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "PlayerBackpackCloseEvent was not fired");
            Assertions.assertEquals(player, ref.get().getPlayer());
            Assertions.assertEquals(item, ref.get().getItem());
        } finally {
            HandlerList.unregisterAll(closeListener);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.SoulboundItemsKeepEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SoulboundItemsReturnEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * Regression coverage for the soulbound API expansion: {@link SoulboundItemsKeepEvent}
 * and {@link SoulboundItemsReturnEvent}, exercised through the real {@link SoulboundListener}
 * death/respawn paths.
 *
 * @author Zurker
 */
class TestSoulboundEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static SoulboundListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        listener = new SoulboundListener(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private PlayerDeathEvent newDeathEvent(PlayerMock player, List<ItemStack> drops) {
        PlayerDeathEvent deathEvent = Mockito.mock(PlayerDeathEvent.class);
        Mockito.when(deathEvent.getEntity()).thenReturn(player);
        Mockito.when(deathEvent.getKeepInventory()).thenReturn(false);
        Mockito.when(deathEvent.getDrops()).thenReturn(drops);
        return deathEvent;
    }

    private PlayerRespawnEvent newRespawnEvent(PlayerMock player) {
        PlayerRespawnEvent respawnEvent = Mockito.mock(PlayerRespawnEvent.class);
        Mockito.when(respawnEvent.getPlayer()).thenReturn(player);
        return respawnEvent;
    }

    @Test
    @DisplayName("Cancelling SoulboundItemsKeepEvent leaves soulbound items in the death drops")
    void testKeepEventCancellation() {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Soulbound Sword");
        SlimefunUtils.setSoulbound(item, true);
        player.getInventory().setItem(6, item);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(item.clone());

        boolean[] seen = { false };
        Listener cancelling = new Listener() {
            @EventHandler
            public void onKeep(SoulboundItemsKeepEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                    Assertions.assertNotNull(event.getDeathEvent());
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            listener.onDamage(newDeathEvent(player, drops));

            Assertions.assertTrue(seen[0], "SoulboundItemsKeepEvent was not fired");
            Assertions.assertEquals(1, drops.size(), "Cancelled keep must leave the soulbound item in the drops");

            // Nothing was stored, so a respawn must not fire a return event for this player
            boolean[] returned = { false };
            Listener returnWatcher = new Listener() {
                @EventHandler
                public void onReturn(SoulboundItemsReturnEvent event) {
                    if (event.getPlayer().equals(player)) {
                        returned[0] = true;
                    }
                }
            };
            server.getPluginManager().registerEvents(returnWatcher, plugin);

            try {
                listener.onRespawn(newRespawnEvent(player));
                Assertions.assertFalse(returned[0], "Cancelled keep must not store items for return");
            } finally {
                HandlerList.unregisterAll(returnWatcher);
            }
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("An excluded soulbound item drops normally and is not returned on respawn")
    void testExcludedItemDropsNormally() {
        PlayerMock player = server.addPlayer();
        ItemStack sword = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Soulbound Sword");
        SlimefunUtils.setSoulbound(sword, true);
        ItemStack shovel = CustomItemStack.create(Material.DIAMOND_SHOVEL, "&4Soulbound Shovel");
        SlimefunUtils.setSoulbound(shovel, true);
        player.getInventory().setItem(6, sword);
        player.getInventory().setItem(7, shovel);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(sword.clone());
        drops.add(shovel.clone());

        Listener excluding = new Listener() {
            @EventHandler
            public void onKeep(SoulboundItemsKeepEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.excludeWhen(item -> item.getType() == Material.DIAMOND_SWORD);
                }
            }
        };
        server.getPluginManager().registerEvents(excluding, plugin);

        try {
            listener.onDamage(newDeathEvent(player, drops));

            Assertions.assertEquals(1, drops.size(), "The excluded sword must have stayed in the death drops");
            Assertions.assertEquals(Material.DIAMOND_SWORD, drops.get(0).getType());

            // MockBukkit does not clear the inventory on death like a real server would
            player.getInventory().clear();

            listener.onRespawn(newRespawnEvent(player));

            Assertions.assertNull(player.getInventory().getItem(6), "The excluded sword must not have been returned");
            Assertions.assertNotNull(player.getInventory().getItem(7), "The shovel must have been kept as usual");
            Assertions.assertEquals(Material.DIAMOND_SHOVEL, player.getInventory().getItem(7).getType());
        } finally {
            HandlerList.unregisterAll(excluding);
        }
    }

    @Test
    @DisplayName("SoulboundItemsKeepEvent validates its exclusion predicates")
    void testExcludeWhenValidation() {
        PlayerMock player = server.addPlayer();
        SoulboundItemsKeepEvent event = new SoulboundItemsKeepEvent(player, newDeathEvent(player, new ArrayList<>()));

        Assertions.assertFalse(event.isExcluded(null), "A null item is never excluded");
        Assertions.assertFalse(event.isExcluded(new ItemStack(Material.DIAMOND_SWORD)), "Without a predicate nothing is excluded");

        event.excludeWhen(item -> item.getType() == Material.DIAMOND_SWORD);

        Assertions.assertTrue(event.isExcluded(new ItemStack(Material.DIAMOND_SWORD)));
        Assertions.assertFalse(event.isExcluded(new ItemStack(Material.DIAMOND_SHOVEL)));

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.excludeWhen(null));
    }

    @Test
    @DisplayName("SoulboundItemsReturnEvent fires on respawn with the returned items")
    void testReturnEventOnRespawn() {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Soulbound Sword");
        SlimefunUtils.setSoulbound(item, true);
        player.getInventory().setItem(6, item);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(item.clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onReturn(SoulboundItemsReturnEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                    Map<Integer, ItemStack> items = event.getItems();

                    Assertions.assertEquals(1, items.size());
                    Assertions.assertTrue(items.containsKey(6));
                    Assertions.assertEquals(Material.DIAMOND_SWORD, items.get(6).getType());
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onDamage(newDeathEvent(player, drops));
            Assertions.assertTrue(drops.isEmpty());

            listener.onRespawn(newRespawnEvent(player));

            Assertions.assertTrue(seen[0], "SoulboundItemsReturnEvent was not fired");
            Assertions.assertEquals(Material.DIAMOND_SWORD, player.getInventory().getItem(6).getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("SoulboundItemsReturnEvent exposes the cursor pseudo-slot and an immutable map")
    void testReturnEventCursorSlotAndImmutability() {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Soulbound Sword");
        SlimefunUtils.setSoulbound(item, true);
        player.setItemOnCursor(item);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(item.clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onReturn(SoulboundItemsReturnEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                    Assertions.assertTrue(event.getItems().containsKey(Integer.MIN_VALUE));
                    Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getItems().put(0, item));
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onDamage(newDeathEvent(player, drops));
            listener.onRespawn(newRespawnEvent(player));

            Assertions.assertTrue(seen[0], "SoulboundItemsReturnEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

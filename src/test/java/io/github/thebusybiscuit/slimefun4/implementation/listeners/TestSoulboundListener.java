package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.SoulboundItem;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

class TestSoulboundListener {

    private static Slimefun plugin;
    private static ServerMock server;
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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("Test if the soulbound item is dropped or not")
    void testItemDrop(boolean soulbound) {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Cool Sword");
        SlimefunUtils.setSoulbound(item, soulbound);
        player.getInventory().setItem(6, item);
        player.setHealth(0);

        server.getPluginManager().assertEventFired(EntityDeathEvent.class, event -> {
            return soulbound != event.getDrops().contains(item);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("Test if soulbound item is dropped if disabled")
    void testItemDropIfItemDisabled(boolean enabled) {
        PlayerMock player = server.addPlayer();

        SlimefunItemStack item = new SlimefunItemStack("SOULBOUND_ITEM_" + (enabled ? "ENABLED" : "DISABLED"), Material.DIAMOND_SWORD, "&5Soulbound Sword");
        SoulboundItem soulboundItem = new SoulboundItem(TestUtilities.getItemGroup(plugin, "soulbound"), item, RecipeType.NULL, new ItemStack[9]);
        soulboundItem.register(plugin);

        if (!enabled) {
            Slimefun.getWorldSettingsService().setEnabled(player.getWorld(), soulboundItem, false);
        }

        player.getInventory().setItem(0, item.item());
        player.setHealth(0);

        server.getPluginManager().assertEventFired(EntityDeathEvent.class, event -> {
            // If the item is enabled, we don't want it to drop.
            return enabled == !event.getDrops().contains(item.item());
        });
        Slimefun.getRegistry().getEnabledSlimefunItems().remove(soulboundItem);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("Test if soulbound item is returned to player")
    void testItemRecover(boolean soulbound) {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Cool Sword");
        SlimefunUtils.setSoulbound(item, soulbound);
        player.getInventory().setItem(6, item);
        player.setHealth(0);
        player.respawn();

        server.getPluginManager().assertEventFired(PlayerRespawnEvent.class, event -> {
            ItemStack stack = player.getInventory().getItem(6);
            return SlimefunUtils.isItemSimilar(stack, item, true) == soulbound;
        });
    }

    @Test
    @DisplayName("Test that keepInventory does not duplicate the cursor item")
    void testKeepInventoryNoDupe() {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Cool Sword");
        SlimefunUtils.setSoulbound(item, true);
        player.setItemOnCursor(item);

        PlayerDeathEvent deathEvent = Mockito.mock(PlayerDeathEvent.class);
        Mockito.when(deathEvent.getEntity()).thenReturn(player);
        Mockito.when(deathEvent.getKeepInventory()).thenReturn(true);

        listener.onDamage(deathEvent);

        // Nothing was stored, so respawning must not hand out a duplicate
        PlayerRespawnEvent respawnEvent = Mockito.mock(PlayerRespawnEvent.class);
        Mockito.when(respawnEvent.getPlayer()).thenReturn(player);
        listener.onRespawn(respawnEvent);

        Assertions.assertFalse(player.getInventory().contains(item.getType()));
    }

    @Test
    @DisplayName("Test that dying then quitting before respawn still returns soulbound items (no dupe on later respawn)")
    void testDeathThenQuitRestoresItems() {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Cool Sword");
        SlimefunUtils.setSoulbound(item, true);
        player.setItemOnCursor(item);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(item.clone());

        PlayerDeathEvent deathEvent = Mockito.mock(PlayerDeathEvent.class);
        Mockito.when(deathEvent.getEntity()).thenReturn(player);
        Mockito.when(deathEvent.getKeepInventory()).thenReturn(false);
        Mockito.when(deathEvent.getDrops()).thenReturn(drops);

        listener.onDamage(deathEvent);
        Assertions.assertTrue(drops.isEmpty(), "Soulbound cursor item should be removed from the death drops");

        // The player dies and disconnects before ever respawning (no PlayerRespawnEvent).
        PlayerQuitEvent quitEvent = Mockito.mock(PlayerQuitEvent.class);
        Mockito.when(quitEvent.getPlayer()).thenReturn(player);
        listener.onQuit(quitEvent);

        // The soulbound item must have been returned so it is saved with the player's data
        // instead of being stuck in memory and lost.
        Assertions.assertTrue(player.getInventory().contains(item.getType()), "Soulbound item should be returned to the inventory on quit-after-death");

        // A later respawn (e.g. after reconnecting) must be a no-op: onQuit already drained the
        // pending items, so it must not hand out a duplicate.
        PlayerRespawnEvent respawnEvent = Mockito.mock(PlayerRespawnEvent.class);
        Mockito.when(respawnEvent.getPlayer()).thenReturn(player);
        listener.onRespawn(respawnEvent);

        int count = 0;

        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND_SWORD) {
                count += stack.getAmount();
            }
        }

        Assertions.assertEquals(1, count, "Respawn after quit must not duplicate the soulbound item");
    }

    @Test
    @DisplayName("Test that the soulbound cursor item is still returned without keepInventory")
    void testCursorItemRecovered() {
        PlayerMock player = server.addPlayer();
        ItemStack item = CustomItemStack.create(Material.DIAMOND_SWORD, "&4Cool Sword");
        SlimefunUtils.setSoulbound(item, true);
        player.setItemOnCursor(item);

        List<ItemStack> drops = new ArrayList<>();
        drops.add(item.clone());

        PlayerDeathEvent deathEvent = Mockito.mock(PlayerDeathEvent.class);
        Mockito.when(deathEvent.getEntity()).thenReturn(player);
        Mockito.when(deathEvent.getKeepInventory()).thenReturn(false);
        Mockito.when(deathEvent.getDrops()).thenReturn(drops);

        listener.onDamage(deathEvent);

        // The soulbound item was removed from the drops...
        Assertions.assertTrue(drops.isEmpty());

        // ... and is returned to the Player on respawn
        PlayerRespawnEvent respawnEvent = Mockito.mock(PlayerRespawnEvent.class);
        Mockito.when(respawnEvent.getPlayer()).thenReturn(player);
        listener.onRespawn(respawnEvent);

        Assertions.assertTrue(player.getInventory().contains(item.getType()));
    }

}

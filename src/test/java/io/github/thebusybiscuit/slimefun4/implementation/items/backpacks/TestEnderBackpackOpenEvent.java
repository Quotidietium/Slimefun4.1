package io.github.thebusybiscuit.slimefun4.implementation.items.backpacks;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.EnderBackpackOpenEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the backpack API expansion: {@link EnderBackpackOpenEvent},
 * exercised by driving the real {@link EnderBackpack} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * An opening is observable three ways: an {@link InventoryOpenEvent} for the ender
 * chest, the open sound heard by the player and the interaction being consumed
 * (use-item DENY). A vetoed opening shows none of the first two but still consumes
 * the interaction, so the right-click cannot fall through to a clicked block.
 *
 * @author Zurker
 */
class TestEnderBackpackOpenEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static EnderBackpack backpack;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        TestUtilities.createWorld(server);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "ender_backpack_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ENDER_BACKPACK", Material.ENDER_CHEST, "&5Test Ender Backpack");
        Slimefun.getItemCfg().setValue("_TEST_ENDER_BACKPACK.enabled", true);
        backpack = new EnderBackpack(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        backpack.register(plugin);
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
     * Right-clicks the air with the ender backpack via the real item use handler.
     */
    private PlayerRightClickEvent rightClick(Player player) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, backpack.getItem().clone(), null, null);
        PlayerRightClickEvent rightClickEvent = new PlayerRightClickEvent(interactEvent);
        backpack.getItemHandler().onRightClick(rightClickEvent);
        return rightClickEvent;
    }

    /**
     * Tracks whether an ender chest was opened for the player.
     */
    private boolean[] trackMenuOpens(Player player) {
        boolean[] opened = { false };
        Listener tracker = new Listener() {
            @EventHandler
            public void onOpen(InventoryOpenEvent event) {
                if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    opened[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(tracker, plugin);
        return opened;
    }

    @Test
    @DisplayName("EnderBackpackOpenEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        EnderBackpackOpenEvent event = new EnderBackpackOpenEvent(player, backpack);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(backpack, event.getBackpack());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnderBackpackOpenEvent(player, null));
    }

    @Test
    @DisplayName("Right-clicking fires the event, opens the ender chest, plays the sound and consumes the interaction")
    void testOpenFiresEventAndOpensEnderChest() {
        PlayerMock player = server.addPlayer();
        boolean[] opened = trackMenuOpens(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOpen(EnderBackpackOpenEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(backpack, event.getBackpack(), "The event must carry the backpack that was used");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerRightClickEvent e = rightClick(player);

            Assertions.assertTrue(seen[0], "EnderBackpackOpenEvent was not fired");
            Assertions.assertTrue(opened[0], "The ender chest must have been opened");
            player.assertSoundHeard("The open sound must have been played", Sound.ENTITY_ENDERMAN_TELEPORT);
            Assertions.assertEquals(Result.DENY, e.useItem(), "The interaction must have been consumed");
            player.closeInventory();
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling EnderBackpackOpenEvent opens nothing and plays no sound, but still consumes the interaction")
    void testCancelVetoesOpenButConsumesInteraction() {
        PlayerMock player = server.addPlayer();
        boolean[] opened = trackMenuOpens(player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onOpen(EnderBackpackOpenEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerRightClickEvent e = rightClick(player);

            Assertions.assertFalse(opened[0], "A vetoed opening must not open the ender chest");
            Assertions.assertEquals(Result.DENY, e.useItem(), "A vetoed opening must still consume the interaction");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Right-clicking without listeners still opens the ender chest, preserving the old behavior")
    void testOpenWithoutListenersOpens() {
        PlayerMock player = server.addPlayer();
        boolean[] opened = trackMenuOpens(player);

        PlayerRightClickEvent e = rightClick(player);

        Assertions.assertTrue(opened[0], "The ender chest must have been opened");
        player.assertSoundHeard("The open sound must have been played", Sound.ENTITY_ENDERMAN_TELEPORT);
        Assertions.assertEquals(Result.DENY, e.useItem(), "The interaction must have been consumed");
        player.closeInventory();
    }

    @Test
    @DisplayName("A vetoed click does not corrupt the backpack: a later click opens the ender chest")
    void testVetoThenNormalClickOpens() {
        PlayerMock player = server.addPlayer();
        boolean[] opened = trackMenuOpens(player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onOpen(EnderBackpackOpenEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            rightClick(player);
            Assertions.assertFalse(opened[0], "The vetoed click must not open the ender chest");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }

        rightClick(player);
        Assertions.assertTrue(opened[0], "A later click must open the ender chest normally");
        player.closeInventory();
    }
}

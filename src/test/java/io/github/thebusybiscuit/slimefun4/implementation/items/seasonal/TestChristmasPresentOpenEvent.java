package io.github.thebusybiscuit.slimefun4.implementation.items.seasonal;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

import io.github.thebusybiscuit.slimefun4.api.events.ChristmasPresentOpenEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the christmas present API expansion: {@link ChristmasPresentOpenEvent},
 * exercised by driving the real {@link ChristmasPresent}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent} holding a clicked {@link Block}.
 * <p>
 * The present is registered with a single gift (an apple), so the roll is deterministic.
 * Spawned gift items accumulate in the shared world, so the tests assert before/after deltas.
 *
 * @author Zurker
 */
class TestChristmasPresentOpenEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ChristmasPresent present;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "christmas_present_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CHRISTMAS_PRESENT", Material.CHEST, "&cTest Christmas Present");
        Slimefun.getItemCfg().setValue("_TEST_CHRISTMAS_PRESENT.enabled", true);
        present = new ChristmasPresent(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.APPLE));
        present.register(plugin);
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
     * Puts three presents in the player's main hand and opens one against the block at the
     * given coordinates via the real handler.
     */
    private void open(Player player, int x, int z) {
        ItemStack item = present.getItem().clone();
        item.setAmount(3);
        player.getInventory().setItemInMainHand(item);

        Block block = world.getBlockAt(x, 4, z);
        block.setType(Material.STONE);

        // MockBukkit clones the ItemStack in setItemInMainHand, so hand the event the reference
        // actually stored in the inventory for consumeItem(e.getItem()) to be visible.
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, handItem, block, BlockFace.UP);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            present.getItemHandler().onRightClick(event);
        } catch (RuntimeException ignored) {
            // firework/spawn tails not fully supported by MockBukkit - the event fired beforehand
        }
    }

    private long countItems(Material type) {
        return server.getWorlds().stream()
            .flatMap(w -> w.getEntities().stream())
            .filter(Item.class::isInstance)
            .map(Item.class::cast)
            .filter(i -> i.getItemStack().getType() == type)
            .count();
    }

    @Test
    @DisplayName("ChristmasPresentOpenEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block clicked = world.getBlockAt(0, 4, 0);
        Block spawn = clicked.getRelative(BlockFace.UP);
        ItemStack gift = new ItemStack(Material.APPLE);

        ChristmasPresentOpenEvent event = new ChristmasPresentOpenEvent(player, present, clicked, spawn, gift);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(present, event.getPresent());
        Assertions.assertEquals(clicked, event.getClickedBlock());
        Assertions.assertEquals(spawn, event.getSpawnBlock());
        Assertions.assertEquals(gift, event.getGift());
        Assertions.assertFalse(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.DIAMOND);
        event.setGift(swapped);
        Assertions.assertEquals(swapped, event.getGift());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ChristmasPresentOpenEvent(player, null, clicked, spawn, gift));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ChristmasPresentOpenEvent(player, present, null, spawn, gift));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ChristmasPresentOpenEvent(player, present, clicked, null, gift));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ChristmasPresentOpenEvent(player, present, clicked, spawn, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setGift(null));
    }

    @Test
    @DisplayName("Placing a present fires the event, consumes it and spawns the rolled gift")
    void testOpenFiresAndSpawnsGift() {
        Player player = server.addPlayer();
        long applesBefore = countItems(Material.APPLE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOpen(ChristmasPresentOpenEvent event) {
                seen[0] = true;
                Assertions.assertEquals(present, event.getPresent());
                Assertions.assertEquals(Material.APPLE, event.getGift().getType());
                Assertions.assertEquals(Material.STONE, event.getClickedBlock().getType());
                Assertions.assertEquals(event.getClickedBlock().getRelative(BlockFace.UP), event.getSpawnBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            open(player, 10, 10);

            Assertions.assertTrue(seen[0], "ChristmasPresentOpenEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The present must have been consumed");
            Assertions.assertEquals(applesBefore + 1, countItems(Material.APPLE), "The rolled gift must have been spawned");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ChristmasPresentOpenEvent keeps the present and spawns no gift")
    void testEventCancellationSkipsOpening() {
        Player player = server.addPlayer();
        long applesBefore = countItems(Material.APPLE);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onOpen(ChristmasPresentOpenEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            open(player, 20, 20);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled opening must keep the present");
            Assertions.assertEquals(applesBefore, countItems(Material.APPLE), "A cancelled opening must not spawn a gift");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the gift via setGift replaces the spawned gift")
    void testGiftSwap() {
        Player player = server.addPlayer();
        long diamondsBefore = countItems(Material.DIAMOND);

        Listener swapping = new Listener() {
            @EventHandler
            public void onOpen(ChristmasPresentOpenEvent event) {
                event.setGift(new ItemStack(Material.DIAMOND));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            open(player, 30, 30);

            Assertions.assertEquals(diamondsBefore + 1, countItems(Material.DIAMOND), "The swapped gift must have been spawned");
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Placing a present without listeners still spawns the gift, preserving the old behavior")
    void testOpenWithoutListenersStillSpawns() {
        Player player = server.addPlayer();
        long applesBefore = countItems(Material.APPLE);

        open(player, 40, 40);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The present must have been consumed");
        Assertions.assertEquals(applesBefore + 1, countItems(Material.APPLE), "The rolled gift must have been spawned");
    }

    @Test
    @DisplayName("Right-clicking air without a clicked block neither fires the event nor opens the present")
    void testRightClickAirDoesNothing() {
        Player player = server.addPlayer();
        long applesBefore = countItems(Material.APPLE);

        ItemStack item = present.getItem().clone();
        item.setAmount(3);
        player.getInventory().setItemInMainHand(item);
        ItemStack handItem = player.getInventory().getItemInMainHand();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOpen(ChristmasPresentOpenEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
            present.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));

            Assertions.assertFalse(seen[0], "ChristmasPresentOpenEvent must not fire without a clicked block");
            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "Nothing must be consumed");
            Assertions.assertEquals(applesBefore, countItems(Material.APPLE), "No gift must be spawned");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

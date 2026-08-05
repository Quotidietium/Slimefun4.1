package io.github.thebusybiscuit.slimefun4.implementation.items.seasonal;

import org.bukkit.Material;
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

import io.github.thebusybiscuit.slimefun4.api.events.EasterEggOpenEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the easter egg API expansion: {@link EasterEggOpenEvent}, exercised
 * by driving the real {@link EasterEgg} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * The egg is registered with a single gift (an apple), so the roll is deterministic.
 * Spawned gift items accumulate in the shared world, so the tests assert before/after deltas.
 *
 * @author Zurker
 */
class TestEasterEggOpenEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static EasterEgg easterEgg;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "easter_egg_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_EASTER_EGG", Material.EGG, "&eTest Easter Egg");
        Slimefun.getItemCfg().setValue("_TEST_EASTER_EGG.enabled", true);
        easterEgg = new EasterEgg(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.EGG), new ItemStack(Material.APPLE));
        easterEgg.register(plugin);
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
     * Puts three eggs in the player's main hand and opens one via the real handler.
     */
    private void open(Player player) {
        ItemStack egg = easterEgg.getItem().clone();
        egg.setAmount(3);
        player.getInventory().setItemInMainHand(egg);

        // MockBukkit clones the ItemStack in setItemInMainHand, so hand the event the reference
        // actually stored in the inventory for consumeItem(e.getItem()) to be visible.
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            easterEgg.getItemHandler().onRightClick(event);
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
    @DisplayName("EasterEggOpenEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack gift = new ItemStack(Material.APPLE);

        EasterEggOpenEvent event = new EasterEggOpenEvent(player, easterEgg, gift);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(easterEgg, event.getEasterEgg());
        Assertions.assertEquals(gift, event.getGift());
        Assertions.assertFalse(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.DIAMOND);
        event.setGift(swapped);
        Assertions.assertEquals(swapped, event.getGift());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EasterEggOpenEvent(player, null, gift));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EasterEggOpenEvent(player, easterEgg, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setGift(null));
    }

    @Test
    @DisplayName("Opening an egg fires the event, consumes it and spawns the rolled gift")
    void testOpenFiresAndSpawnsGift() {
        Player player = server.addPlayer();
        long applesBefore = countItems(Material.APPLE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOpen(EasterEggOpenEvent event) {
                seen[0] = true;
                Assertions.assertEquals(easterEgg, event.getEasterEgg());
                Assertions.assertEquals(Material.APPLE, event.getGift().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            open(player);

            Assertions.assertTrue(seen[0], "EasterEggOpenEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The egg must have been consumed");
            Assertions.assertEquals(applesBefore + 1, countItems(Material.APPLE), "The rolled gift must have been spawned");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling EasterEggOpenEvent keeps the egg and spawns no gift")
    void testEventCancellationSkipsOpening() {
        Player player = server.addPlayer();
        long applesBefore = countItems(Material.APPLE);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onOpen(EasterEggOpenEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            open(player);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled opening must keep the egg");
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
            public void onOpen(EasterEggOpenEvent event) {
                event.setGift(new ItemStack(Material.DIAMOND));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            open(player);

            Assertions.assertEquals(diamondsBefore + 1, countItems(Material.DIAMOND), "The swapped gift must have been spawned");
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Opening an egg without listeners still spawns the gift, preserving the old behavior")
    void testOpenWithoutListenersStillSpawns() {
        Player player = server.addPlayer();
        long applesBefore = countItems(Material.APPLE);

        open(player);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The egg must have been consumed");
        Assertions.assertEquals(applesBefore + 1, countItems(Material.APPLE), "The rolled gift must have been spawned");
    }
}

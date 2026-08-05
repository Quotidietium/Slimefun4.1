package io.github.thebusybiscuit.slimefun4.implementation.items.altar;

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

import io.github.thebusybiscuit.slimefun4.api.events.PedestalItemPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the ancient pedestal API expansion: {@link PedestalItemPlaceEvent},
 * exercised by driving the real {@link AncientPedestal#placeItem} directly. The downstream
 * ArmorStand/item-spawn tail is not fully supported by MockBukkit, so a RuntimeException from
 * that tail is ignored here - the event was fired and the hand consumed beforehand.
 *
 * @author Zurker
 */
class TestPedestalItemPlaceEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AncientPedestal pedestal;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "pedestal_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_PEDESTAL", Material.NETHER_BRICK, "&fTest Pedestal");
        // The pedestal is left unregistered: its BlockDispenseHandler fails framework validation
        // for a test item, but placeItem (and the event) only need the instance itself.
        pedestal = new AncientPedestal(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.AIR));
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Block placePedestal(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.STONE);
        return b;
    }

    private void place(Player player, Block pedestalBlock) {
        try {
            pedestal.placeItem(player, pedestalBlock);
        } catch (RuntimeException ignored) {
            // ArmorStand/spawn tail not fully supported by MockBukkit - see class javadoc
        }
    }

    @Test
    @DisplayName("PedestalItemPlaceEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placePedestal(1, 1);
        ItemStack item = new ItemStack(Material.DIAMOND);

        PedestalItemPlaceEvent event = new PedestalItemPlaceEvent(player, pedestal, b, item);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(pedestal, event.getPedestal());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PedestalItemPlaceEvent(player, null, b, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PedestalItemPlaceEvent(player, pedestal, null, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PedestalItemPlaceEvent(player, pedestal, b, null));
    }

    @Test
    @DisplayName("Placing an item fires the event and consumes the hand item")
    void testPlaceFiresAndConsumes() {
        Player player = server.addPlayer();
        Block b = placePedestal(10, 10);
        ItemStack hand = new ItemStack(Material.DIAMOND, 3);
        player.getInventory().setItemInMainHand(hand);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPlace(PedestalItemPlaceEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pedestal, event.getPedestal());
                Assertions.assertEquals(b, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            place(player, b);

            Assertions.assertTrue(seen[0], "PedestalItemPlaceEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The hand item must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling PedestalItemPlaceEvent keeps the hand item untouched")
    void testEventCancellationKeepsItem() {
        Player player = server.addPlayer();
        Block b = placePedestal(20, 20);
        ItemStack hand = new ItemStack(Material.DIAMOND, 3);
        player.getInventory().setItemInMainHand(hand);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPlace(PedestalItemPlaceEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            place(player, b);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled place must keep the hand item");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Placing without listeners still consumes, preserving the old behavior")
    void testPlaceWithoutListenersStillConsumes() {
        Player player = server.addPlayer();
        Block b = placePedestal(30, 30);
        ItemStack hand = new ItemStack(Material.DIAMOND, 3);
        player.getInventory().setItemInMainHand(hand);

        place(player, b);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The hand item must have been consumed");
    }
}

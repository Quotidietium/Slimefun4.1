package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
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
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;

import io.github.thebusybiscuit.slimefun4.api.events.ItemMagnetPullEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.InfusedMagnet;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the infused magnet API expansion:
 * {@link ItemMagnetPullEvent}, exercised through the real {@link InfusedMagnetTask#executeTask()}
 * pull path with real item entities.
 *
 * @author Zurker
 */
class TestItemMagnetPullEvent {

    private static final double RADIUS = 6.0;

    private static ServerMock server;
    private static Slimefun plugin;

    private static InfusedMagnet magnet;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "magnet_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_INFUSED_MAGNET", Material.NETHER_STAR, "&fTest Infused Magnet");
        Slimefun.getItemCfg().setValue("TEST_INFUSED_MAGNET.enabled", true);
        magnet = new InfusedMagnet(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        magnet.register(plugin);
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
     * Spawns a dropped item at the given offset from the player and registers it in the
     * player's own world, so {@code getNearbyEntities} finds it there.
     */
    private Item dropItem(Player player, double dx, double dz, Material material) {
        Location origin = player.getLocation();
        ItemEntityMock item = new ItemEntityMock(server, UUID.randomUUID(), new ItemStack(material));
        item.setLocation(new Location(origin.getWorld(), origin.getX() + dx, origin.getY(), origin.getZ() + dz));
        item.setPickupDelay(0);
        server.registerEntity(item);
        return item;
    }

    /**
     * Runs the magnet task for a player at the origin with the test radius.
     */
    private void runMagnet(Player player) {
        InfusedMagnetTask task = new InfusedMagnetTask(player, magnet, RADIUS);
        task.executeTask();
    }

    @Test
    @DisplayName("ItemMagnetPullEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Item item = dropItem(player, 1, 1, Material.DIAMOND);

        ItemMagnetPullEvent event = new ItemMagnetPullEvent(player, magnet, item);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(magnet, event.getMagnet());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(player.getLocation(), event.getDestination(), "The pull must default to the player's location");
        Assertions.assertFalse(event.isCancelled());

        // The pull can be redirected
        Location vault = new Location(player.getWorld(), 100, 65, 100);
        event.setDestination(vault);
        Assertions.assertEquals(vault, event.getDestination());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDestination(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ItemMagnetPullEvent(player, null, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ItemMagnetPullEvent(player, magnet, null));
    }

    @Test
    @DisplayName("The magnet pulls nearby items and fires an event per item")
    void testPullFiresAndTeleports() {
        Player player = server.addPlayer();

        Item diamond = dropItem(player, 3, 0, Material.DIAMOND);
        Item gold = dropItem(player, 0, 3, Material.GOLD_INGOT);

        boolean[] seenDiamond = { false };
        boolean[] seenGold = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPull(ItemMagnetPullEvent event) {
                if (event.getItem() == diamond) {
                    seenDiamond[0] = true;
                    Assertions.assertEquals(magnet, event.getMagnet());
                } else if (event.getItem() == gold) {
                    seenGold[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runMagnet(player);

            Assertions.assertTrue(seenDiamond[0], "ItemMagnetPullEvent was not fired for the diamond");
            Assertions.assertTrue(seenGold[0], "ItemMagnetPullEvent was not fired for the gold");
            Assertions.assertEquals(player.getLocation(), diamond.getLocation(), "The diamond must have been pulled to the player");
            Assertions.assertEquals(player.getLocation(), gold.getLocation(), "The gold must have been pulled to the player");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ItemMagnetPullEvent leaves that item where it is")
    void testEventCancellationSkipsItem() {
        Player player = server.addPlayer();

        Location diamondStart = new Location(player.getWorld(), player.getLocation().getX() + 3, player.getLocation().getY(), player.getLocation().getZ());
        Location goldStart = new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ() + 3);
        Item diamond = dropItem(player, 3, 0, Material.DIAMOND);
        Item gold = dropItem(player, 0, 3, Material.GOLD_INGOT);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPull(ItemMagnetPullEvent event) {
                if (event.getItem() == diamond) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            runMagnet(player);

            Assertions.assertEquals(diamondStart, diamond.getLocation(), "The cancelled diamond must have stayed where it was");
            Assertions.assertEquals(player.getLocation(), gold.getLocation(), "The gold must still have been pulled");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Redirecting the destination teleports the item there instead of to the player")
    void testSetDestinationRedirectsPull() {
        Player player = server.addPlayer();

        Item diamond = dropItem(player, 3, 0, Material.DIAMOND);
        Item gold = dropItem(player, 0, 3, Material.GOLD_INGOT);
        Location vault = new Location(player.getWorld(), player.getLocation().getX() + 2, player.getLocation().getY(), player.getLocation().getZ() + 2);

        Listener redirecting = new Listener() {
            @EventHandler
            public void onPull(ItemMagnetPullEvent event) {
                if (event.getItem() == diamond) {
                    Assertions.assertEquals(player.getLocation(), event.getDestination(), "The destination must default to the player");
                    event.setDestination(vault);
                }
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            runMagnet(player);

            Assertions.assertEquals(vault, diamond.getLocation(), "The diamond must have been teleported to the redirected destination");
            Assertions.assertEquals(player.getLocation(), gold.getLocation(), "The gold must still have been pulled to the player");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Pulling without listeners still teleports, preserving the old behavior")
    void testPullWithoutListenersStillTeleports() {
        Player player = server.addPlayer();

        Item diamond = dropItem(player, 3, 0, Material.DIAMOND);

        runMagnet(player);

        Assertions.assertEquals(player.getLocation(), diamond.getLocation(), "The diamond must have been pulled to the player");
    }


    @Test
    @DisplayName("Items flagged as no-pickup are never pulled")
    void testNoPickupItemNotPulled() {
        Player player = server.addPlayer();

        Item diamond = dropItem(player, 3, 0, Material.DIAMOND);
        Location start = diamond.getLocation().clone();
        io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.markAsNoPickup(diamond, "test");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPull(ItemMagnetPullEvent event) {
                if (event.getItem() == diamond) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runMagnet(player);

            Assertions.assertFalse(seen[0], "No event must be fired for a no-pickup item");
            Assertions.assertEquals(start, diamond.getLocation(), "A no-pickup item must not be pulled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

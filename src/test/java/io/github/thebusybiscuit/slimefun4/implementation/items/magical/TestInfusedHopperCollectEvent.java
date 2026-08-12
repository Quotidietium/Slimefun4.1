package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
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

import io.github.thebusybiscuit.slimefun4.api.events.InfusedHopperCollectEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the infused hopper API expansion:
 * {@link InfusedHopperCollectEvent}, exercised by driving the real {@link InfusedHopper}
 * {@link BlockTicker} against a {@link BlockStorage}-backed hopper block with real item
 * entities in range.
 * <p>
 * A tick teleports every valid dropped item onto the hopper, so tests assert the outcome
 * end-to-end: a cancelled event leaves that item where it is.
 *
 * @author Zurker
 */
class TestInfusedHopperCollectEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static InfusedHopper infusedHopper;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "infused_hopper_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_INFUSED_HOPPER", Material.HOPPER, "&7Test Infused Hopper");
        Slimefun.getItemCfg().setValue("_TEST_INFUSED_HOPPER.enabled", true);
        infusedHopper = new InfusedHopper(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        infusedHopper.register(plugin);
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
     * Places an infused hopper block at a high, flat y to stay clear of the terrain.
     */
    private Block placeHopper(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.HOPPER);
        BlockStorage.addBlockInfo(b, "id", "_TEST_INFUSED_HOPPER");
        return b;
    }

    /**
     * Spawns a dropped item and registers it in the world, so {@code getNearbyEntities}
     * finds it there.
     */
    private Item dropItem(Location location, Material material) {
        ItemEntityMock item = new ItemEntityMock(server, UUID.randomUUID(), new ItemStack(material));
        item.setLocation(location);
        item.setPickupDelay(0);
        server.registerEntity(item);
        return item;
    }

    /**
     * Runs one tick of the hopper's real {@link BlockTicker}.
     */
    private void tick(Block b) {
        infusedHopper.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, infusedHopper, BlockStorage.getLocationInfo(b.getLocation())));
    }

    /**
     * The location the hopper teleports items onto.
     * <p>
     * Must be captured BEFORE the tick: {@code BlockMock.getLocation()} returns a shared
     * mutable {@link Location} and the ticker offsets it in place, so computing the
     * expectation after the tick would add the offset twice.
     */
    private Location target(Block b) {
        return b.getLocation().add(0.5, 1.2, 0.5);
    }

    @Test
    @DisplayName("InfusedHopperCollectEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = placeHopper(1, 1);
        Item item = dropItem(new Location(world, 3, 60, 1), Material.DIAMOND);

        InfusedHopperCollectEvent event = new InfusedHopperCollectEvent(infusedHopper, b, item);

        Assertions.assertEquals(infusedHopper, event.getHopper());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertFalse(event.isCancelled());

        // The collection defaults to the point above the hopper
        Assertions.assertEquals(new Location(world, 1.5, 61.2, 1.5), event.getDestination(), "The collection must default to the point above the hopper");

        // And can be redirected
        Location vault = new Location(world, 50, 60, 50);
        event.setDestination(vault);
        Assertions.assertEquals(vault, event.getDestination());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDestination(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InfusedHopperCollectEvent(null, b, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InfusedHopperCollectEvent(infusedHopper, null, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InfusedHopperCollectEvent(infusedHopper, b, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InfusedHopperCollectEvent(infusedHopper, b, item, null));
    }

    @Test
    @DisplayName("A tick with an item in range fires the event and teleports the item onto the hopper")
    void testTickFiresEventAndCollects() {
        Block b = placeHopper(100, 100);
        Location target = target(b);
        Item item = dropItem(new Location(world, 102, 60, 100), Material.DIAMOND);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(InfusedHopperCollectEvent event) {
                seen[0] = true;
                Assertions.assertEquals(infusedHopper, event.getHopper());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(item, event.getItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "InfusedHopperCollectEvent was not fired");
            Assertions.assertEquals(target, item.getLocation(), "The item must have been teleported onto the hopper");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling InfusedHopperCollectEvent leaves the item where it is")
    void testCancelKeepsItem() {
        Block b = placeHopper(200, 200);
        Item item = dropItem(new Location(world, 202, 60, 200), Material.DIAMOND);
        Location start = item.getLocation().clone();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCollect(InfusedHopperCollectEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            Assertions.assertEquals(start, item.getLocation(), "A cancelled collection must leave the item in place");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Collecting without listeners still teleports, preserving the old behavior")
    void testCollectWithoutListenersStillCollects() {
        Block b = placeHopper(300, 300);
        Location target = target(b);
        Item item = dropItem(new Location(world, 302, 60, 300), Material.DIAMOND);

        tick(b);

        Assertions.assertEquals(target, item.getLocation(), "The item must have been teleported onto the hopper");
    }

    @Test
    @DisplayName("Redirecting the destination teleports the item there instead of onto the hopper")
    void testSetDestinationRedirectsCollection() {
        Block b = placeHopper(600, 600);
        Location target = target(b);
        Item diamond = dropItem(new Location(world, 602, 60, 600), Material.DIAMOND);
        Item gold = dropItem(new Location(world, 600, 60, 602), Material.GOLD_INGOT);
        Location vault = new Location(world, 650, 60, 650);

        Listener redirecting = new Listener() {
            @EventHandler
            public void onCollect(InfusedHopperCollectEvent event) {
                if (event.getItem() == diamond) {
                    Assertions.assertEquals(target, event.getDestination(), "The destination must default to the point above the hopper");
                    event.setDestination(vault);
                }
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            tick(b);

            Assertions.assertEquals(vault, diamond.getLocation(), "The diamond must have been teleported to the redirected destination");
            Assertions.assertEquals(target, gold.getLocation(), "The gold must still have been collected onto the hopper");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("An item out of range fires no event and stays in place")
    void testOutOfRangeFiresNothing() {
        Block b = placeHopper(400, 400);
        Item item = dropItem(new Location(world, 410, 60, 400), Material.DIAMOND);
        Location start = item.getLocation().clone();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(InfusedHopperCollectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an item out of range");
            Assertions.assertEquals(start, item.getLocation(), "An item out of range must not be teleported");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An item with a pickup delay fires no event and stays in place")
    void testPickupDelayFiresNothing() {
        Block b = placeHopper(500, 500);
        Item item = dropItem(new Location(world, 502, 60, 500), Material.DIAMOND);
        item.setPickupDelay(40);
        Location start = item.getLocation().clone();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(InfusedHopperCollectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an item with a pickup delay");
            Assertions.assertEquals(start, item.getLocation(), "An item with a pickup delay must not be teleported");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

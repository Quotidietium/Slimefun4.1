package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeRestrictionEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoNode;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

/**
 * Regression coverage for the cargo node placement API expansion:
 * {@link CargoNodeRestrictionEvent}, exercised through the real {@link CargoNodeListener}
 * top/bottom placement restriction.
 *
 * @author Zurker
 */
class TestCargoNodeRestrictionEvent {

    /**
     * A minimal {@link CargoNode} test item.
     */
    static class CargoNodeMockItem extends MockSlimefunItem implements CargoNode {

        CargoNodeMockItem(ItemGroup itemGroup, ItemStack item, String id) {
            super(itemGroup, item, id);
        }

        @Override
        public int getSelectedChannel(@Nonnull Block b) {
            return 0;
        }

        @Override
        public boolean hasItemFilter() {
            return false;
        }
    }

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static CargoNodeMockItem cargoNode;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new CargoNodeListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "cargo_node_test");
        Slimefun.getItemCfg().setValue("TEST_CARGO_NODE.enabled", true);
        cargoNode = new CargoNodeMockItem(itemGroup, new ItemStack(Material.POLISHED_BLACKSTONE_BUTTON), "TEST_CARGO_NODE");
        cargoNode.register(plugin);
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
     * Fires a placement of the cargo node from the player onto the given face of the
     * support block and returns the event for assertions.
     */
    private BlockPlaceEvent placeNode(Player player, Block support, BlockFace face) {
        Block placed = support.getRelative(face);
        placed.setType(Material.AIR);
        BlockState replacedState = placed.getState();
        ItemStack hand = cargoNode.getItem().clone();

        BlockPlaceEvent event = new BlockPlaceEvent(placed, replacedState, support, hand, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("CargoNodeRestrictionEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block placed = world.getBlockAt(1, 1, 1);
        Block against = world.getBlockAt(1, 0, 1);
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(placed, placed.getState(), against, cargoNode.getItem().clone(), player, true, EquipmentSlot.HAND);

        CargoNodeRestrictionEvent event = new CargoNodeRestrictionEvent(player, cargoNode, placed, against, placeEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(cargoNode, event.getCargoNode());
        Assertions.assertEquals(placed, event.getBlock());
        Assertions.assertEquals(against, event.getBlockAgainst());
        Assertions.assertEquals(placeEvent, event.getPlaceEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeRestrictionEvent(player, null, placed, against, placeEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeRestrictionEvent(player, cargoNode, null, against, placeEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeRestrictionEvent(player, cargoNode, placed, null, placeEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CargoNodeRestrictionEvent(player, cargoNode, placed, against, null));
    }

    @Test
    @DisplayName("Placing a cargo node on top of a block fires the event and is cancelled")
    void testTopPlacementFiresAndCancels() {
        Player player = server.addPlayer();
        Block support = world.getBlockAt(10, 1, 10);
        support.setType(Material.IRON_BLOCK);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestrict(CargoNodeRestrictionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(cargoNode, event.getCargoNode());
                Assertions.assertEquals(support.getRelative(BlockFace.UP), event.getBlock());
                Assertions.assertEquals(support, event.getBlockAgainst());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockPlaceEvent event = placeNode(player, support, BlockFace.UP);

            Assertions.assertTrue(seen[0], "CargoNodeRestrictionEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "A top placement must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling CargoNodeRestrictionEvent allows the placement")
    void testEventCancellationAllowsPlacement() {
        Player player = server.addPlayer();
        Block support = world.getBlockAt(20, 1, 20);
        support.setType(Material.IRON_BLOCK);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRestrict(CargoNodeRestrictionEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            BlockPlaceEvent event = placeNode(player, support, BlockFace.UP);

            Assertions.assertFalse(event.isCancelled(), "A vetoed restriction must allow the placement");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Placing a cargo node on the side of a block fires no event and is allowed")
    void testSidePlacementFiresNothing() {
        Player player = server.addPlayer();
        Block support = world.getBlockAt(30, 1, 30);
        support.setType(Material.IRON_BLOCK);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestrict(CargoNodeRestrictionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockPlaceEvent event = placeNode(player, support, BlockFace.NORTH);

            Assertions.assertFalse(seen[0], "No event must be fired for a side placement");
            Assertions.assertFalse(event.isCancelled(), "A side placement must be allowed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Placing a non-cargo-node item on top fires no event")
    void testNonCargoNodeFiresNothing() {
        Player player = server.addPlayer();
        Block support = world.getBlockAt(40, 1, 40);
        support.setType(Material.IRON_BLOCK);

        Block placed = support.getRelative(BlockFace.UP);
        placed.setType(Material.AIR);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRestrict(CargoNodeRestrictionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockPlaceEvent event = new BlockPlaceEvent(placed, placed.getState(), support, new ItemStack(Material.STONE), player, true, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-cargo-node item");
            Assertions.assertFalse(event.isCancelled(), "A non-cargo-node placement must be left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Restriction without listeners still cancels, preserving the old behavior")
    void testRestrictionWithoutListenersStillCancels() {
        Player player = server.addPlayer();
        Block support = world.getBlockAt(50, 1, 50);
        support.setType(Material.IRON_BLOCK);

        BlockPlaceEvent event = placeNode(player, support, BlockFace.UP);

        Assertions.assertTrue(event.isCancelled(), "The top placement must have been cancelled");
    }
}

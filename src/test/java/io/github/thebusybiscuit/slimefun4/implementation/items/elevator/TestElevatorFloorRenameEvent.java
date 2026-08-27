package io.github.thebusybiscuit.slimefun4.implementation.items.elevator;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.ElevatorFloorRenameEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the elevator API expansion: {@link ElevatorFloorRenameEvent},
 * exercised by driving the package-private {@link ElevatorPlate#renameFloor} against a
 * {@link BlockStorage}-backed plate.
 * <p>
 * The rename stores the new name in BlockStorage (with legacy section signs replaced
 * by '&amp;') and re-opens the floor editor, so an {@link InventoryOpenEvent} tracker
 * is the completion signal: a vetoed rename stores nothing and opens nothing.
 * <p>
 * The event carries the raw typed name; the section-sign sanitization happens after
 * the event, so listeners see (and may replace) exactly what the player typed.
 *
 * @author Zurker
 */
class TestElevatorFloorRenameEvent {

    // ElevatorPlate#DATA_KEY, asserted literally because it is private
    private static final String FLOOR_KEY = "floor";

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ElevatorPlate plate;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "elevator_rename_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ELEVATOR_PLATE", Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "&fTest Elevator Plate");
        Slimefun.getItemCfg().setValue("_TEST_ELEVATOR_PLATE.enabled", true);
        plate = new ElevatorPlate(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
        plate.register(plugin);
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
     * Places the plate as a real block backed by {@link BlockStorage} with the given
     * floor name, mirroring what the place handler stores.
     */
    private Block placePlate(Player owner, int x, int z, String floorName) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        BlockStorage.addBlockInfo(b, "id", plate.getId(), true);
        BlockStorage.addBlockInfo(b, "owner", owner.getUniqueId().toString(), true);

        if (floorName != null) {
            BlockStorage.addBlockInfo(b, FLOOR_KEY, floorName, true);
        }

        return b;
    }

    /**
     * Tracks whether a floor editor menu was opened for the player.
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

    private String storedName(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), FLOOR_KEY);
    }

    @Test
    @DisplayName("ElevatorFloorRenameEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(1, 60, 1);

        ElevatorFloorRenameEvent event = new ElevatorFloorRenameEvent(player, b, "&7Old", "New Floor");

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(b, event.getElevator());
        Assertions.assertEquals("&7Old", event.getPreviousName());
        Assertions.assertEquals("New Floor", event.getNewName());
        Assertions.assertFalse(event.isCancelled());

        event.setNewName("Other Floor");
        Assertions.assertEquals("Other Floor", event.getNewName());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElevatorFloorRenameEvent(player, null, "&7Old", "New Floor"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElevatorFloorRenameEvent(player, b, "&7Old", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNewName(null));
    }

    @Test
    @DisplayName("Renaming after the plate was broken writes no ghost floor data onto the replacement block")
    void testRenameAfterPlateBrokenWritesNothing() {
        Player player = server.addPlayer();

        // A plain block without any BlockStorage id - the plate was broken while the
        // player typed the name and another block now occupies the spot
        Block b = world.getBlockAt(50, 60, 50);
        b.setType(Material.STONE);

        plate.renameFloor(player, b, "Ghost Floor");

        Assertions.assertNull(storedName(b), "No floor data must be written to a block that is no longer an elevator plate");
    }

    @Test
    @DisplayName("Renaming fires the event with the raw typed name and stores the sanitized name")
    void testRenameFiresEventAndStoresName() {
        Player player = server.addPlayer();
        Block b = placePlate(player, 10, 10, "&7Old Floor");
        boolean[] opened = trackMenuOpens(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRename(ElevatorFloorRenameEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(b, event.getElevator());
                Assertions.assertEquals("&7Old Floor", event.getPreviousName());
                Assertions.assertEquals("§cRed Floor", event.getNewName(), "The event must carry the raw typed name, before sanitization");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            plate.renameFloor(player, b, "§cRed Floor");

            Assertions.assertTrue(seen[0], "ElevatorFloorRenameEvent was not fired");
            Assertions.assertEquals("&cRed Floor", storedName(b), "The stored name must have legacy section signs replaced");
            Assertions.assertTrue(opened[0], "The floor editor must have been re-opened");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ElevatorFloorRenameEvent keeps the old name and opens no editor")
    void testCancelKeepsOldNameAndSkipsEditor() {
        Player player = server.addPlayer();
        Block b = placePlate(player, 20, 20, "&7Old Floor");
        boolean[] opened = trackMenuOpens(player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRename(ElevatorFloorRenameEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            plate.renameFloor(player, b, "New Floor");

            Assertions.assertEquals("&7Old Floor", storedName(b), "A vetoed rename must keep the old name");
            Assertions.assertFalse(opened[0], "A vetoed rename must not re-open the editor");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Renaming without listeners still stores the name, preserving the old behavior")
    void testRenameWithoutListenersStores() {
        Player player = server.addPlayer();
        Block b = placePlate(player, 30, 30, "&7Old Floor");
        boolean[] opened = trackMenuOpens(player);

        plate.renameFloor(player, b, "&bSilent Floor");

        Assertions.assertEquals("&bSilent Floor", storedName(b), "The new name must have been stored");
        Assertions.assertTrue(opened[0], "The floor editor must have been re-opened");
    }

    @Test
    @DisplayName("Redirecting via setNewName changes the stored name")
    void testSetNewNameRedirect() {
        Player player = server.addPlayer();
        Block b = placePlate(player, 40, 40, "&7Old Floor");

        Listener redirecting = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onRename(ElevatorFloorRenameEvent event) {
                event.setNewName("§2Lab");
            }
        };
        Listener checking = new Listener() {
            @EventHandler(priority = EventPriority.NORMAL)
            public void onRename(ElevatorFloorRenameEvent event) {
                Assertions.assertEquals("§2Lab", event.getNewName(), "The redirected name must be visible to later listeners");
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);
        server.getPluginManager().registerEvents(checking, plugin);

        try {
            plate.renameFloor(player, b, "New Floor");

            Assertions.assertEquals("&2Lab", storedName(b), "The redirected name must have been stored, sanitized like a typed name");
        } finally {
            HandlerList.unregisterAll(redirecting);
            HandlerList.unregisterAll(checking);
        }
    }

    @Test
    @DisplayName("Renaming a never-named floor exposes a null previous name")
    void testUnnamedFloorPreviousNameNull() {
        Player player = server.addPlayer();
        Block b = placePlate(player, 50, 50, null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRename(ElevatorFloorRenameEvent event) {
                seen[0] = true;
                Assertions.assertNull(event.getPreviousName(), "A never-named floor must expose a null previous name");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            plate.renameFloor(player, b, "First Name");

            Assertions.assertTrue(seen[0], "ElevatorFloorRenameEvent was not fired");
            Assertions.assertEquals("First Name", storedName(b), "The new name must have been stored");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Renaming after the plate was re-placed by someone else is rejected")
    void testRenameAfterPlateReplacedByOtherOwner() {
        Player originalOwner = server.addPlayer();
        Player newOwner = server.addPlayer();
        Block b = placePlate(newOwner, 60, 60, "&7Old Floor");
        boolean[] opened = trackMenuOpens(originalOwner);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRename(ElevatorFloorRenameEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // The original owner's pending chat input arrives after the plate changed hands
            plate.renameFloor(originalOwner, b, "Stolen Floor");

            Assertions.assertFalse(seen[0], "ElevatorFloorRenameEvent must not fire for a non-owner rename");
            Assertions.assertEquals("&7Old Floor", storedName(b), "A non-owner rename must not change the floor name");
            Assertions.assertFalse(opened[0], "A non-owner rename must not re-open the editor");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Floor selector page count has no empty trailing page")
    void testFloorSelectorPageCount() {
        /*
         * The old formula 1 + floors / 27 produced an extra empty page whenever the
         * floor count was an exact multiple of the page size (e.g. exactly 27 floors).
         */
        org.junit.jupiter.api.Assertions.assertEquals(1, io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate.getPageCount(2), "Two floors fit on one page");
        org.junit.jupiter.api.Assertions.assertEquals(1, io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate.getPageCount(27), "Exactly 27 floors must fill exactly one page");
        org.junit.jupiter.api.Assertions.assertEquals(2, io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate.getPageCount(28));
        org.junit.jupiter.api.Assertions.assertEquals(2, io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate.getPageCount(54), "Exactly 54 floors must fill exactly two pages");
        org.junit.jupiter.api.Assertions.assertEquals(3, io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate.getPageCount(55));
    }
}

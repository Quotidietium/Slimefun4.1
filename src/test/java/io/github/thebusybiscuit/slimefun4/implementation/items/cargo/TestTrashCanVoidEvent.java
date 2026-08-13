package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.TrashCanVoidEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the trash can API expansion: {@link TrashCanVoidEvent}, exercised
 * by driving the real {@link TrashCan} {@link BlockTicker} against a {@link BlockStorage}-backed
 * trash can block with items in its input slots.
 * <p>
 * A tick voids every occupied input slot, so tests assert the outcome end-to-end: a cancelled
 * event keeps every item in place.
 *
 * @author Zurker
 */
class TestTrashCanVoidEvent {

    private static final int INPUT_SLOT = 10;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static TrashCan trashCan;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "trash_can_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_TRASH_CAN", Material.CHEST, "&7Test Trash Can");
        Slimefun.getItemCfg().setValue("_TEST_TRASH_CAN.enabled", true);
        trashCan = new TrashCan(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        trashCan.register(plugin);
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
     * Places a trash can block and returns its menu.
     */
    private BlockMenu placeTrashCan(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.CHEST);
        BlockStorage.addBlockInfo(b, "id", "_TEST_TRASH_CAN");
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the trash can's real {@link BlockTicker}.
     */
    private void tick(BlockMenu menu) {
        Block b = menu.getBlock();
        trashCan.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, trashCan, BlockStorage.getLocationInfo(b.getLocation())));
    }

    @Test
    @DisplayName("TrashCanVoidEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        ItemStack dirt = new ItemStack(Material.DIRT);

        TrashCanVoidEvent event = new TrashCanVoidEvent(trashCan, b, List.of(dirt));

        Assertions.assertEquals(trashCan, event.getTrashCan());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(1, event.getItems().size());
        Assertions.assertEquals(dirt, event.getItems().get(0));
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getItems().add(dirt), "The items view must be unmodifiable");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TrashCanVoidEvent(null, b, List.of(dirt)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TrashCanVoidEvent(trashCan, null, List.of(dirt)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TrashCanVoidEvent(trashCan, b, null));
    }

    @Test
    @DisplayName("spareItem validates its argument and is reported via getSparedItems")
    void testSpareItemValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        ItemStack dirt = new ItemStack(Material.DIRT);
        ItemStack stone = new ItemStack(Material.STONE);

        TrashCanVoidEvent event = new TrashCanVoidEvent(trashCan, b, List.of(dirt));

        Assertions.assertTrue(event.getSparedItems().isEmpty());

        event.spareItem(dirt);
        Assertions.assertEquals(1, event.getSparedItems().size());
        Assertions.assertEquals(dirt, event.getSparedItems().get(0));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getSparedItems().add(stone), "The spared view must be unmodifiable");

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.spareItem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.spareItem(stone), "Sparing an item that is not voided must be rejected");
    }

    @Test
    @DisplayName("A spared item stays in its slot while the rest is voided")
    void testSparedItemStays() {
        BlockMenu menu = placeTrashCan(60, 60);
        ItemStack dirt = new ItemStack(Material.DIRT);
        menu.replaceExistingItem(INPUT_SLOT, dirt);
        menu.replaceExistingItem(INPUT_SLOT + 1, new ItemStack(Material.COBBLESTONE, 3));

        Listener sparing = new Listener() {
            @EventHandler
            public void onVoid(TrashCanVoidEvent event) {
                for (ItemStack item : event.getItems()) {
                    if (item.getType() == Material.DIRT) {
                        event.spareItem(item);
                    }
                }
            }
        };
        server.getPluginManager().registerEvents(sparing, plugin);

        try {
            tick(menu);

            ItemStack kept = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(kept, "The spared dirt must have been kept");
            Assertions.assertEquals(Material.DIRT, kept.getType());
            Assertions.assertNull(menu.getItemInSlot(INPUT_SLOT + 1), "The unspared cobblestone must have been voided");
        } finally {
            HandlerList.unregisterAll(sparing);
        }
    }

    @Test
    @DisplayName("Sparing every item voids nothing")
    void testSparingEverythingVoidsNothing() {
        BlockMenu menu = placeTrashCan(70, 70);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.DIRT));
        menu.replaceExistingItem(INPUT_SLOT + 1, new ItemStack(Material.COBBLESTONE, 3));

        Listener sparing = new Listener() {
            @EventHandler
            public void onVoid(TrashCanVoidEvent event) {
                for (ItemStack item : event.getItems()) {
                    event.spareItem(item);
                }
            }
        };
        server.getPluginManager().registerEvents(sparing, plugin);

        try {
            tick(menu);

            Assertions.assertNotNull(menu.getItemInSlot(INPUT_SLOT), "A spared item must have been kept");
            Assertions.assertNotNull(menu.getItemInSlot(INPUT_SLOT + 1), "A spared item must have been kept");
        } finally {
            HandlerList.unregisterAll(sparing);
        }
    }

    @Test
    @DisplayName("Sparing an item must not confuse slots whose items differ only in item meta")
    void testSparedItemMatchesByMeta() {
        BlockMenu menu = placeTrashCan(80, 80);
        ItemStack plain = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemStack named = new ItemStack(Material.DIAMOND_PICKAXE);
        named.editMeta(meta -> meta.setDisplayName("Precious"));

        // ItemStack#equals ignores meta, so the plain pickaxe "equals" the named one.
        // The plain one sits in the earlier slot on purpose: sparing must still keep
        // the named pickaxe and void the plain one, not the other way around.
        menu.replaceExistingItem(INPUT_SLOT, plain);
        menu.replaceExistingItem(INPUT_SLOT + 1, named);

        Listener sparing = new Listener() {
            @EventHandler
            public void onVoid(TrashCanVoidEvent event) {
                for (ItemStack item : event.getItems()) {
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        event.spareItem(item);
                    }
                }
            }
        };
        server.getPluginManager().registerEvents(sparing, plugin);

        try {
            tick(menu);

            Assertions.assertNull(menu.getItemInSlot(INPUT_SLOT), "The unspared plain pickaxe must have been voided");
            ItemStack kept = menu.getItemInSlot(INPUT_SLOT + 1);
            Assertions.assertNotNull(kept, "The spared named pickaxe must have been kept");
            Assertions.assertTrue(kept.hasItemMeta(), "The kept pickaxe must be the named one");
            Assertions.assertEquals("Precious", kept.getItemMeta().getDisplayName());
        } finally {
            HandlerList.unregisterAll(sparing);
        }
    }

    @Test
    @DisplayName("A tick with items in the input fires the event and voids them")
    void testTickFiresEventAndVoids() {
        BlockMenu menu = placeTrashCan(10, 10);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onVoid(TrashCanVoidEvent event) {
                seen[0] = true;
                Assertions.assertEquals(trashCan, event.getTrashCan());
                Assertions.assertEquals(menu.getBlock(), event.getBlock());
                Assertions.assertEquals(1, event.getItems().size());
                Assertions.assertEquals(Material.DIRT, event.getItems().get(0).getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(menu);

            Assertions.assertTrue(seen[0], "TrashCanVoidEvent was not fired");
            Assertions.assertNull(menu.getItemInSlot(INPUT_SLOT), "The item must have been voided");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling TrashCanVoidEvent keeps the items in the trash can")
    void testCancelKeepsItems() {
        BlockMenu menu = placeTrashCan(20, 20);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.DIRT));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onVoid(TrashCanVoidEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(menu);

            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "A cancelled void must keep the item");
            Assertions.assertEquals(Material.DIRT, slot.getType());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Voiding without listeners still voids, preserving the old behavior")
    void testVoidWithoutListenersStillVoids() {
        BlockMenu menu = placeTrashCan(30, 30);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.DIRT));

        tick(menu);

        Assertions.assertNull(menu.getItemInSlot(INPUT_SLOT), "The item must have been voided");
    }

    @Test
    @DisplayName("An empty trash can fires no event")
    void testEmptyTrashCanFiresNothing() {
        BlockMenu menu = placeTrashCan(40, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onVoid(TrashCanVoidEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(menu);

            Assertions.assertFalse(seen[0], "No event must be fired for an empty trash can");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Every occupied input slot contributes to the voided items list")
    void testMultipleSlotsAreListed() {
        BlockMenu menu = placeTrashCan(50, 50);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.DIRT));
        menu.replaceExistingItem(INPUT_SLOT + 1, new ItemStack(Material.COBBLESTONE, 3));

        int[] count = { 0 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onVoid(TrashCanVoidEvent event) {
                count[0] = event.getItems().size();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(menu);

            Assertions.assertEquals(2, count[0], "Both occupied input slots must be listed");
            Assertions.assertNull(menu.getItemInSlot(INPUT_SLOT), "The first item must have been voided");
            Assertions.assertNull(menu.getItemInSlot(INPUT_SLOT + 1), "The second item must have been voided");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

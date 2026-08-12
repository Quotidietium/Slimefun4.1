package io.github.thebusybiscuit.slimefun4.implementation.listeners;

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

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.PedestalItemTakeEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the pedestal API expansion: {@link PedestalItemTakeEvent}, exercised
 * through the real {@link AncientAltarListener#onInteract(PlayerRightClickEvent)} pedestal path
 * with a display item placed via the world (mirroring {@link AncientPedestal#placeItem}'s entity
 * without the MockBukkit-unsupported {@code SlimefunUtils.spawnItem} call).
 * <p>
 * The returned item lands in the player's inventory and the display entity is removed, so both
 * are asserted end-to-end; a cancelled event leaves the pedestal untouched.
 *
 * @author Zurker
 */
class TestPedestalItemTakeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AncientPedestal pedestal;
    private static AncientAltarListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "pedestal_take_test");

        SlimefunItemStack altarStack = new SlimefunItemStack("_TEST_ANCIENT_ALTAR", Material.ENCHANTING_TABLE, "&5Test Ancient Altar");
        Slimefun.getItemCfg().setValue("_TEST_ANCIENT_ALTAR.enabled", true);
        AncientAltar altar = new AncientAltar(itemGroup, altarStack, RecipeType.NULL, new ItemStack[9]);
        altar.register(plugin);

        SlimefunItemStack pedestalStack = new SlimefunItemStack("_TEST_ANCIENT_PEDESTAL", Material.DISPENSER, "&dTest Ancient Pedestal");
        Slimefun.getItemCfg().setValue("_TEST_ANCIENT_PEDESTAL.enabled", true);
        pedestal = new AncientPedestal(itemGroup, pedestalStack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.DISPENSER));
        pedestal.register(plugin);

        listener = new AncientAltarListener(plugin, altar, pedestal);
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
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", "_TEST_ANCIENT_PEDESTAL");
        return b;
    }

    /**
     * Places a display item on the pedestal the way {@link AncientPedestal#placeItem} does.
     * <p>
     * Note that MockBukkit's BlockMock shares its internal {@link org.bukkit.Location} instance,
     * so the drop location must be cloned before being shifted - otherwise the pedestal block
     * itself "moves" and the pedestal's entity scan misses the placed item.
     */
    private Item placeDisplayItem(Block b, ItemStack handItem) {
        ItemStack displayItem = CustomItemStack.create(handItem, AncientPedestal.ITEM_PREFIX + System.nanoTime());
        displayItem.setAmount(1);
        return world.dropItem(b.getLocation().clone().add(0.5, 1.2, 0.5), displayItem);
    }

    /**
     * Right-clicks the pedestal via the real listener path.
     */
    private void use(Player player, Block b) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, b, BlockFace.UP);
        listener.onInteract(new PlayerRightClickEvent(interactEvent));
    }

    @Test
    @DisplayName("PedestalItemTakeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(1, 1, 1);
        ItemStack item = new ItemStack(Material.DIAMOND);

        PedestalItemTakeEvent event = new PedestalItemTakeEvent(player, pedestal, b, item);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(pedestal, event.getPedestal());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertFalse(event.isCancelled());

        // The returned item can be changed
        ItemStack other = new ItemStack(Material.EMERALD);
        event.setItem(other);
        Assertions.assertEquals(other, event.getItem());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setItem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PedestalItemTakeEvent(player, null, b, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PedestalItemTakeEvent(player, pedestal, null, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PedestalItemTakeEvent(player, pedestal, b, null));
    }

    @Test
    @DisplayName("Taking an item from a pedestal fires the event and returns the item")
    void testTakeFiresEventAndReturnsItem() {
        Player player = server.addPlayer();
        Block b = placePedestal(10, 10);
        Item entity = placeDisplayItem(b, new ItemStack(Material.DIAMOND));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTake(PedestalItemTakeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pedestal, event.getPedestal());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(Material.DIAMOND, event.getItem().getType());
                Assertions.assertEquals(1, event.getItem().getAmount());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player, b);

            Assertions.assertTrue(seen[0], "PedestalItemTakeEvent was not fired");
            Assertions.assertTrue(player.getInventory().contains(Material.DIAMOND), "The item must have been returned to the player");
            Assertions.assertFalse(entity.isValid(), "The display entity must have been removed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling PedestalItemTakeEvent keeps the item on the pedestal")
    void testCancelKeepsItemOnPedestal() {
        Player player = server.addPlayer();
        Block b = placePedestal(20, 20);
        Item entity = placeDisplayItem(b, new ItemStack(Material.DIAMOND));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onTake(PedestalItemTakeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            use(player, b);

            Assertions.assertFalse(player.getInventory().contains(Material.DIAMOND), "A cancelled take must not return the item");
            Assertions.assertTrue(entity.isValid(), "A cancelled take must keep the display entity");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("setItem changes the returned item while the display entity is removed")
    void testSetItemChangesReturnedItem() {
        Player player = server.addPlayer();
        Block b = placePedestal(60, 60);
        Item entity = placeDisplayItem(b, new ItemStack(Material.DIAMOND));

        boolean[] seen = { false };
        Listener changing = new Listener() {
            @EventHandler
            public void onTake(PedestalItemTakeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(Material.DIAMOND, event.getItem().getType(), "The returned item must default to the placed item");
                event.setItem(new ItemStack(Material.EMERALD, 2));
            }
        };
        server.getPluginManager().registerEvents(changing, plugin);

        try {
            use(player, b);

            Assertions.assertTrue(seen[0], "PedestalItemTakeEvent was not fired");
            Assertions.assertTrue(player.getInventory().contains(Material.EMERALD, 2), "The changed item must have been returned to the player");
            Assertions.assertFalse(player.getInventory().contains(Material.DIAMOND), "The original item must not have been returned");
            Assertions.assertFalse(entity.isValid(), "The display entity must have been removed");
        } finally {
            HandlerList.unregisterAll(changing);
        }
    }

    @Test
    @DisplayName("Taking without listeners still returns the item, preserving the old behavior")
    void testTakeWithoutListenersStillReturnsItem() {
        Player player = server.addPlayer();
        Block b = placePedestal(30, 30);
        Item entity = placeDisplayItem(b, new ItemStack(Material.DIAMOND));

        use(player, b);

        Assertions.assertTrue(player.getInventory().contains(Material.DIAMOND), "The item must have been returned to the player");
        Assertions.assertFalse(entity.isValid(), "The display entity must have been removed");
    }

    @Test
    @DisplayName("Placing onto an empty pedestal fires no take event")
    void testPlaceBranchFiresNoTakeEvent() {
        Player player = server.addPlayer();
        Block b = placePedestal(40, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTake(PedestalItemTakeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND));

        try {
            try {
                PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), b, BlockFace.UP);
                listener.onInteract(new PlayerRightClickEvent(interactEvent));
            } catch (RuntimeException ignored) {
                // SlimefunUtils.spawnItem at the end of placeItem is not fully supported by MockBukkit
            }

            Assertions.assertFalse(seen[0], "Placing an item must not fire the take event");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Right-clicking an unrelated block fires no event")
    void testUnrelatedBlockFiresNothing() {
        Player player = server.addPlayer();
        Block stone = world.getBlockAt(50, 1, 50);
        stone.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTake(PedestalItemTakeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player, stone);

            Assertions.assertFalse(seen[0], "No event must be fired for an unrelated block");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

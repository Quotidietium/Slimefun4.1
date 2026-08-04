package io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.inventory.InventoryMock;
import be.seeseemelk.mockbukkit.inventory.SimpleInventoryViewMock;
import be.seeseemelk.mockbukkit.inventory.WorkbenchInventoryMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemWorkstationEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemWorkstationEvent.Workstation;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the vanilla workstation API expansion:
 * {@link SlimefunItemWorkstationEvent}, exercised through the real crafting listeners
 * (anvil, cauldron, crafting table, brewing stand).
 *
 * @author Zurker
 */
class TestSlimefunItemWorkstationEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SlimefunItem tradeItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register them manually
        new AnvilListener(plugin);
        new GrindstoneListener(plugin);
        new CartographyTableListener(plugin);
        new CauldronListener(plugin);
        new CraftingTableListener(plugin);
        new SmithingTableListener(plugin);
        new BrewingStandListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first.
        // A leather helmet doubles as the cauldron test item (leather armor tag).
        Slimefun.getItemCfg().setValue("TEST_WORKSTATION_ITEM.enabled", true);
        tradeItem = TestUtilities.mockSlimefunItem(plugin, "TEST_WORKSTATION_ITEM", new ItemStack(Material.LEATHER_HELMET));
        tradeItem.register(plugin);
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
     * Builds a view over the given top inventory. MockBukkit's SimpleInventoryViewMock
     * leaves convertSlot/getInventory/getItem unimplemented, so an identity-mapped
     * override is provided (the events call them during construction and dispatch).
     */
    private SimpleInventoryViewMock viewOf(Player player, Inventory top, InventoryType type) {
        return new SimpleInventoryViewMock(player, top, player.getInventory(), type) {
            @Override
            public int convertSlot(int rawSlot) {
                return rawSlot;
            }

            @Override
            public Inventory getInventory(int rawSlot) {
                return rawSlot < getTopInventory().getSize() ? getTopInventory() : getBottomInventory();
            }

            @Override
            public ItemStack getItem(int rawSlot) {
                if (rawSlot < getTopInventory().getSize()) {
                    return getTopInventory().getItem(rawSlot);
                }
                return getBottomInventory().getItem(rawSlot - getTopInventory().getSize());
            }
        };
    }

    private InventoryClickEvent clickAnvilResult(Player player, ItemStack input) {
        InventoryMock anvil = new InventoryMock(null, 3, InventoryType.ANVIL);
        anvil.setItem(0, input);
        SimpleInventoryViewMock view = viewOf(player, anvil, InventoryType.ANVIL);

        InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.RESULT, 2, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("SlimefunItemWorkstationEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        SlimefunItemWorkstationEvent event = new SlimefunItemWorkstationEvent(player, tradeItem, tradeItem.getItem(), Workstation.ANVIL);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(tradeItem, event.getSlimefunItem());
        Assertions.assertEquals(tradeItem.getItem(), event.getItemStack());
        Assertions.assertEquals(Workstation.ANVIL, event.getWorkstation());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemWorkstationEvent(player, null, tradeItem.getItem(), Workstation.ANVIL));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemWorkstationEvent(player, tradeItem, null, Workstation.ANVIL));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemWorkstationEvent(player, tradeItem, tradeItem.getItem(), null));
    }

    @Test
    @DisplayName("The anvil fires the event and denies a SlimefunItem result")
    void testAnvilFiresAndDenies() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(SlimefunItemWorkstationEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(tradeItem, event.getSlimefunItem());
                Assertions.assertEquals(Workstation.ANVIL, event.getWorkstation());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryClickEvent clickEvent = clickAnvilResult(player, tradeItem.getItem().clone());

            Assertions.assertTrue(seen[0], "SlimefunItemWorkstationEvent was not fired");
            Assertions.assertEquals(Event.Result.DENY, clickEvent.getResult(), "The anvil result must have been denied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunItemWorkstationEvent allows the anvil result")
    void testAnvilCancellationAllows() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onUse(SlimefunItemWorkstationEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            InventoryClickEvent clickEvent = clickAnvilResult(player, tradeItem.getItem().clone());

            Assertions.assertNotEquals(Event.Result.DENY, clickEvent.getResult(), "A cancelled event must allow the anvil result");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("The cauldron fires the event and blocks discoloring a Slimefun leather armor")
    void testCauldronFiresAndBlocks() {
        Player player = server.addPlayer();
        Block cauldron = world.getBlockAt(new Location(world, 10, 1, 10));
        cauldron.setType(Material.CAULDRON);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(SlimefunItemWorkstationEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(tradeItem, event.getSlimefunItem());
                Assertions.assertEquals(Workstation.CAULDRON, event.getWorkstation());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, tradeItem.getItem().clone(), cauldron, org.bukkit.block.BlockFace.UP);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertTrue(seen[0], "SlimefunItemWorkstationEvent was not fired");
            Assertions.assertTrue(interactEvent.isCancelled(), "The cauldron use must have been blocked");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("The crafting table fires the event and denies the result")
    void testCraftingTableFiresAndDenies() {
        Player player = server.addPlayer();
        WorkbenchInventoryMock workbench = new WorkbenchInventoryMock(null);
        workbench.setItem(0, tradeItem.getItem().clone());
        SimpleInventoryViewMock view = viewOf(player, workbench, InventoryType.WORKBENCH);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(SlimefunItemWorkstationEvent event) {
                seen[0] = true;
                Assertions.assertEquals(tradeItem, event.getSlimefunItem());
                Assertions.assertEquals(Workstation.CRAFTING_TABLE, event.getWorkstation());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            org.bukkit.event.inventory.CraftItemEvent craftEvent = new org.bukkit.event.inventory.CraftItemEvent(Mockito.mock(Recipe.class), view, InventoryType.SlotType.RESULT, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
            server.getPluginManager().callEvent(craftEvent);

            Assertions.assertTrue(seen[0], "SlimefunItemWorkstationEvent was not fired");
            Assertions.assertEquals(Event.Result.DENY, craftEvent.getResult(), "The crafting result must have been denied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("The crafting table preview is hidden unless the event is cancelled")
    void testCraftingTablePreviewHiddenAndRestored() {
        Player player = server.addPlayer();
        WorkbenchInventoryMock workbench = new WorkbenchInventoryMock(null);
        workbench.setItem(0, tradeItem.getItem().clone());
        workbench.setResult(new ItemStack(Material.STONE));
        SimpleInventoryViewMock view = viewOf(player, workbench, InventoryType.WORKBENCH);

        // No listener: the preview is hidden, preserving the old behavior
        PrepareItemCraftEvent prepareEvent = new PrepareItemCraftEvent(workbench, view, false);
        server.getPluginManager().callEvent(prepareEvent);
        Assertions.assertNull(workbench.getResult(), "The preview must have been hidden");

        // With a cancelling listener: the preview stays visible
        workbench.setResult(new ItemStack(Material.STONE));
        Listener cancelling = new Listener() {
            @EventHandler
            public void onUse(SlimefunItemWorkstationEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PrepareItemCraftEvent prepareEvent2 = new PrepareItemCraftEvent(workbench, view, false);
            server.getPluginManager().callEvent(prepareEvent2);
            Assertions.assertNotNull(workbench.getResult(), "A cancelled event must keep the preview visible");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("The brewing stand fires the event and blocks the ingredient")
    void testBrewingStandFiresAndBlocks() {
        Player player = server.addPlayer();

        // The listener insists on a BrewingStand holder, MockBukkit has none: stub one
        org.bukkit.block.BrewingStand brewingStandState = Mockito.mock(org.bukkit.block.BrewingStand.class);
        Inventory brewingTop = Mockito.mock(Inventory.class);
        Mockito.when(brewingTop.getType()).thenReturn(InventoryType.BREWING);
        Mockito.when(brewingTop.getHolder()).thenReturn(brewingStandState);
        Mockito.when(brewingTop.getSize()).thenReturn(5);

        SimpleInventoryViewMock view = viewOf(player, brewingTop, InventoryType.BREWING);
        view.setCursor(tradeItem.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(SlimefunItemWorkstationEvent event) {
                seen[0] = true;
                Assertions.assertEquals(tradeItem, event.getSlimefunItem());
                Assertions.assertEquals(Workstation.BREWING_STAND, event.getWorkstation());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryClickEvent clickEvent = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
            server.getPluginManager().callEvent(clickEvent);

            Assertions.assertTrue(seen[0], "SlimefunItemWorkstationEvent was not fired");
            Assertions.assertTrue(clickEvent.isCancelled(), "The brewing stand must have blocked the ingredient");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An anvil use without listeners is still denied, preserving the old behavior")
    void testAnvilWithoutListenersStillDenied() {
        Player player = server.addPlayer();

        InventoryClickEvent clickEvent = clickAnvilResult(player, tradeItem.getItem().clone());

        Assertions.assertEquals(Event.Result.DENY, clickEvent.getResult(), "The anvil result must have been denied");
    }

    @Test
    @DisplayName("A vanilla item fires no event and stays allowed")
    void testVanillaItemStaysAllowed() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(SlimefunItemWorkstationEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            InventoryClickEvent clickEvent = clickAnvilResult(player, new ItemStack(Material.IRON_INGOT));

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla item");
            Assertions.assertNotEquals(Event.Result.DENY, clickEvent.getResult(), "A vanilla item must stay usable");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

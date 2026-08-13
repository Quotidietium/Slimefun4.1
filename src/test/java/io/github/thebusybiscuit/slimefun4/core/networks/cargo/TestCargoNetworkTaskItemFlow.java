package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;

import io.github.thebusybiscuit.slimefun4.api.events.CargoItemInsertEvent;
import io.github.thebusybiscuit.slimefun4.api.events.CargoItemWithdrawEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * End-to-end regression coverage for the item-flow wiring of {@link CargoNetworkTask}:
 * a withdrawn item must always end up in exactly one place - an output container,
 * back in its source container, or (only when an addon explicitly replaces it with
 * air) nowhere. This guards the cargo API expansion against item duplication and
 * item voiding.
 * <p>
 * The {@link CargoNet} is mocked (attached blocks, item filter, regulator) so the
 * task runs against plain vanilla chests without node menus; the block/item flow
 * itself uses the real {@link CargoNetworkTask}, {@link CargoUtils} and chest
 * inventories.
 *
 * @author Zurker
 */
class TestCargoNetworkTaskItemFlow {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = server.addSimpleWorld("cargo-flow");
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
     * Everything needed to drive one cargo route: the task, the input node location,
     * its attached chest and the output map (channel 0).
     */
    private record Route(CargoNetworkTask task, Location inputNode, Block inputChest, Map<Integer, List<Location>> outputs) {
    }

    /**
     * Builds a one-input one-output cargo route on unique coordinates: a plain node
     * block with a chest beside it on each end. The input chest is stocked with the
     * given item.
     */
    private Route createRoute(int xi, int zi, int xo, int zo, ItemStack sourceItem) {
        Location inputNode = new Location(world, xi, 60, zi);
        world.getBlockAt(inputNode).setType(Material.STONE);
        Block inputChest = world.getBlockAt(xi + 1, 60, zi);
        inputChest.setType(Material.CHEST);

        if (sourceItem != null) {
            getInventory(xi + 1, zi).setItem(0, sourceItem);
        }

        Location outputNode = new Location(world, xo, 60, zo);
        world.getBlockAt(outputNode).setType(Material.STONE);
        Block outputChest = world.getBlockAt(xo + 1, 60, zo);
        outputChest.setType(Material.CHEST);

        ItemFilter allowAll = Mockito.mock(ItemFilter.class);
        Mockito.when(allowAll.test(Mockito.any())).thenReturn(true);
        Mockito.when(allowAll.isDirty()).thenReturn(false);

        CargoNet network = Mockito.mock(CargoNet.class);
        Mockito.when(network.getAttachedBlock(inputNode)).thenReturn(Optional.of(inputChest));
        Mockito.when(network.getAttachedBlock(outputNode)).thenReturn(Optional.of(outputChest));
        Mockito.when(network.getItemFilter(Mockito.any())).thenReturn(allowAll);
        Mockito.when(network.getRegulator()).thenReturn(new Location(world, 0, 60, 0));

        Map<Location, Integer> inputs = new HashMap<>();
        inputs.put(inputNode, 0);
        Map<Integer, List<Location>> outputs = new HashMap<>();
        outputs.put(0, new ArrayList<>(List.of(outputNode)));

        return new Route(new CargoNetworkTask(network, inputs, outputs), inputNode, inputChest, outputs);
    }

    /**
     * Drives {@code routeItems} directly via reflection. {@link CargoNetworkTask#run()}
     * also feeds the profiler with the (unregistered in unit tests) cargo node item,
     * so tests go one level deeper into the actual item-flow logic.
     */
    private void route(Route route) {
        try {
            Method routeItems = CargoNetworkTask.class.getDeclaredMethod("routeItems", Location.class, Block.class, int.class, Map.class);
            routeItems.setAccessible(true);
            routeItems.invoke(route.task(), route.inputNode(), route.inputChest(), 0, route.outputs());
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            throw new IllegalStateException("routeItems failed", x.getCause());
        } catch (ReflectiveOperationException x) {
            throw new IllegalStateException("routeItems is not accessible", x);
        }
    }

    private Inventory getInventory(int x, int z) {
        return ((InventoryHolder) world.getBlockAt(x, 60, z).getState()).getInventory();
    }

    /**
     * Counts the total amount of the given material in the inventory.
     */
    private int countMaterial(Inventory inv, Material material) {
        int total = 0;

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }

        return total;
    }

    @Test
    @DisplayName("Without listeners the withdrawn item is transferred to the output chest")
    void testBaselineTransfer() {
        Route route = createRoute(10, 10, 20, 20, new ItemStack(Material.DIAMOND, 5));

        route(route);

        Assertions.assertEquals(0, countMaterial(getInventory(11, 10), Material.DIAMOND), "The source chest must have been emptied");
        Assertions.assertEquals(5, countMaterial(getInventory(21, 20), Material.DIAMOND), "The output chest must have received the item");
    }

    @Test
    @DisplayName("Cancelling CargoItemWithdrawEvent returns the item to its source slot")
    void testWithdrawCancelReturnsItem() {
        Route route = createRoute(30, 30, 40, 40, new ItemStack(Material.DIAMOND, 5));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onWithdraw(CargoItemWithdrawEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            route(route);

            Assertions.assertEquals(5, countMaterial(getInventory(31, 30), Material.DIAMOND), "A vetoed withdrawal must return the item to the source chest");
            Assertions.assertEquals(0, countMaterial(getInventory(41, 40), Material.DIAMOND), "The output chest must stay empty");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("CargoItemWithdrawEvent#setItem replaces the distributed item")
    void testWithdrawSetItemReplacement() {
        Route route = createRoute(50, 50, 60, 60, new ItemStack(Material.DIAMOND, 5));

        Listener replacing = new Listener() {
            @EventHandler
            public void onWithdraw(CargoItemWithdrawEvent event) {
                event.setItem(new ItemStack(Material.EMERALD, 3));
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            route(route);

            Assertions.assertEquals(0, countMaterial(getInventory(51, 50), Material.DIAMOND), "The original item was withdrawn from the source");
            Assertions.assertEquals(0, countMaterial(getInventory(61, 60), Material.DIAMOND), "The original item must not reach the output");
            Assertions.assertEquals(3, countMaterial(getInventory(61, 60), Material.EMERALD), "The replacement must have been delivered");
        } finally {
            HandlerList.unregisterAll(replacing);
        }
    }

    @Test
    @DisplayName("CargoItemWithdrawEvent#setItem with air destroys the withdrawn item")
    void testWithdrawSetItemAirVoidsItem() {
        Route route = createRoute(70, 70, 80, 80, new ItemStack(Material.DIAMOND, 5));

        Listener voiding = new Listener() {
            @EventHandler
            public void onWithdraw(CargoItemWithdrawEvent event) {
                event.setItem(new ItemStack(Material.AIR));
            }
        };
        server.getPluginManager().registerEvents(voiding, plugin);

        try {
            route(route);

            Assertions.assertEquals(0, countMaterial(getInventory(71, 70), Material.DIAMOND), "The item must not return to the source chest");
            Assertions.assertEquals(0, countMaterial(getInventory(81, 80), Material.DIAMOND), "The item must not reach the output chest");
        } finally {
            HandlerList.unregisterAll(voiding);
        }
    }

    @Test
    @DisplayName("Cancelling CargoItemInsertEvent keeps the item in transit and returns it to the source")
    void testInsertCancelReturnsItemToSource() {
        Route route = createRoute(90, 90, 100, 100, new ItemStack(Material.DIAMOND, 5));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onInsert(CargoItemInsertEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            route(route);

            Assertions.assertEquals(5, countMaterial(getInventory(91, 90), Material.DIAMOND), "An undeliverable item must return to the source chest");
            Assertions.assertEquals(0, countMaterial(getInventory(101, 100), Material.DIAMOND), "The output chest must stay empty");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("CargoItemInsertEvent#setItem replaces the inserted item")
    void testInsertSetItemReplacement() {
        Route route = createRoute(110, 110, 120, 120, new ItemStack(Material.DIAMOND, 5));

        Listener replacing = new Listener() {
            @EventHandler
            public void onInsert(CargoItemInsertEvent event) {
                event.setItem(new ItemStack(Material.GOLD_INGOT, 2));
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            route(route);

            Assertions.assertEquals(0, countMaterial(getInventory(111, 110), Material.DIAMOND), "The original item must not return to the source");
            Assertions.assertEquals(0, countMaterial(getInventory(121, 120), Material.DIAMOND), "The original item must not reach the output");
            Assertions.assertEquals(2, countMaterial(getInventory(121, 120), Material.GOLD_INGOT), "The replacement must have been inserted");
        } finally {
            HandlerList.unregisterAll(replacing);
        }
    }

    @Test
    @DisplayName("CargoItemInsertEvent#setItem with air destroys the item in transit")
    void testInsertSetItemAirDestroysItem() {
        Route route = createRoute(130, 130, 140, 140, new ItemStack(Material.DIAMOND, 5));

        Listener voiding = new Listener() {
            @EventHandler
            public void onInsert(CargoItemInsertEvent event) {
                event.setItem(new ItemStack(Material.AIR));
            }
        };
        server.getPluginManager().registerEvents(voiding, plugin);

        try {
            route(route);

            Assertions.assertEquals(0, countMaterial(getInventory(131, 130), Material.DIAMOND), "The item must not return to the source chest");
            Assertions.assertEquals(0, countMaterial(getInventory(141, 140), Material.DIAMOND), "The item must not reach the output chest");
        } finally {
            HandlerList.unregisterAll(voiding);
        }
    }

    @Test
    @DisplayName("A listener breaking the source container mid-event does not void the returned item")
    void testBrokenSourceContainerDropsItem() {
        Route route = createRoute(150, 150, 160, 160, new ItemStack(Material.DIAMOND, 5));

        /*
         * Break the source chest while the withdraw event is being handled, then break
         * the output chest too: the withdrawn item has nowhere to return to. The stale
         * cached Inventory of the broken chest must not silently swallow it.
         */
        Listener breaking = new Listener() {
            @EventHandler
            public void onWithdraw(CargoItemWithdrawEvent event) {
                world.getBlockAt(151, 60, 150).setType(Material.AIR);
                world.getBlockAt(161, 60, 160).setType(Material.AIR);
            }
        };
        server.getPluginManager().registerEvents(breaking, plugin);

        try {
            route(route);

            long dropped = world.getEntities().stream()
                .filter(e -> e instanceof org.bukkit.entity.Item item && item.getItemStack().getType() == Material.DIAMOND)
                .mapToInt(e -> ((org.bukkit.entity.Item) e).getItemStack().getAmount())
                .sum();

            Assertions.assertEquals(5, dropped, "The withdrawn item must have been dropped on the ground instead of voided");
        } finally {
            HandlerList.unregisterAll(breaking);
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

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

import io.github.thebusybiscuit.slimefun4.api.events.DustWashProcessEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the dust-washer API expansion: {@link DustWashProcessEvent},
 * exercised by driving the real {@link ElectricDustWasher} {@link BlockTicker} against
 * a {@link BlockStorage}-backed washer whose input slot holds sand.
 * <p>
 * The sand-to-salt path is deterministic and does not touch the {@code OreWasher}
 * reference (which is absent under the unit-test startup), so it is fully observable:
 * the input is consumed and a {@code CraftingOperation} carrying the salt appears on
 * the processor. A vetoed wash keeps the input and starts no operation.
 *
 * @author Zurker
 */
class TestDustWashProcessEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ElectricDustWasher washer;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "dust_washer_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_DUST_WASHER", Material.DISPENSER, "&fTest Dust Washer");
        Slimefun.getItemCfg().setValue("_TEST_DUST_WASHER.enabled", true);
        washer = new ElectricDustWasher(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        washer.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        washer.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private BlockMenu placeWasher(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", washer.getId(), true);
        return BlockStorage.getInventory(b);
    }

    private void tick(Block b) {
        washer.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, washer, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return washer.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    private void assertInputsConsumed(BlockMenu menu) {
        for (int slot : washer.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertTrue(item == null || item.getAmount() == 0, "The input must have been consumed, slot " + slot + " held: " + item);
        }
    }

    private void assertInputKept(BlockMenu menu, Material type) {
        ItemStack item = menu.getItemInSlot(washer.getInputSlots()[0]);
        Assertions.assertNotNull(item, "The input must stay in its slot");
        Assertions.assertEquals(type, item.getType(), "The input must stay untouched");
        Assertions.assertTrue(item.getAmount() > 0, "The input amount must be unchanged");
    }

    @Test
    @DisplayName("DustWashProcessEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);
        ItemStack input = new ItemStack(Material.SAND);
        ItemStack output = SlimefunItems.SALT.item();

        DustWashProcessEvent event = new DustWashProcessEvent(washer, b.getLocation(), input, output);

        Assertions.assertEquals(washer, event.getMachine());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertEquals(input, event.getInput());
        Assertions.assertEquals(output, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.IRON_INGOT);
        event.setResult(replacement);
        Assertions.assertEquals(replacement, event.getResult());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new DustWashProcessEvent(null, b.getLocation(), input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DustWashProcessEvent(washer, null, input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DustWashProcessEvent(washer, b.getLocation(), null, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DustWashProcessEvent(washer, b.getLocation(), input, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("Washing sand fires the event, consumes the input and starts the operation")
    void testWashFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeWasher(10, 10);
        menu.replaceExistingItem(washer.getInputSlots()[0], new ItemStack(Material.SAND));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWash(DustWashProcessEvent event) {
                seen[0] = true;
                Assertions.assertEquals(washer, event.getMachine());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertEquals(Material.SAND, event.getInput().getType());
                Assertions.assertEquals(SlimefunItems.SALT.item(), event.getResult());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "DustWashProcessEvent was not fired");
            assertInputsConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The washing operation must have been started");

            ItemStack produced = washer.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertEquals(SlimefunItems.SALT.item(), produced, "The operation must produce salt");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling DustWashProcessEvent keeps the input and starts no operation")
    void testCancelKeepsInputAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeWasher(20, 20);
        menu.replaceExistingItem(washer.getInputSlots()[0], new ItemStack(Material.SAND));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onWash(DustWashProcessEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            assertInputKept(menu, Material.SAND);
            Assertions.assertFalse(hasOperation(b), "A vetoed wash must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Washing without listeners still consumes the input, preserving the old behavior")
    void testWashWithoutListenersProcesses() {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeWasher(30, 30);
        menu.replaceExistingItem(washer.getInputSlots()[0], new ItemStack(Material.SAND));

        tick(b);

        assertInputsConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The washing operation must have been started");
    }

    @Test
    @DisplayName("Replacing the output via setResult bakes the replacement into the operation")
    void testSetResultRedirect() {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeWasher(40, 40);
        menu.replaceExistingItem(washer.getInputSlots()[0], new ItemStack(Material.SAND));

        ItemStack replacement = new ItemStack(Material.IRON_INGOT);
        Listener redirecting = new Listener() {
            @EventHandler
            public void onWash(DustWashProcessEvent event) {
                event.setResult(replacement);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            tick(b);

            Assertions.assertTrue(hasOperation(b), "The washing operation must have been started");
            ItemStack produced = washer.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertEquals(replacement, produced, "The operation must produce the replacement output");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("An invalid input fires no event and keeps the input")
    void testInvalidInputFiresNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeWasher(50, 50);
        menu.replaceExistingItem(washer.getInputSlots()[0], new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWash(DustWashProcessEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an invalid input");
            assertInputKept(menu, Material.DIRT);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A jammed output fires no event and keeps the input")
    void testJammedOutputFiresNothing() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeWasher(60, 60);
        // Fill every output slot so the salt cannot fit
        for (int slot : washer.getOutputSlots()) {
            menu.replaceExistingItem(slot, new ItemStack(Material.STONE));
        }
        menu.replaceExistingItem(washer.getInputSlots()[0], new ItemStack(Material.SAND));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWash(DustWashProcessEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired when the output is jammed");
            Assertions.assertEquals(Material.SAND, menu.getItemInSlot(washer.getInputSlots()[0]).getType(), "The sand must stay in its slot");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

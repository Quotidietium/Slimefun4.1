package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import java.util.EnumSet;
import java.util.Set;

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

import io.github.thebusybiscuit.slimefun4.api.events.ElectricGoldPanProcessEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GoldPan;
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.NetherGoldPan;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the electric gold pan API expansion:
 * {@link ElectricGoldPanProcessEvent}, exercised by driving the real
 * {@link ElectricGoldPan} {@link BlockTicker} against a {@link BlockStorage}-backed
 * machine whose input slot holds gravel.
 * <p>
 * The rolled output is randomized, so the event is the only fixed observation point:
 * it fires before the input is consumed, can be cancelled to keep the gravel, and can
 * replace the rolled output. The gold-pan and nether-gold-pan definitions are
 * registered under their real ids first, so the {@link ElectricGoldPan} can resolve
 * them at construction time.
 *
 * @author Zurker
 */
class TestElectricGoldPanProcessEvent {

    /**
     * The materials a gravel pan can roll. SIFTED_ORE is a Slimefun item built on
     * GUNPOWDER, so that is the material the roll produces for it.
     */
    private static final Set<Material> GOLD_PAN_DROPS = EnumSet.of(Material.FLINT, Material.CLAY_BALL, Material.IRON_NUGGET, Material.GUNPOWDER);

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ElectricGoldPan machine;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "electric_gold_pan_test");

        // Register the gold-pan variants under their real ids so the ElectricGoldPan can resolve them
        Slimefun.getItemCfg().setValue("GOLD_PAN.enabled", true);
        new GoldPan(itemGroup, SlimefunItems.GOLD_PAN, RecipeType.NULL, new ItemStack[9]).register(plugin);

        Slimefun.getItemCfg().setValue("NETHER_GOLD_PAN.enabled", true);
        new NetherGoldPan(itemGroup, SlimefunItems.NETHER_GOLD_PAN, RecipeType.NULL, new ItemStack[9]).register(plugin);

        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ELECTRIC_GOLD_PAN", Material.BROWN_TERRACOTTA, "&fTest Electric Gold Pan");
        Slimefun.getItemCfg().setValue("_TEST_ELECTRIC_GOLD_PAN.enabled", true);
        machine = new ElectricGoldPan(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        machine.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        machine.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private BlockMenu placeMachine(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.BROWN_TERRACOTTA);
        BlockStorage.addBlockInfo(b, "id", machine.getId(), true);
        return BlockStorage.getInventory(b);
    }

    private void tick(Block b) {
        machine.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, machine, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return machine.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    private void assertInputConsumed(BlockMenu menu) {
        ItemStack item = menu.getItemInSlot(machine.getInputSlots()[0]);
        Assertions.assertTrue(item == null || item.getAmount() == 0, "The gravel must have been consumed");
    }

    private void assertInputKept(BlockMenu menu) {
        ItemStack item = menu.getItemInSlot(machine.getInputSlots()[0]);
        Assertions.assertNotNull(item, "The gravel must stay in its slot");
        Assertions.assertEquals(Material.GRAVEL, item.getType(), "The gravel must stay untouched");
        Assertions.assertTrue(item.getAmount() > 0, "The gravel amount must be unchanged");
    }

    @Test
    @DisplayName("ElectricGoldPanProcessEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);
        ItemStack input = new ItemStack(Material.GRAVEL);
        ItemStack output = new ItemStack(Material.FLINT);

        ElectricGoldPanProcessEvent event = new ElectricGoldPanProcessEvent(machine, b.getLocation(), input, output);

        Assertions.assertEquals(machine, event.getMachine());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertEquals(input, event.getInput());
        Assertions.assertEquals(output, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.DIAMOND);
        event.setResult(replacement);
        Assertions.assertEquals(replacement, event.getResult());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElectricGoldPanProcessEvent(null, b.getLocation(), input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElectricGoldPanProcessEvent(machine, null, input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElectricGoldPanProcessEvent(machine, b.getLocation(), null, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElectricGoldPanProcessEvent(machine, b.getLocation(), input, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("Processing gravel fires the event, consumes the input and starts the operation")
    void testProcessFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeMachine(10, 10);
        menu.replaceExistingItem(machine.getInputSlots()[0], new ItemStack(Material.GRAVEL));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(ElectricGoldPanProcessEvent event) {
                seen[0] = true;
                Assertions.assertEquals(machine, event.getMachine());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertEquals(Material.GRAVEL, event.getInput().getType());
                Assertions.assertTrue(GOLD_PAN_DROPS.contains(event.getResult().getType()), "The rolled output must be a valid gold-pan drop, got: " + event.getResult().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "ElectricGoldPanProcessEvent was not fired");
            assertInputConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The panning operation must have been started");

            ItemStack produced = machine.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertTrue(GOLD_PAN_DROPS.contains(produced.getType()), "The operation must produce a gold-pan drop, got: " + produced.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ElectricGoldPanProcessEvent keeps the gravel and starts no operation")
    void testCancelKeepsInputAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeMachine(20, 20);
        menu.replaceExistingItem(machine.getInputSlots()[0], new ItemStack(Material.GRAVEL));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onProcess(ElectricGoldPanProcessEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            assertInputKept(menu);
            Assertions.assertFalse(hasOperation(b), "A vetoed pan must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Processing without listeners still consumes the gravel, preserving the old behavior")
    void testProcessWithoutListenersProcesses() {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeMachine(30, 30);
        menu.replaceExistingItem(machine.getInputSlots()[0], new ItemStack(Material.GRAVEL));

        tick(b);

        assertInputConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The panning operation must have been started");
    }

    @Test
    @DisplayName("Replacing the rolled output via setResult bakes the replacement into the operation")
    void testSetResultRedirect() {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeMachine(40, 40);
        menu.replaceExistingItem(machine.getInputSlots()[0], new ItemStack(Material.GRAVEL));

        ItemStack replacement = new ItemStack(Material.DIAMOND);
        Listener redirecting = new Listener() {
            @EventHandler
            public void onProcess(ElectricGoldPanProcessEvent event) {
                event.setResult(replacement);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            tick(b);

            Assertions.assertTrue(hasOperation(b), "The panning operation must have been started");
            ItemStack produced = machine.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertEquals(replacement, produced, "The operation must produce the replacement output");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("An invalid input fires no event and keeps the input")
    void testInvalidInputFiresNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeMachine(50, 50);
        menu.replaceExistingItem(machine.getInputSlots()[0], new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(ElectricGoldPanProcessEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an invalid input");
            ItemStack item = menu.getItemInSlot(machine.getInputSlots()[0]);
            Assertions.assertEquals(Material.DIRT, item.getType(), "The dirt must stay in its slot");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A jammed output fires no event and keeps the gravel")
    void testJammedOutputFiresNothing() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeMachine(60, 60);
        // Fill every output slot so the rolled drop cannot fit
        for (int slot : machine.getOutputSlots()) {
            menu.replaceExistingItem(slot, new ItemStack(Material.STONE));
        }
        menu.replaceExistingItem(machine.getInputSlots()[0], new ItemStack(Material.GRAVEL));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(ElectricGoldPanProcessEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired when the output is jammed");
            assertInputKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

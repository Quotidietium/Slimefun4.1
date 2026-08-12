package me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems;

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

import io.github.thebusybiscuit.slimefun4.api.events.MachineRecipeStartEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the machine recipe API expansion:
 * {@link MachineRecipeStartEvent}, exercised by driving the real {@link AContainer}
 * {@link BlockTicker} against a {@link BlockStorage}-backed machine whose input slot
 * matches its single registered recipe.
 * <p>
 * A matching tick consumes the inputs and starts a {@code CraftingOperation}, so tests
 * assert the outcome end-to-end: a vetoed recipe leaves the inputs untouched and the
 * machine idle. The event must not fire when the output slots are jammed, because no
 * inputs would be consumed in that case.
 *
 * @author Zurker
 */
class TestMachineRecipeStartEvent {

    private static final int INPUT_SLOT = 19;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AContainer machine;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "recipe_start_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_RECIPE_MACHINE", Material.DISPENSER, "&fTest Recipe Machine");
        Slimefun.getItemCfg().setValue("TEST_RECIPE_MACHINE.enabled", true);
        machine = new AContainer(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ItemStack getProgressBar() {
                return new ItemStack(Material.FLINT_AND_STEEL);
            }

            @Override
            public String getMachineIdentifier() {
                return "TEST_RECIPE_MACHINE";
            }
        };
        machine.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        machine.register(plugin);
        machine.registerRecipe(4, new ItemStack(Material.COBBLESTONE), new ItemStack(Material.STONE));
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
     * Places the machine as a real block backed by {@link BlockStorage} and returns its menu.
     */
    private BlockMenu placeMachine(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", machine.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the machine's real {@link BlockTicker}.
     */
    private void tick(Block b) {
        machine.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, machine, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return machine.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    /**
     * {@link BlockMenu#consumeItem(int, int)} leaves a zero-amount stack behind instead of
     * clearing the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertInputConsumed(BlockMenu menu) {
        ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
        Assertions.assertTrue(slot == null || slot.getAmount() == 0, "The input must have been consumed, got: " + slot);
    }

    @Test
    @DisplayName("MachineRecipeStartEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);
        MachineRecipe recipe = machine.getMachineRecipes().get(0);

        MachineRecipeStartEvent event = new MachineRecipeStartEvent(machine, b.getLocation(), recipe);

        Assertions.assertEquals(machine, event.getMachine());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertEquals(recipe, event.getRecipe());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MachineRecipeStartEvent(null, b.getLocation(), recipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MachineRecipeStartEvent(machine, null, recipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MachineRecipeStartEvent(machine, b.getLocation(), null));
    }

    @Test
    @DisplayName("A matching tick fires the event, consumes the inputs and starts the operation")
    void testRecipeStartFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeMachine(10, 10);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.COBBLESTONE));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRecipeStart(MachineRecipeStartEvent event) {
                seen[0] = true;
                Assertions.assertEquals(machine, event.getMachine());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertSame(machine.getMachineRecipes().get(0), event.getRecipe(), "The event must carry the matched recipe");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "MachineRecipeStartEvent was not fired");
            assertInputConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The crafting operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MachineRecipeStartEvent keeps the inputs and starts no operation")
    void testCancelKeepsInputsAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeMachine(20, 20);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.COBBLESTONE));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRecipeStart(MachineRecipeStartEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "A vetoed recipe must keep the inputs");
            Assertions.assertEquals(1, slot.getAmount(), "A vetoed recipe must keep the inputs untouched");
            Assertions.assertFalse(hasOperation(b), "A vetoed recipe must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A matching tick without listeners still consumes the inputs, preserving the old behavior")
    void testRecipeStartWithoutListenersStarts() {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeMachine(30, 30);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.COBBLESTONE));

        tick(b);

        assertInputConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The crafting operation must have been started");
    }

    @Test
    @DisplayName("The operation duration defaults to the recipe ticks and is modifiable and validated")
    void testTicksDefaultAndValidation() {
        Block b = world.getBlockAt(2, 60, 2);
        MachineRecipe recipe = machine.getMachineRecipes().get(0);

        MachineRecipeStartEvent event = new MachineRecipeStartEvent(machine, b.getLocation(), recipe);

        Assertions.assertEquals(recipe.getTicks(), event.getTicks(), "The duration must default to the recipe's ticks");

        event.setTicks(40);
        Assertions.assertEquals(40, event.getTicks());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTicks(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTicks(-1));
    }

    @Test
    @DisplayName("A modified duration is applied to the started operation without touching the shared recipe")
    void testModifiedTicksAppliedToOperationOnly() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeMachine(60, 60);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.COBBLESTONE));

        MachineRecipe recipe = machine.getMachineRecipes().get(0);
        int originalTicks = recipe.getTicks();

        Listener accelerating = new Listener() {
            @EventHandler
            public void onRecipeStart(MachineRecipeStartEvent event) {
                event.setTicks(3);
            }
        };
        server.getPluginManager().registerEvents(accelerating, plugin);

        try {
            tick(b);

            assertInputConsumed(menu);

            CraftingOperation operation = machine.getMachineProcessor().getOperation(b.getLocation());
            Assertions.assertNotNull(operation, "The crafting operation must have been started");
            Assertions.assertEquals(3, operation.getTotalTicks(), "The operation must run with the modified duration");
            Assertions.assertEquals(originalTicks, recipe.getTicks(), "The shared recipe must keep its original duration");
        } finally {
            HandlerList.unregisterAll(accelerating);
        }
    }

    @Test
    @DisplayName("An unchanged duration starts the operation with the recipe's own ticks")
    void testUnchangedTicksKeepsRecipeDuration() {
        Block b = world.getBlockAt(70, 60, 70);
        BlockMenu menu = placeMachine(70, 70);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.COBBLESTONE));

        MachineRecipe recipe = machine.getMachineRecipes().get(0);

        Listener watcher = new Listener() {
            @EventHandler
            public void onRecipeStart(MachineRecipeStartEvent event) {
                // Only observe, do not touch the duration
                Assertions.assertEquals(recipe.getTicks(), event.getTicks());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            CraftingOperation operation = machine.getMachineProcessor().getOperation(b.getLocation());
            Assertions.assertNotNull(operation, "The crafting operation must have been started");
            Assertions.assertEquals(recipe.getTicks(), operation.getTotalTicks(), "An untouched duration must reproduce the recipe's ticks");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A non-matching input fires no event and starts no operation")
    void testNonMatchingInputFiresNothing() {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeMachine(40, 40);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRecipeStart(MachineRecipeStartEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without a matching recipe");
            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "The foreign item must have stayed put");
            Assertions.assertEquals(Material.DIRT, slot.getType());
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A jammed output fires no event, keeps the inputs and starts no operation")
    void testJammedOutputFiresNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeMachine(50, 50);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.COBBLESTONE));
        menu.replaceExistingItem(24, new ItemStack(Material.STONE, 64));
        menu.replaceExistingItem(25, new ItemStack(Material.STONE, 64));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRecipeStart(MachineRecipeStartEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired when the output cannot hold the results");
            ItemStack slot = menu.getItemInSlot(INPUT_SLOT);
            Assertions.assertNotNull(slot, "A jammed machine must keep the inputs");
            Assertions.assertEquals(1, slot.getAmount(), "A jammed machine must keep the inputs untouched");
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}

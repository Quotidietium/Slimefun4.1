package me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Discriminating coverage for the whole-list Material index in
 * {@link AContainer}'s recipe scan: when no present Material appears in any
 * recipe input (and no recipe bypasses the Material prefilter), the scan may
 * shortcut to "matched nothing" without walking the list.
 *
 * <p>The shortcut must be invisible: it may only fire when the full scan
 * provably ends with matchedNothing, must stay dormant while any
 * prefilter-bypassing recipe exists, and must rebuild when the recipe list
 * grows. These tests pin each property against real
 * {@link BlockStorage}-backed machines.
 *
 * @author Zurker
 */
class TestRecipeScanMaterialIndex {

    private static final int INPUT_SLOT = 19;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AContainer machine;
    private static AContainer emptyInputMachine;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "material_index_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_MATERIAL_INDEX_MACHINE", Material.DISPENSER, "&fTest Material Index Machine");
        Slimefun.getItemCfg().setValue("TEST_MATERIAL_INDEX_MACHINE.enabled", true);
        machine = new AContainer(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ItemStack getProgressBar() {
                return new ItemStack(Material.FLINT_AND_STEEL);
            }

            @Override
            public String getMachineIdentifier() {
                return "TEST_MATERIAL_INDEX_MACHINE";
            }
        };
        machine.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        machine.register(plugin);
        machine.registerRecipe(4, new ItemStack(Material.DIRT), new ItemStack(Material.GOLD_INGOT));
        machine.registerRecipe(4, new ItemStack(Material.SAND), new ItemStack(Material.GLASS));

        /*
         * A machine whose recipe list contains a zero-length input array: such
         * a recipe bypasses the Material prefilter entirely (and matches any
         * input), so the whole-list shortcut must stay dormant for it.
         */
        SlimefunItemStack emptyStack = new SlimefunItemStack("TEST_EMPTY_INPUT_MACHINE", Material.DISPENSER, "&fTest Empty Input Machine");
        Slimefun.getItemCfg().setValue("TEST_EMPTY_INPUT_MACHINE.enabled", true);
        emptyInputMachine = new AContainer(itemGroup, emptyStack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ItemStack getProgressBar() {
                return new ItemStack(Material.FLINT_AND_STEEL);
            }

            @Override
            public String getMachineIdentifier() {
                return "TEST_EMPTY_INPUT_MACHINE";
            }
        };
        emptyInputMachine.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        emptyInputMachine.register(plugin);
        emptyInputMachine.registerRecipe(4, new ItemStack[0], new ItemStack[] { new ItemStack(Material.DIAMOND) });
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private BlockMenu placeMachine(AContainer m, int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", m.getId(), true);
        return BlockStorage.getInventory(b);
    }

    @Test
    @DisplayName("A present Material absent from every recipe input shortcuts to idle without consuming")
    void testShortcutShapeEqualsFullScan() {
        BlockMenu menu = placeMachine(machine, 1, 1);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.OBSIDIAN, 64));

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNull(found, "A Material no recipe requires must never match, shortcut or not");
        Assertions.assertEquals(64, menu.getItemInSlot(INPUT_SLOT).getAmount(), "The junk input must not be consumed");
    }

    @Test
    @DisplayName("A machine recipe with an empty input array disables the shortcut and matches any input")
    void testEmptyInputRecipeForcesFullScan() {
        BlockMenu menu = placeMachine(emptyInputMachine, 2, 2);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.OBSIDIAN, 64));

        MachineRecipe found = emptyInputMachine.findNextRecipe(menu);

        Assertions.assertNotNull(found, "The empty-input recipe matches regardless of the present Material");
        Assertions.assertEquals(Material.DIAMOND, found.getOutput()[0].getType(), "The empty-input recipe's output must be produced");
    }

    @Test
    @DisplayName("A recipe registered after the first scan is found (index rebuild on growth)")
    void testLateRegisteredRecipeRebuild() {
        BlockMenu menu = placeMachine(machine, 3, 3);

        // First scan with a Material no recipe uses: builds/validates the index.
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BRICKS, 64));
        Assertions.assertNull(machine.findNextRecipe(menu), "Precondition: BRICKS matches nothing initially");

        // A late registration introduces a brand-new input Material.
        machine.registerRecipe(4, new ItemStack(Material.BRICKS), new ItemStack(Material.NETHERITE_INGOT));

        // Reset the slot contents (the earlier scan consumed nothing).
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.BRICKS, 64));
        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNotNull(found, "The late-registered recipe must be found after the index rebuilds");
        Assertions.assertEquals(Material.NETHERITE_INGOT, found.getOutput()[0].getType());
        Assertions.assertEquals(63, menu.getItemInSlot(INPUT_SLOT).getAmount(), "One input must have been consumed by the started operation");
    }

    @Test
    @DisplayName("A present Material in the recipe index still matches normally (shortcut stays dormant)")
    void testKnownMaterialStillMatches() {
        BlockMenu menu = placeMachine(machine, 4, 4);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.SAND, 64));

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNotNull(found, "A fully valid input must still match with the index in place");
        Assertions.assertEquals(Material.GLASS, found.getOutput()[0].getType());
    }

    @Test
    @DisplayName("An empty input slot scans idle both via shortcut and full scan")
    void testEmptySlotIdle() {
        BlockMenu menu = placeMachine(machine, 5, 5);

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNull(found, "No input at all must never match a recipe");
    }
}

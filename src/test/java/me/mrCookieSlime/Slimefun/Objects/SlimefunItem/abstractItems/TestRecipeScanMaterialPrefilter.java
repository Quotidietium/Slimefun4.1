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
 * Discriminating coverage for the Material prefilter in
 * {@link AContainer}'s recipe scan: recipes whose input Material is not
 * present in any input slot are skipped before the meta-aware comparison.
 *
 * <p>The prefilter must be invisible to behavior: it may never fabricate a
 * match, never skip a matchable recipe, and never change which recipe wins
 * when several share an input Material. These tests pin all three properties
 * against real {@link BlockStorage}-backed machines.
 *
 * @author Zurker
 */
class TestRecipeScanMaterialPrefilter {

    private static final int INPUT_SLOT = 19;
    private static final int INPUT_SLOT_2 = 20;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AContainer machine;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "prefilter_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_PREFILTER_MACHINE", Material.DISPENSER, "&fTest Prefilter Machine");
        Slimefun.getItemCfg().setValue("TEST_PREFILTER_MACHINE.enabled", true);
        machine = new AContainer(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public ItemStack getProgressBar() {
                return new ItemStack(Material.FLINT_AND_STEEL);
            }

            @Override
            public String getMachineIdentifier() {
                return "TEST_PREFILTER_MACHINE";
            }
        };
        machine.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        machine.register(plugin);

        /*
         * Recipe order matters (first match wins), so register in a fixed
         * order: a filler recipe (never matched here), the single-input
         * candidates in priority order, and a multi-input recipe at the end.
         */
        machine.registerRecipe(4, new ItemStack(Material.BEDROCK), new ItemStack(Material.DIAMOND));
        machine.registerRecipe(4, new ItemStack(Material.DIRT), new ItemStack(Material.GOLD_INGOT));
        machine.registerRecipe(4, new ItemStack(Material.DIRT), new ItemStack(Material.IRON_INGOT));
        machine.registerRecipe(4, new ItemStack[] { new ItemStack(Material.SAND), new ItemStack(Material.GRAVEL) }, new ItemStack[] { new ItemStack(Material.EMERALD) });
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
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", machine.getId(), true);
        return BlockStorage.getInventory(b);
    }

    @Test
    @DisplayName("A junk input whose Material matches no recipe leaves the machine idle")
    void testJunkInputNeverMatches() {
        BlockMenu menu = placeMachine(1, 1);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.OBSIDIAN, 64));

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNull(found, "A Material that no recipe requires must never match");
        Assertions.assertEquals(64, menu.getItemInSlot(INPUT_SLOT).getAmount(), "The junk input must not be consumed");
    }

    @Test
    @DisplayName("The first matching recipe in list order wins among same-Material recipes")
    void testPriorityOrderPreserved() {
        BlockMenu menu = placeMachine(2, 2);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.DIRT, 64));

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNotNull(found, "The matching recipe must not be skipped by the Material prefilter");
        ItemStack output = found.getOutput()[0];
        Assertions.assertEquals(Material.GOLD_INGOT, output.getType(), "The first registered DIRT recipe must win");
    }

    @Test
    @DisplayName("A multi-input recipe only matches when every input Material is present")
    void testMultiInputPartialPresenceSkipped() {
        BlockMenu menu = placeMachine(3, 3);
        // SAND present, GRAVEL absent: the multi-input recipe cannot match and must not start.
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.SAND, 64));

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNull(found, "A partially-satisfied multi-input recipe must not match");
        Assertions.assertEquals(64, menu.getItemInSlot(INPUT_SLOT).getAmount(), "The input must not be consumed");
    }

    @Test
    @DisplayName("A multi-input recipe matches and consumes both slots when fully satisfied")
    void testMultiInputFullPresenceMatches() {
        BlockMenu menu = placeMachine(4, 4);
        menu.replaceExistingItem(INPUT_SLOT, new ItemStack(Material.SAND, 32));
        menu.replaceExistingItem(INPUT_SLOT_2, new ItemStack(Material.GRAVEL, 32));

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNotNull(found, "The fully satisfied multi-input recipe must match");
        Assertions.assertEquals(Material.EMERALD, found.getOutput()[0].getType());

        ItemStack slot1 = menu.getItemInSlot(INPUT_SLOT);
        ItemStack slot2 = menu.getItemInSlot(INPUT_SLOT_2);
        // The recipe needs one of each input (registered with amount 1).
        Assertions.assertEquals(31, slot1 == null ? 0 : slot1.getAmount(), "Exactly one SAND must be consumed, got: " + slot1);
        Assertions.assertEquals(31, slot2 == null ? 0 : slot2.getAmount(), "Exactly one GRAVEL must be consumed, got: " + slot2);
    }

    @Test
    @DisplayName("Same Material but different meta must still be rejected by the full comparison")
    void testMetaMismatchStillRejected() {
        BlockMenu menu = placeMachine(5, 5);
        ItemStack named = new ItemStack(Material.DIRT, 64);
        named.editMeta(meta -> meta.setDisplayName("Renamed"));
        menu.replaceExistingItem(INPUT_SLOT, named);

        MachineRecipe found = machine.findNextRecipe(menu);

        /*
         * The prefilter lets this recipe through (same Material), so the
         * meta-aware isItemSimilar comparison must still reject it. This pins
         * that the prefilter never replaces the full comparison.
         */
        Assertions.assertNull(found, "A renamed stack of the right Material must still not match the plain recipe");
    }

    @Test
    @DisplayName("An empty input area reports no recipe for machines with only non-empty recipes")
    void testEmptyInputsNoMatch() {
        BlockMenu menu = placeMachine(6, 6);

        MachineRecipe found = machine.findNextRecipe(menu);

        Assertions.assertNull(found, "No inputs must mean no recipe");
    }
}

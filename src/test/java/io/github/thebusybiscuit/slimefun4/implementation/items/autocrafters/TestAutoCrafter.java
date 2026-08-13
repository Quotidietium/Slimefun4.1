package io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bakedlibs.dough.data.persistent.PersistentDataAPI;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.inventory.ChestInventoryMock;
import be.seeseemelk.mockbukkit.inventory.InventoryMock;

class TestAutoCrafter {

    private static Slimefun plugin;
    private static World world;

    @BeforeAll
    public static void load() {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test crafting a valid ShapelessRecipe")
    void testValidShapelessRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "shapeless_recipe_test");
        ItemStack result = CustomItemStack.create(Material.DIAMOND, "&6Special Diamond :o");
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new MaterialChoice(Material.IRON_NUGGET, Material.GOLD_NUGGET));

        AbstractRecipe abstractRecipe = AbstractRecipe.of(recipe);
        AbstractAutoCrafter crafter = getVanillaAutoCrafter();
        InventoryMock inv = new ChestInventoryMock(null, 9);

        // Test first choice
        inv.addItem(new ItemStack(Material.IRON_NUGGET));
        Assertions.assertTrue(crafter.craft(inv, abstractRecipe));
        Assertions.assertFalse(inv.contains(Material.IRON_NUGGET, 1));
        Assertions.assertTrue(inv.containsAtLeast(result, 1));

        inv.clear();

        // Test other choice
        inv.addItem(new ItemStack(Material.GOLD_NUGGET));
        Assertions.assertTrue(crafter.craft(inv, abstractRecipe));
        Assertions.assertFalse(inv.contains(Material.GOLD_NUGGET, 1));
        Assertions.assertTrue(inv.containsAtLeast(result, 1));
    }

    @Test
    @DisplayName("Test crafting a valid ShapelessRecipe")
    void testDisabledRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "disabled_recipe_test");
        ItemStack result = CustomItemStack.create(Material.DIAMOND, "&bAmazing Diamond :o");
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new MaterialChoice(Material.GOLD_NUGGET));

        AbstractRecipe abstractRecipe = AbstractRecipe.of(recipe);
        AbstractAutoCrafter crafter = getVanillaAutoCrafter();
        InventoryMock inv = new ChestInventoryMock(null, 9);

        // Test enabled Recipe
        abstractRecipe.setEnabled(true);
        inv.addItem(new ItemStack(Material.GOLD_NUGGET));
        Assertions.assertTrue(crafter.craft(inv, abstractRecipe));
        Assertions.assertFalse(inv.contains(Material.GOLD_NUGGET, 1));
        Assertions.assertTrue(inv.containsAtLeast(result, 1));

        inv.clear();

        // Test disabled Recipe
        abstractRecipe.setEnabled(false);
        inv.addItem(new ItemStack(Material.GOLD_NUGGET));
        Assertions.assertFalse(crafter.craft(inv, abstractRecipe));
        Assertions.assertTrue(inv.contains(Material.GOLD_NUGGET, 1));
        Assertions.assertFalse(inv.containsAtLeast(result, 1));
    }

    @Test
    @DisplayName("Test resource leftovers when crafting")
    void testResourceLeftovers() {
        NamespacedKey key = new NamespacedKey(plugin, "resource_leftovers_test");
        ItemStack result = CustomItemStack.create(Material.DIAMOND, "&9Diamond. Nuff said.");
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(new MaterialChoice(Material.HONEY_BOTTLE));
        recipe.addIngredient(new MaterialChoice(Material.HONEY_BOTTLE));

        AbstractRecipe abstractRecipe = AbstractRecipe.of(recipe);
        AbstractAutoCrafter crafter = getVanillaAutoCrafter();
        InventoryMock inv = new ChestInventoryMock(null, 9);

        inv.addItem(new ItemStack(Material.HONEY_BOTTLE, 2));
        Assertions.assertTrue(crafter.craft(inv, abstractRecipe));

        Assertions.assertFalse(inv.contains(Material.HONEY_BOTTLE, 2));
        Assertions.assertTrue(inv.containsAtLeast(result, 1));

        // Check for leftovers
        Assertions.assertTrue(inv.contains(Material.GLASS_BOTTLE, 2));
    }

    @Test
    @DisplayName("Test crafting an invalid ShapelessRecipe")
    void testInvalidShapelessRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "shapeless_recipe_test");
        ItemStack result = CustomItemStack.create(Material.DIAMOND, "&6Special Diamond :o");
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(Material.IRON_NUGGET);

        AbstractRecipe abstractRecipe = AbstractRecipe.of(recipe);
        AbstractAutoCrafter crafter = getVanillaAutoCrafter();
        InventoryMock inv = new ChestInventoryMock(null, 9);

        // Test non-compatible Item
        inv.addItem(new ItemStack(Material.BAMBOO));
        Assertions.assertFalse(crafter.craft(inv, abstractRecipe));
        Assertions.assertTrue(inv.contains(Material.BAMBOO, 1));
        Assertions.assertFalse(inv.containsAtLeast(result, 1));
    }

    @Test
    @DisplayName("Test crafting a ShapelessRecipe with a SlimefunItem")
    void ShapelessRecipeWithSlimefunItem() {
        NamespacedKey key = new NamespacedKey(plugin, "shapeless_recipe_test");
        ItemStack result = CustomItemStack.create(Material.DIAMOND, "&6Special Diamond :o");
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(Material.BAMBOO);

        AbstractRecipe abstractRecipe = AbstractRecipe.of(recipe);
        AbstractAutoCrafter crafter = getVanillaAutoCrafter();
        InventoryMock inv = new ChestInventoryMock(null, 9);

        SlimefunItemStack itemStack = new SlimefunItemStack("AUTO_CRAFTER_TEST_ITEM", Material.BAMBOO, "Panda Candy");
        SlimefunItem slimefunItem = TestUtilities.mockSlimefunItem(plugin, itemStack.getItemId(), itemStack.item());
        slimefunItem.register(plugin);

        inv.addItem(itemStack.item());

        // Test unusable SlimefunItem
        slimefunItem.setUseableInWorkbench(false);
        Assertions.assertFalse(crafter.craft(inv, abstractRecipe));
        Assertions.assertTrue(inv.containsAtLeast(itemStack.item(), 1));
        Assertions.assertFalse(inv.containsAtLeast(result, 1));

        // Test allowed SlimefunItem
        slimefunItem.setUseableInWorkbench(true);
        Assertions.assertTrue(crafter.craft(inv, abstractRecipe));
        Assertions.assertFalse(inv.containsAtLeast(itemStack.item(), 1));
        Assertions.assertTrue(inv.containsAtLeast(result, 1));
    }

    @Test
    @DisplayName("Test crafting with a full Inventory")
    void testFullInventory() {
        NamespacedKey key = new NamespacedKey(plugin, "shapeless_recipe_test");
        ItemStack result = CustomItemStack.create(Material.DIAMOND, "&6Special Diamond :o");
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(Material.IRON_NUGGET);

        AbstractRecipe abstractRecipe = AbstractRecipe.of(recipe);
        AbstractAutoCrafter crafter = getVanillaAutoCrafter();

        InventoryMock inv = new ChestInventoryMock(null, 9);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, new ItemStack(Material.OAK_SAPLING));
        }

        // Test valid item but inventory is full.
        inv.addItem(new ItemStack(Material.IRON_NUGGET));
        Assertions.assertFalse(crafter.craft(inv, abstractRecipe));
        Assertions.assertTrue(inv.contains(Material.OAK_SAPLING, 9));
        Assertions.assertFalse(inv.containsAtLeast(result, 1));
    }

    @Test
    @DisplayName("Verify Auto Crafters are marked as energy consumers")
    void testEnergyConsumer() {
        AbstractAutoCrafter crafter = getVanillaAutoCrafter();
        Assertions.assertEquals(EnergyNetComponentType.CONSUMER, crafter.getEnergyComponentType());
    }

    @Test
    @DisplayName("A corrupted stored recipe key is tolerated as no recipe instead of crashing the ticker")
    void testCorruptedRecipeKeyReturnsNull() {
        VanillaAutoCrafter crafter = (VanillaAutoCrafter) getVanillaAutoCrafter();
        Block b = world.getBlockAt(1, 10, 1);
        b.setType(Material.PLAYER_HEAD);

        // Self-calibration: confirm MockBukkit actually models this block as a Skull with a
        // PersistentDataContainer, otherwise getSelectedRecipe would short-circuit on the
        // instanceof check and the corruption path below would never be exercised.
        Assertions.assertTrue(b.getState() instanceof Skull, "MockBukkit must model PLAYER_HEAD as a Skull for this test to be meaningful");

        // A valid, registered recipe round-trips through the skull's persistent data.
        NamespacedKey key = new NamespacedKey(plugin, "corruption_round_trip_test");
        ItemStack result = CustomItemStack.create(Material.DIAMOND, "&aRound Trip Diamond");
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(Material.GOLD_NUGGET);
        org.bukkit.Bukkit.addRecipe(recipe);

        Skull validSkull = (Skull) b.getState();
        PersistentDataAPI.setString(validSkull, crafter.recipeStorageKey, key.toString());
        validSkull.update(true, false);

        AbstractRecipe resolved = crafter.getSelectedRecipe(b);
        Assertions.assertNotNull(resolved, "A valid stored recipe key must resolve back to a recipe");

        // Now corrupt the stored value: an invalid NamespacedKey (illegal namespace char) would
        // previously throw IllegalArgumentException out of getSelectedRecipe into the BlockTicker,
        // which destroys the Auto-Crafter after four errors. It must now fail closed to null.
        Skull corruptedSkull = (Skull) b.getState();
        PersistentDataAPI.setString(corruptedSkull, crafter.recipeStorageKey, "invalid@namespace:key");
        corruptedSkull.update(true, false);

        Assertions.assertDoesNotThrow(() -> crafter.getSelectedRecipe(b), "A corrupted recipe key must not propagate an exception");
        Assertions.assertNull(crafter.getSelectedRecipe(b), "A corrupted recipe key must resolve to no recipe");
    }

    @Test
    @DisplayName("SlimefunAutoCrafter tolerates a stored item whose RecipeType no longer matches")
    void testMismatchedRecipeTypeReturnsNull() {
        // A crafter that targets RecipeType.NULL
        SlimefunItemStack crafterStack = new SlimefunItemStack("TEST_SF_CRAFTER_MISMATCH", Material.PLAYER_HEAD, "&7Test SF Crafter");
        SlimefunAutoCrafter crafter = new SlimefunAutoCrafter(TestUtilities.getItemGroup(plugin, "sf_crafter_mismatch"), crafterStack, RecipeType.NULL, new ItemStack[9], RecipeType.NULL);
        crafter.setCapacity(100);
        crafter.setEnergyConsumption(10);
        crafter.register(plugin);

        // An item registered under a DIFFERENT RecipeType - of(item, targetRecipeType) returns null
        SlimefunItemStack itemStack = new SlimefunItemStack("TEST_MISMATCH_ITEM", Material.GOLD_INGOT, "&6Mismatch Item");
        SlimefunItem item = new SlimefunItem(TestUtilities.getItemGroup(plugin, "mismatch_item"), itemStack, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[9]);
        item.register(plugin);

        Block b = world.getBlockAt(2, 10, 2);
        b.setType(Material.PLAYER_HEAD);
        Assertions.assertTrue(b.getState() instanceof Skull, "MockBukkit must model PLAYER_HEAD as a Skull");

        Skull skull = (Skull) b.getState();
        // Store the mismatched item's id as the selected recipe
        PersistentDataAPI.setString(skull, crafter.recipeStorageKey, item.getId());
        skull.update(true, false);

        Assertions.assertDoesNotThrow(() -> crafter.getSelectedRecipe(b), "A RecipeType mismatch must not propagate an NPE into the ticker");
        Assertions.assertNull(crafter.getSelectedRecipe(b), "A stored item whose RecipeType no longer matches must resolve to no recipe");
    }

    @Nonnull
    private AbstractAutoCrafter getVanillaAutoCrafter() {
        SlimefunItemStack item = new SlimefunItemStack("MOCK_AUTO_CRAFTER", Material.CRAFTING_TABLE, "Mock Auto Crafter");
        return new VanillaAutoCrafter(TestUtilities.getItemGroup(plugin, "auto_crafter"), item, RecipeType.NULL, new ItemStack[9]);
    }

}

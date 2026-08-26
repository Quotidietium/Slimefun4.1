package io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.AfterAll;
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

/**
 * Regression coverage for the consumption-failure path of
 * {@link AbstractAutoCrafter#craft(Inventory, AbstractRecipe, ItemStack)}:
 * once the ingredients have been consumed, every produced item must be accounted for.
 * An event-modified result can exceed the max stack size, making {@code Inventory#addItem}
 * overflow even though a free slot existed - the overflow (and the bucket-type leftovers)
 * used to be silently voided in that case.
 *
 * @author Zurker
 */
class TestAutoCrafterCraftOverflow {

    private static ServerMock server;
    private static Slimefun plugin;
    private static org.bukkit.World world;

    private static AbstractAutoCrafter crafter;
    private static ShapelessRecipe vanillaRecipe;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "auto_crafter_overflow");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_OVERFLOW_AUTO_CRAFTER", Material.PLAYER_HEAD, "&7Test Auto Crafter");

        crafter = new AbstractAutoCrafter(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public AbstractRecipe getSelectedRecipe(@Nonnull Block b) {
                return null;
            }

            @Override
            protected void updateRecipe(@Nonnull Block b, @Nonnull org.bukkit.entity.Player p) {}
        };
        crafter.setCapacity(100);
        crafter.setEnergyConsumption(10);
        crafter.register(plugin);

        vanillaRecipe = new ShapelessRecipe(new NamespacedKey(plugin, "auto_crafter_overflow_recipe"), new ItemStack(Material.IRON_INGOT));
        vanillaRecipe.addIngredient(Material.WATER_BUCKET);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        for (Entity entity : world.getEntities()) {
            entity.remove();
        }
    }

    /**
     * Fills a chest with filler material, one water bucket ingredient and exactly one
     * remaining empty slot.
     */
    @Nonnull
    private Inventory prepareChest(int x, int z) {
        Block chest = world.getBlockAt(x, 0, z);
        chest.setType(Material.CHEST);
        Inventory inv = ((InventoryHolder) chest.getState()).getInventory();
        inv.clear();

        // setItem (not addItem): addItem would stack the filler into a single slot
        for (int i = 0; i < 25; i++) {
            inv.setItem(i, new ItemStack(Material.STONE));
        }

        inv.setItem(25, new ItemStack(Material.WATER_BUCKET));
        assertEquals(26, inv.firstEmpty(), "Sanity: exactly one empty slot before crafting");
        return inv;
    }

    private int countInInventory(@Nonnull Inventory inv, @Nonnull Material type) {
        int total = 0;

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == type && item.getAmount() > 0) {
                total += item.getAmount();
            }
        }

        return total;
    }

    private int countOnGround(@Nonnull Material type) {
        int total = 0;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item item && item.getItemStack().getType() == type) {
                total += item.getItemStack().getAmount();
            }
        }

        return total;
    }

    @Test
    @DisplayName("A normal craft stores the result and the bucket leftover without dropping anything")
    void testNormalCraftStoresEverything() {
        Inventory inv = prepareChest(10, 0);
        AbstractRecipe recipe = AbstractRecipe.of(vanillaRecipe);

        boolean crafted = crafter.craft(inv, recipe, new ItemStack(Material.IRON_INGOT));

        assertTrue(crafted, "A normal craft must succeed");
        assertEquals(1, countInInventory(inv, Material.IRON_INGOT));
        assertEquals(1, countInInventory(inv, Material.BUCKET) + countOnGround(Material.BUCKET), "The bucket leftover must not be voided");
        assertEquals(0, countOnGround(Material.IRON_INGOT));
    }

    @Test
    @DisplayName("An oversized event-modified result must not void the overflow or the leftovers")
    void testOversizedResultDoesNotVoidItems() {
        Inventory inv = prepareChest(20, 0);
        AbstractRecipe recipe = AbstractRecipe.of(vanillaRecipe);

        // 128 exceeds the max stack size of 64: one stack fits into the single empty
        // slot, the other half can no longer be stored and must be dropped, not voided
        boolean crafted = crafter.craft(inv, recipe, new ItemStack(Material.IRON_INGOT, 128));

        assertTrue(crafted, "The craft consumed the ingredients and produced the result");
        assertEquals(128, countInInventory(inv, Material.IRON_INGOT) + countOnGround(Material.IRON_INGOT), "Every produced ingot must be either in the chest or on the ground");
        assertEquals(1, countInInventory(inv, Material.BUCKET) + countOnGround(Material.BUCKET), "The bucket leftover must be either in the chest or on the ground");
    }
}

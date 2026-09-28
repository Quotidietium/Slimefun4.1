package benchmark;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;

/**
 * A processing machine with a large recipe list, standing in for the
 * big-recipe machines a live server runs (Electric Smeltery-class items with
 * hundreds of entries). The {@code recipe-scan} scenario uses it to measure
 * the full recipe-scan cost that every input change forces.
 *
 * <p>Extends {@link BenchMachine} to inherit its widened
 * {@code takeCharge}/{@code findNextRecipe} bridges; only the recipe list and
 * the identifier differ.
 */
public class BenchHeftyMachine extends BenchMachine {

    public static final String ID = "BENCH_HEFTY_MACHINE";

    /**
     * The number of recipes registered. Ten recipes (like {@link BenchMachine})
     * is the Electric Furnace class; this covers the heavy class.
     */
    public static final int RECIPE_COUNT = 150;

    /**
     * A Material that is guaranteed not to appear in any recipe input, so a
     * stack of it in the input slot forces the scan to walk the entire recipe
     * list without matching anything.
     */
    public static final Material JUNK_MATERIAL = junkMaterial();

    BenchHeftyMachine(ItemGroup itemGroup, SlimefunItemStack item) {
        super(itemGroup, item);
    }

    @Override
    public String getMachineIdentifier() {
        return ID;
    }

    @Override
    protected void registerDefaultRecipes() {
        List<Material> itemMaterials = recipeMaterials(RECIPE_COUNT * 2 + 1);

        for (int i = 0; i < RECIPE_COUNT; i++) {
            registerRecipe(10, new ItemStack(itemMaterials.get(i)), new ItemStack(itemMaterials.get(i + 1)));
        }
    }

    private static Material junkMaterial() {
        // The material right after the last recipe output - never an input.
        return recipeMaterials(RECIPE_COUNT * 2 + 2).get(RECIPE_COUNT * 2 + 1);
    }

    private static List<Material> recipeMaterials(int count) {
        List<Material> itemMaterials = new ArrayList<>(count);

        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) {
                itemMaterials.add(material);

                if (itemMaterials.size() >= count) {
                    break;
                }
            }
        }

        return itemMaterials;
    }
}


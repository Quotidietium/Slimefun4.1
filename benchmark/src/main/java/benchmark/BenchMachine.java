package benchmark;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;

/**
 * A minimal electric machine with a realistic recipe count, standing in for
 * the machines a live server runs (default Slimefun items are not registered
 * in the MockBukkit unit-test environment, so the benchmark provides its own).
 */
public class BenchMachine extends AContainer {

    public static final String ID = "BENCH_MACHINE";

    BenchMachine(ItemGroup itemGroup, SlimefunItemStack item) {
        super(itemGroup, item, RecipeType.NULL, new ItemStack[9]);

        setCapacity(128);
        setEnergyConsumption(4);
        setProcessingSpeed(1);
    }

    @Override
    public String getMachineIdentifier() {
        return ID;
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.FLINT_AND_STEEL);
    }

    @Override
    protected void registerDefaultRecipes() {
        // Ten recipes, roughly the Electric Furnace's recipe count.
        registerRecipe(10, new ItemStack(Material.COBBLESTONE), new ItemStack(Material.STONE));
        registerRecipe(10, new ItemStack(Material.IRON_ORE), new ItemStack(Material.IRON_INGOT));
        registerRecipe(10, new ItemStack(Material.GOLD_ORE), new ItemStack(Material.GOLD_INGOT));
        registerRecipe(10, new ItemStack(Material.SAND), new ItemStack(Material.GLASS));
        registerRecipe(10, new ItemStack(Material.CLAY_BALL), new ItemStack(Material.BRICK));
        registerRecipe(10, new ItemStack(Material.NETHERRACK), new ItemStack(Material.NETHER_BRICK));
        registerRecipe(10, new ItemStack(Material.BEEF), new ItemStack(Material.COOKED_BEEF));
        registerRecipe(10, new ItemStack(Material.POTATO), new ItemStack(Material.BAKED_POTATO));
        registerRecipe(10, new ItemStack(Material.OAK_LOG), new ItemStack(Material.CHARCOAL));
        registerRecipe(10, new ItemStack(Material.WET_SPONGE), new ItemStack(Material.SPONGE));
    }
}

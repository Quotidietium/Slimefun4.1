package benchmark;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;

/**
 * A minimal burning generator, standing in for the generators a live server
 * runs (default Slimefun items are not registered in the MockBukkit
 * environment, so the benchmark provides its own).
 *
 * <p>The fuel burns for an extremely long time so that a whole benchmark
 * session stays inside the steady "operation in progress" branch of
 * {@code AGenerator#getGeneratedOutput(Location, Config)} instead of cycling
 * through fuel-consumption and byproduct branches.
 */
public class BenchGenerator extends AGenerator {

    public static final String ID = "BENCH_GENERATOR";

    BenchGenerator(ItemGroup itemGroup, SlimefunItemStack item) {
        super(itemGroup, item, RecipeType.NULL, new ItemStack[9]);

        setCapacity(512);
        setEnergyProduction(4);
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.FLINT_AND_STEEL);
    }

    @Override
    protected void registerDefaultFuelTypes() {
        registerFuel(new MachineFuel(36000, new ItemStack(Material.COAL)));
        registerFuel(new MachineFuel(36000, new ItemStack(Material.CHARCOAL)));
    }
}

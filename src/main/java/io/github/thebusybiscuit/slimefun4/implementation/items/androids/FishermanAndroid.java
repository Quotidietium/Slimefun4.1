package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.collections.RandomizedSet;
import io.github.thebusybiscuit.slimefun4.api.events.AndroidFishEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;

import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

public class FishermanAndroid extends ProgrammableAndroid {

    private final RandomizedSet<ItemStack> fishingLoot = new RandomizedSet<>();

    @ParametersAreNonnullByDefault
    public FishermanAndroid(ItemGroup itemGroup, int tier, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, tier, item, recipeType, recipe);

        // Fish
        for (Material fish : Tag.ITEMS_FISHES.getValues()) {
            fishingLoot.add(new ItemStack(fish), 25);
        }

        // Junk
        fishingLoot.add(new ItemStack(Material.BONE), 10);
        fishingLoot.add(new ItemStack(Material.STRING), 10);
        fishingLoot.add(new ItemStack(Material.INK_SAC), 8);
        fishingLoot.add(new ItemStack(Material.KELP), 6);
        fishingLoot.add(new ItemStack(Material.STICK), 5);
        fishingLoot.add(new ItemStack(Material.ROTTEN_FLESH), 3);
        fishingLoot.add(new ItemStack(Material.LEATHER), 2);
        fishingLoot.add(new ItemStack(Material.BAMBOO), 3);

        // "loot"
        fishingLoot.add(new ItemStack(Material.SADDLE), 1);
        fishingLoot.add(new ItemStack(Material.NAME_TAG), 1);
        fishingLoot.add(new ItemStack(Material.NAUTILUS_SHELL), 1);
    }

    @Override
    public AndroidType getAndroidType() {
        return AndroidType.FISHERMAN;
    }

    @Override
    protected void fish(Block b, BlockMenu menu) {
        Block water = b.getRelative(BlockFace.DOWN);

        if (water.getType() == Material.WATER) {
            SoundEffect.FISHERMAN_ANDROID_FISHING_SOUND.playAt(water);

            if (ThreadLocalRandom.current().nextInt(100) < 10 * getTier()) {
                ItemStack drop = fishingLoot.getRandom();

                /*
                 * Fire an AndroidFishEvent before inserting the catch. Cancellation
                 * discards the catch; listeners may also replace the drop to implement
                 * custom loot tables. Gated on registered listeners to keep the
                 * default path allocation-free.
                 */
                if (AndroidFishEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    AndroidFishEvent event = new AndroidFishEvent(new AndroidInstance(this, b), drop.clone());
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return;
                    }

                    drop = event.getDrop();
                }

                ItemStack rest = menu.pushItem(drop.clone(), getOutputSlots());

                if (rest != null) {
                    // The output slots are full: drop the catch instead of voiding it
                    b.getWorld().dropItemNaturally(b.getLocation(), rest);
                }
            }
        }
    }
}

package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.AndroidFarmEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

public class FarmerAndroid extends ProgrammableAndroid {

    @ParametersAreNonnullByDefault
    public FarmerAndroid(ItemGroup itemGroup, int tier, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, tier, item, recipeType, recipe);
    }

    @Override
    public AndroidType getAndroidType() {
        return getTier() == 1 ? AndroidType.FARMER : AndroidType.ADVANCED_FARMER;
    }

    @Override
    protected void farm(Block b, BlockMenu menu, Block block, boolean isAdvanced) {
        Material blockType = block.getType();
        BlockData data = block.getBlockData();
        ItemStack drop = null;

        if (!block.getWorld().getWorldBorder().isInside(block.getLocation())) {
            return;
        }

        // The owner must be allowed to break blocks here. Otherwise a Farmer Android could wander
        // along the edge of another player's claim and silently harvest their crops. Ownerless
        // (legacy) androids skip farming entirely, consistent with MinerAndroid.
        OfflinePlayer owner = getOwner(b);

        if (owner == null || !Slimefun.getProtectionManager().hasPermission(owner, block.getLocation(), Interaction.BREAK_BLOCK)) {
            return;
        }

        if (data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
            drop = getDropFromCrop(blockType);
        }

        AndroidInstance instance = new AndroidInstance(this, b);

        AndroidFarmEvent event = new AndroidFarmEvent(block, instance, isAdvanced, drop);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            drop = event.getDrop();

            if (drop != null) {
                ItemStack leftover = menu.pushItem(drop, getOutputSlots());

                if (leftover != null) {
                    // The output slots are full - drop the excess on the ground instead of voiding it
                    block.getWorld().dropItemNaturally(block.getLocation(), leftover);
                }

                // Replant the crop regardless of whether the output was full: the harvest happened,
                // so the crop must be reset. Previously, a full output left the crop at max age AND
                // voided the leftover drop every tick (data loss + soft stall).
                block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, blockType);

                if (data instanceof Ageable ageable) {
                    ageable.setAge(0);
                    block.setBlockData(data);
                }
            }
        }
    }

    private ItemStack getDropFromCrop(Material crop) {
        Random random = ThreadLocalRandom.current();

        return switch (crop) {
            case WHEAT -> new ItemStack(Material.WHEAT, random.nextInt(2) + 1);
            case POTATOES -> new ItemStack(Material.POTATO, random.nextInt(3) + 1);
            case CARROTS -> new ItemStack(Material.CARROT, random.nextInt(3) + 1);
            case BEETROOTS -> new ItemStack(Material.BEETROOT, random.nextInt(3) + 1);
            case COCOA -> new ItemStack(Material.COCOA_BEANS, random.nextInt(3) + 1);
            case NETHER_WART -> new ItemStack(Material.NETHER_WART, random.nextInt(3) + 1);
            case SWEET_BERRY_BUSH -> new ItemStack(Material.SWEET_BERRIES, random.nextInt(3) + 1);
            default -> null;
        };
    }

}

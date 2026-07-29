package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedParticle;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

public class AnimalGrowthAccelerator extends AbstractGrowthAccelerator {

    private static final int ENERGY_CONSUMPTION = 14;
    private static final double RADIUS = 3.0;

    // We wanna strip the Slimefun Item id here
    private static final ItemStack organicFood = ItemStackWrapper.wrap(SlimefunItems.ORGANIC_FOOD.item());

    public AnimalGrowthAccelerator(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        addItemHandler(SlimefunUtils.ownerTrackingPlaceHandler());
    }

    @Override
    public int getCapacity() {
        return 1024;
    }

    @Override
    protected void tick(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);
        OfflinePlayer owner = SlimefunUtils.getOwner(b.getLocation());

        // Only accelerate animals the machine's owner may interact with, so this cannot speed up
        // growth in a neighbour's pen across a claim border.
        for (Entity n : b.getWorld().getNearbyEntities(b.getLocation(), RADIUS, RADIUS, RADIUS, en -> isReadyToGrow(en, owner))) {
            for (int slot : getInputSlots()) {
                if (SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), organicFood, false, false)) {
                    if (getCharge(b.getLocation()) < ENERGY_CONSUMPTION) {
                        return;
                    }

                    Ageable ageable = (Ageable) n;
                    removeCharge(b.getLocation(), ENERGY_CONSUMPTION);
                    inv.consumeItem(slot);
                    ageable.setAge(ageable.getAge() + 2000);

                    if (ageable.getAge() > 0) {
                        ageable.setAge(0);
                    }

                    n.getWorld().spawnParticle(VersionedParticle.HAPPY_VILLAGER, ((LivingEntity) n).getEyeLocation(), 8, 0.2F, 0.2F, 0.2F);
                    return;
                }
            }
        }
    }

    private boolean isReadyToGrow(Entity n, OfflinePlayer owner) {
        if (n instanceof Ageable ageable && n.isValid() && !ageable.isAdult()) {
            return owner == null || Slimefun.getProtectionManager().hasPermission(owner, n.getLocation(), Interaction.INTERACT_ENTITY);
        }

        return false;
    }

}

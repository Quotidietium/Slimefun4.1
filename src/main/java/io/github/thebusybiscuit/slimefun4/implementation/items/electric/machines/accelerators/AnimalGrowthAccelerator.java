package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.AnimalAccelerateEvent;
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
    private static final int AGE_BOOST = 2000;
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

                    Integer boost = ageBoost(b, (Ageable) n, inv.getItemInSlot(slot));

                    if (boost == null) {
                        // An addon vetoed the acceleration of this animal; try the next slot.
                        break;
                    }

                    Ageable ageable = (Ageable) n;
                    removeCharge(b.getLocation(), ENERGY_CONSUMPTION);
                    inv.consumeItem(slot);
                    ageable.setAge(ageable.getAge() + boost);

                    if (ageable.getAge() > 0) {
                        ageable.setAge(0);
                    }

                    n.getWorld().spawnParticle(VersionedParticle.HAPPY_VILLAGER, ((LivingEntity) n).getEyeLocation(), 8, 0.2F, 0.2F, 0.2F);
                    return;
                }
            }
        }
    }

    /**
     * Fires an {@link AnimalAccelerateEvent} if any listener is registered and returns
     * the age boost to apply to this animal, or {@code null} when a listener cancelled
     * the acceleration. Without listeners this costs nothing and the old behavior is
     * preserved.
     */
    @Nullable
    @ParametersAreNonnullByDefault
    private Integer ageBoost(Block machine, Ageable animal, ItemStack food) {
        if (AnimalAccelerateEvent.getHandlerList().getRegisteredListeners().length > 0) {
            AnimalAccelerateEvent event = new AnimalAccelerateEvent(this, machine, animal, food);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }

            // An addon may have adjusted the age boost
            return event.getAgeBoost();
        }

        return AGE_BOOST;
    }

    private boolean isReadyToGrow(Entity n, OfflinePlayer owner) {
        if (n instanceof Ageable ageable && n.isValid() && !ageable.isAdult()) {
            return owner == null || Slimefun.getProtectionManager().hasPermission(owner, n.getLocation(), Interaction.INTERACT_ENTITY);
        }

        return false;
    }

}

package io.github.thebusybiscuit.slimefun4.implementation.items.medical;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.MedicineConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

public class Medicine extends MedicalSupply<ItemConsumptionHandler> {

    @ParametersAreNonnullByDefault
    public Medicine(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, 8, item, recipeType, recipe);
    }

    @Override
    public ItemConsumptionHandler getItemHandler() {
        return (e, p, item) -> {
            int healAmt = getHealAmount();

            if (MedicineConsumeEvent.getHandlerList().getRegisteredListeners().length > 0) {
                MedicineConsumeEvent event = new MedicineConsumeEvent(p, this, healAmt);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                healAmt = event.getHealAmount();
            }

            p.setFireTicks(0);
            clearNegativeEffects(p);
            RadiationUtils.clearExposure(p);
            heal(p, healAmt);
        };
    }

}

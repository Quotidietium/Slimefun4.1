package io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.WaterStaffExtinguishEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * The {@link WaterStaff} is a magical {@link SlimefunItem}.
 * When you right click it, any fire on your {@link Player} will be extinguished.
 * 
 * @author TheBusyBiscuit
 *
 */
public class WaterStaff extends SimpleSlimefunItem<ItemUseHandler> {

    @ParametersAreNonnullByDefault
    public WaterStaff(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            Player p = e.getPlayer();

            if (WaterStaffExtinguishEvent.getHandlerList().getRegisteredListeners().length > 0) {
                WaterStaffExtinguishEvent event = new WaterStaffExtinguishEvent(p, WaterStaff.this);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }
            }

            p.setFireTicks(0);
            Slimefun.getLocalization().sendMessage(p, "messages.fire-extinguish", true);
        };
    }

}

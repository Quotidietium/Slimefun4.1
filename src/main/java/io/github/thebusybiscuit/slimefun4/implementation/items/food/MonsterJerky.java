package io.github.thebusybiscuit.slimefun4.implementation.items.food;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.github.thebusybiscuit.slimefun4.api.events.MonsterJerkyConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.SlimefunFood;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * {@link MonsterJerky} is basically just Rotten Flesh but without the Hunger Effect.
 * 
 * @author TheBusyBiscuit
 * 
 * @see MeatJerky
 *
 */
public class MonsterJerky extends SimpleSlimefunItem<ItemConsumptionHandler> implements SlimefunFood {

    @ParametersAreNonnullByDefault
    public MonsterJerky(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public ItemConsumptionHandler getItemHandler() {
        return (e, p, item) -> {
            PotionEffect effect = new PotionEffect(PotionEffectType.SATURATION, 5, 0);

            if (MonsterJerkyConsumeEvent.getHandlerList().getRegisteredListeners().length > 0) {
                MonsterJerkyConsumeEvent event = new MonsterJerkyConsumeEvent(p, this, effect);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                effect = event.getEffect();
            }

            PotionEffect applied = effect;
            Slimefun.runSync(() -> {
                if (p.hasPotionEffect(PotionEffectType.HUNGER)) {
                    p.removePotionEffect(PotionEffectType.HUNGER);
                }

                p.addPotionEffect(applied);
            }, 1L);
        };
    }

}

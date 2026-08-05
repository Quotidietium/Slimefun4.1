package io.github.thebusybiscuit.slimefun4.implementation.items.food;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.github.thebusybiscuit.slimefun4.api.events.DietCookieConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.SlimefunFood;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * The {@link DietCookie} gives you a {@link PotionEffect} of Type {@code PotionEffectType.LEVITATION}
 * when consumed.
 * 
 * @author Linox
 * 
 * @see FortuneCookie
 * @see ItemConsumptionHandler
 *
 */
public class DietCookie extends SimpleSlimefunItem<ItemConsumptionHandler> implements SlimefunFood {

    @ParametersAreNonnullByDefault
    public DietCookie(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @Nonnull ItemConsumptionHandler getItemHandler() {
        return (e, p, item) -> {
            PotionEffect effect = PotionEffectType.LEVITATION.createEffect(60, 1);

            if (DietCookieConsumeEvent.getHandlerList().getRegisteredListeners().length > 0) {
                DietCookieConsumeEvent event = new DietCookieConsumeEvent(p, this, effect);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                effect = event.getEffect();
            }

            Slimefun.getLocalization().sendMessage(p, "messages.diet-cookie");
            SoundEffect.DIET_COOKIE_CONSUME_SOUND.playFor(p);

            if (p.hasPotionEffect(PotionEffectType.LEVITATION)) {
                p.removePotionEffect(PotionEffectType.LEVITATION);
            }

            p.addPotionEffect(effect);
        };
    }
}

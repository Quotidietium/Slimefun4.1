package io.github.thebusybiscuit.slimefun4.implementation.items.medical;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.events.SplintHealEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedAttribute;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedPotionEffectType;

public class Splint extends SimpleSlimefunItem<ItemUseHandler> {

    @ParametersAreNonnullByDefault
    public Splint(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
    }

    @Override
    public @Nonnull ItemUseHandler getItemHandler() {
        return e -> {
            Player p = e.getPlayer();

            // Player is neither burning nor injured
            var maxHealthAttr = p.getAttribute(VersionedAttribute.MAX_HEALTH);
            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;

            if (p.getFireTicks() <= 0 && p.getHealth() >= maxHealth) {
                return;
            }

            PotionEffect effect = new PotionEffect(VersionedPotionEffectType.INSTANT_HEALTH, 1, 0);

            if (SplintHealEvent.getHandlerList().getRegisteredListeners().length > 0) {
                SplintHealEvent event = new SplintHealEvent(p, this, effect);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    e.cancel();
                    return;
                }

                effect = event.getEffect();
            }

            if (p.getGameMode() != GameMode.CREATIVE) {
                ItemUtils.consumeItem(e.getItem(), false);
            }

            SoundEffect.SPLINT_CONSUME_SOUND.playFor(p);
            p.addPotionEffect(effect);

            e.cancel();
        };
    }
}

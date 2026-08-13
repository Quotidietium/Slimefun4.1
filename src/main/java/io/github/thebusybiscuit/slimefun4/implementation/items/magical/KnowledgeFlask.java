package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.events.KnowledgeFlaskFillEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * The {@link KnowledgeFlask} is a magical {@link SlimefunItem} which allows you to store
 * experience levels in a bottle when you right click.
 * 
 * @author TheBusyBiscuit
 *
 */
public class KnowledgeFlask extends SimpleSlimefunItem<ItemUseHandler> {

    @ParametersAreNonnullByDefault
    public KnowledgeFlask(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
    }

    @Override
    public @Nonnull ItemUseHandler getItemHandler() {
        return e -> {
            e.cancel();
            Player p = e.getPlayer();

            if (p.getLevel() >= 1 && (e.getClickedBlock().isEmpty() || !(e.getClickedBlock().get().getType().isInteractable()))) {
                KnowledgeFlaskFillEvent event = new KnowledgeFlaskFillEvent(p, this, e.getItem(), 1, SlimefunItems.FILLED_FLASK_OF_KNOWLEDGE.item());
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }

                int levelCost = event.getLevelCost();

                if (levelCost > p.getLevel()) {
                    /*
                     * The event may have raised the cost above what the player has:
                     * deducting it would drive their level negative. Fail closed -
                     * nothing is consumed and nothing is produced.
                     */
                    Slimefun.getLocalization().sendMessage(p, "messages.not-enough-xp", true);
                    return;
                }

                p.setLevel(p.getLevel() - levelCost);

                ItemStack item = event.getResult();

                if (!p.getInventory().addItem(item).isEmpty()) {
                    // The Item could not be added, let's drop it to the ground (fixes #2728)
                    p.getWorld().dropItemNaturally(p.getLocation(), item);
                }

                SoundEffect.FLASK_OF_KNOWLEDGE_FILLUP_SOUND.playFor(p);
                ItemUtils.consumeItem(e.getItem(), false);
            }
        };
    }

}

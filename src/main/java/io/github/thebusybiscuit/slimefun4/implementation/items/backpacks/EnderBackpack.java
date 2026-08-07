package io.github.thebusybiscuit.slimefun4.implementation.items.backpacks;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.EnderBackpackOpenEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * The {@link EnderBackpack} is a pretty simple {@link SlimefunItem} which opens your
 * {@link EnderChest} upon right clicking.
 *
 * @author TheBusyBiscuit
 *
 */
public class EnderBackpack extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    @ParametersAreNonnullByDefault
    public EnderBackpack(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @Nonnull ItemUseHandler getItemHandler() {
        return e -> {
            Player p = e.getPlayer();

            if (EnderBackpackOpenEvent.getHandlerList().getRegisteredListeners().length > 0) {
                EnderBackpackOpenEvent event = new EnderBackpackOpenEvent(p, this);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    /*
                     * An addon vetoed the opening; the interaction is still consumed,
                     * so the right-click does not fall through to the clicked block.
                     */
                    e.cancel();
                    return;
                }
            }

            p.openInventory(p.getEnderChest());
            SoundEffect.ENDER_BACKPACK_OPEN_SOUND.playFor(p);
            e.cancel();
        };
    }
}

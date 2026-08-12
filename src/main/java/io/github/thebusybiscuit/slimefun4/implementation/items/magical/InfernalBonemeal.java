package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.events.InfernalBonemealGrowEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * The {@link InfernalBonemeal} is a special type of bone meal which will work on
 * Nether Warts.
 * 
 * @author TheBusyBiscuit
 *
 */
public class InfernalBonemeal extends SimpleSlimefunItem<ItemUseHandler> {

    @ParametersAreNonnullByDefault
    public InfernalBonemeal(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            Optional<Block> block = e.getClickedBlock();
            e.setUseBlock(Result.DENY);

            if (block.isPresent()) {
                Block b = block.get();

                if (b.getType() == Material.NETHER_WART) {
                    Ageable ageable = (Ageable) b.getBlockData();

                    if (ageable.getAge() < ageable.getMaximumAge()) {
                        Integer targetAge = growTargetAge(e.getPlayer(), b, ageable);

                        if (targetAge != null) {
                            ageable.setAge(targetAge);
                            b.setBlockData(ageable);
                            // BlockData is the primary data type of this effect
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK.createBlockData());

                            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                                ItemUtils.consumeItem(e.getItem(), false);
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * Fires an {@link InfernalBonemealGrowEvent} if any listener is registered and
     * returns the age the Nether Wart will be grown to, or {@code null} when a listener
     * cancelled the growth. Without listeners this costs nothing and the old behavior
     * is preserved.
     */
    @Nullable
    @ParametersAreNonnullByDefault
    private Integer growTargetAge(Player p, Block b, Ageable ageable) {
        if (InfernalBonemealGrowEvent.getHandlerList().getRegisteredListeners().length > 0) {
            InfernalBonemealGrowEvent event = new InfernalBonemealGrowEvent(p, this, b, ageable.getMaximumAge());
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }

            // An addon may have adjusted how far the wart grows
            return event.getTargetAge();
        }

        return ageable.getMaximumAge();
    }

}

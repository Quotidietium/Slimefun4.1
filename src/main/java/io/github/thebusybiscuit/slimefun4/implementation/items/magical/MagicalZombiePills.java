package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.MagicalZombiePillsCureEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityInteractHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * This {@link SlimefunItem} allows you to convert any {@link ZombieVillager} to
 * their {@link Villager} variant. It is also one of the very few utilisations of {@link EntityInteractHandler}.
 *
 * @author Linox
 *
 * @see EntityInteractHandler
 *
 */
public class MagicalZombiePills extends SimpleSlimefunItem<EntityInteractHandler> implements NotPlaceable {

    @ParametersAreNonnullByDefault
    public MagicalZombiePills(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        addItemHandler(onRightClick());
    }

    @Override
    public @Nonnull EntityInteractHandler getItemHandler() {
        return (e, item, offhand) -> {
            Entity entity = e.getRightClicked();

            if (e.isCancelled() || !Slimefun.getProtectionManager().hasPermission(e.getPlayer(), entity.getLocation(), Interaction.INTERACT_ENTITY)) {
                // They don't have permission to use it in this area
                return;
            }

            Player p = e.getPlayer();

            if (entity instanceof ZombieVillager zombieVillager) {
                MagicalZombiePillsCureEvent event = fireCureEvent(p, entity, item);

                if (event != null && event.isCancelled()) {
                    return;
                }

                useItem(p, item);
                healZombieVillager(zombieVillager, p, event != null ? event.getConversionTime() : 1);
            } else if (entity instanceof PigZombie pigZombie) {
                MagicalZombiePillsCureEvent event = fireCureEvent(p, entity, item);

                if (event != null && event.isCancelled()) {
                    return;
                }

                useItem(p, item);
                healZombifiedPiglin(pigZombie);
            }
        };
    }

    /**
     * Fires a {@link MagicalZombiePillsCureEvent} if any addon is listening.
     *
     * @return The fired event, or null when no addon is listening
     */
    @javax.annotation.Nullable
    private MagicalZombiePillsCureEvent fireCureEvent(@Nonnull Player p, @Nonnull Entity entity, @Nonnull ItemStack item) {
        if (MagicalZombiePillsCureEvent.getHandlerList().getRegisteredListeners().length > 0) {
            MagicalZombiePillsCureEvent event = new MagicalZombiePillsCureEvent(p, this, entity, item);
            Bukkit.getPluginManager().callEvent(event);

            return event;
        }

        return null;
    }

    /**
     * This method cancels {@link PlayerRightClickEvent} to prevent placing {@link MagicalZombiePills}.
     *
     * @return the {@link ItemUseHandler} of this {@link SlimefunItem}
     */
    public ItemUseHandler onRightClick() {
        return PlayerRightClickEvent::cancel;
    }

    private void useItem(@Nonnull Player p, @Nonnull ItemStack item) {
        if (p.getGameMode() != GameMode.CREATIVE) {
            ItemUtils.consumeItem(item, false);
        }

        // This is supposed to be a vanilla sound. No need for a SoundEffect
        p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1, 1);
    }

    private void healZombieVillager(@Nonnull ZombieVillager zombieVillager, @Nonnull Player p, int conversionTime) {
        zombieVillager.setConversionTime(conversionTime);
        zombieVillager.setConversionPlayer(p);
    }

    private void healZombifiedPiglin(@Nonnull PigZombie zombiePiglin) {
        Location loc = zombiePiglin.getLocation();

        zombiePiglin.remove();
        loc.getWorld().spawnEntity(loc, EntityType.PIGLIN);
    }
}

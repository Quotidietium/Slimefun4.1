package io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.NetherStarReactorWitherEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;

/**
 * The {@link NetherStarReactor} is an implementation of {@link Reactor} that consumes
 * Nether Stars and adds Withering to any nearby {@link LivingEntity}
 * 
 * @author John000708
 * 
 * @see NuclearReactor
 *
 */
public abstract class NetherStarReactor extends Reactor {

    @ParametersAreNonnullByDefault
    protected NetherStarReactor(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    protected void registerDefaultFuelTypes() {
        registerFuel(new MachineFuel(1800, new ItemStack(Material.NETHER_STAR)));
    }

    @Override
    public void extraTick(@Nonnull Location l) {
        Slimefun.runSync(() -> {
            OfflinePlayer owner = SlimefunUtils.getOwner(l);

            // Respect claim protection: only wither entities the reactor's owner may attack, so a
            // Nether Star Reactor at a claim border cannot wither a neighbour's mobs or players.
            for (Entity entity : l.getWorld().getNearbyEntities(l, 5, 5, 5, n -> {
                if (!(n instanceof LivingEntity) || !n.isValid()) {
                    return false;
                }

                if (owner == null) {
                    return true;
                }

                Interaction interaction = n instanceof Player ? Interaction.ATTACK_PLAYER : Interaction.ATTACK_ENTITY;
                return Slimefun.getProtectionManager().hasPermission(owner, n.getLocation(), interaction);
            })) {
                LivingEntity living = (LivingEntity) entity;
                PotionEffect witherEffect = new PotionEffect(PotionEffectType.WITHER, 60, 1);

                if (NetherStarReactorWitherEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    NetherStarReactorWitherEvent event = new NetherStarReactorWitherEvent(this, l, living);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        // An addon vetoed the withering of this entity.
                        continue;
                    }

                    // An addon may have replaced the applied effect
                    witherEffect = event.getEffect();
                }

                living.addPotionEffect(witherEffect);
            }
        });
    }

    @Override
    public ItemStack getCoolant() {
        return SlimefunItems.NETHER_ICE_COOLANT_CELL.item();
    }

    @Override
    public ItemStack getFuelIcon() {
        return new ItemStack(Material.NETHER_STAR);
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.NETHER_STAR);
    }

}

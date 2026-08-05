package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.events.SwordOfBeheadingDropEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.SlimefunWeapon;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityKillHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * The {@link SwordOfBeheading} is a special kind of sword which allows you to obtain
 * {@link Zombie}, {@link Skeleton}, {@link Creeper} and {@link Piglin} skulls when killing the respective
 * {@link Monster}.
 * Additionally, you can also obtain the head of a {@link Player} by killing them too.
 * This sword also allows you to have a higher chance of getting the skull of a {@link WitherSkeleton} too.
 * 
 * All chances are managed by an {@link ItemSetting} and can be configured.
 * 
 * @author TheBusyBiscuit
 *
 */
public class SwordOfBeheading extends SimpleSlimefunItem<EntityKillHandler> implements SlimefunWeapon {

    private final ItemSetting<Integer> chanceZombie = new IntRangeSetting(this, "chance.ZOMBIE", 0, 40, 100);
    private final ItemSetting<Integer> chanceSkeleton = new IntRangeSetting(this, "chance.SKELETON", 0, 40, 100);
    private final ItemSetting<Integer> chanceWitherSkeleton = new IntRangeSetting(this, "chance.WITHER_SKELETON", 0, 25, 100);
    private final ItemSetting<Integer> chanceCreeper = new IntRangeSetting(this, "chance.CREEPER", 0, 40, 100);
    private final ItemSetting<Integer> chancePiglin = new IntRangeSetting(this, "chance.PIGLIN", 0, 40, 100);
    private final ItemSetting<Integer> chancePlayer = new IntRangeSetting(this, "chance.PLAYER", 0, 70, 100);

    @ParametersAreNonnullByDefault
    public SwordOfBeheading(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemSetting(chanceZombie, chanceSkeleton, chanceWitherSkeleton, chanceCreeper, chancePiglin, chancePlayer);
    }

    @Override
    public EntityKillHandler getItemHandler() {
        return (e, entity, killer, item) -> {
            Random random = ThreadLocalRandom.current();

            switch (e.getEntityType()) {
                case ZOMBIE -> {
                    if (random.nextInt(100) < chanceZombie.getValue()) {
                        dropHead(e, killer, new ItemStack(Material.ZOMBIE_HEAD));
                    }
                }
                case SKELETON -> {
                    if (random.nextInt(100) < chanceSkeleton.getValue()) {
                        dropHead(e, killer, new ItemStack(Material.SKELETON_SKULL));
                    }
                }
                case CREEPER -> {
                    if (random.nextInt(100) < chanceCreeper.getValue()) {
                        dropHead(e, killer, new ItemStack(Material.CREEPER_HEAD));
                    }
                }
                case WITHER_SKELETON -> {
                    if (random.nextInt(100) < chanceWitherSkeleton.getValue()) {
                        dropHead(e, killer, new ItemStack(Material.WITHER_SKELETON_SKULL));
                    }
                }
                case PIGLIN -> {
                    if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_20) &&
                        random.nextInt(100) < chancePiglin.getValue()) {
                        dropHead(e, killer, new ItemStack(Material.PIGLIN_HEAD));
                    }
                }
                case PLAYER -> {
                    if (random.nextInt(100) < chancePlayer.getValue()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);

                        ItemMeta meta = skull.getItemMeta();
                        ((SkullMeta) meta).setOwningPlayer((Player) e.getEntity());
                        skull.setItemMeta(meta);

                        dropHead(e, killer, skull);
                    }
                }
                default -> {}
            }
        };
    }

    @ParametersAreNonnullByDefault
    private void dropHead(EntityDeathEvent e, Player killer, ItemStack head) {
        if (SwordOfBeheadingDropEvent.getHandlerList().getRegisteredListeners().length > 0) {
            SwordOfBeheadingDropEvent event = new SwordOfBeheadingDropEvent(killer, this, e.getEntity(), head);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            head = event.getHead();
        }

        e.getDrops().add(head);
    }

}

package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.AndroidAttackEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

public class ButcherAndroid extends ProgrammableAndroid {

    private static final String METADATA_KEY = "android_killer";

    @ParametersAreNonnullByDefault
    public ButcherAndroid(ItemGroup itemGroup, int tier, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, tier, item, recipeType, recipe);
    }

    @Override
    public AndroidType getAndroidType() {
        return AndroidType.FIGHTER;
    }

    @Override
    protected void attack(Block b, BlockFace face, Predicate<LivingEntity> predicate) {
        double damage = getTier() >= 3 ? 20D : 4D * getTier();
        double radius = 4.0 + getTier();

        // Only an android with a known owner may attack, and only entities the owner is allowed
        // to attack. Otherwise a Butcher Android could slaughter another player's livestock (or a
        // named/villager mob) across a claim border and have the drops collected by the android.
        // Ownerless (legacy) androids skip attacking entirely, consistent with MinerAndroid.
        OfflinePlayer owner = getOwner(b);

        if (owner == null) {
            return;
        }

        for (Entity n : b.getWorld().getNearbyEntities(b.getLocation(), radius, radius, radius, n -> n instanceof LivingEntity livingEntity && !(n instanceof ArmorStand) && !(n instanceof Player) && n.isValid() && predicate.test(livingEntity))) {
            // Check if our android is facing this entity.
            boolean willAttack = switch (face) {
                case NORTH -> n.getLocation().getZ() < b.getZ();
                case EAST -> n.getLocation().getX() > b.getX();
                case SOUTH -> n.getLocation().getZ() > b.getZ();
                case WEST -> n.getLocation().getX() < b.getX();
                default -> false;
            };

            if (willAttack) {
                // Skip entities the owner is not allowed to attack and try the next facing one.
                if (!Slimefun.getProtectionManager().hasPermission(owner, n.getLocation(), Interaction.ATTACK_ENTITY)) {
                    continue;
                }

                AndroidInstance instance = new AndroidInstance(this, b);

                /*
                 * Fire an AndroidAttackEvent before damaging the entity. Cancellation
                 * skips this entity and continues with the next facing one - the same
                 * semantics as a protection denial above. Gated on registered listeners
                 * to keep the default path allocation-free.
                 */
                if (AndroidAttackEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    AndroidAttackEvent event = new AndroidAttackEvent(instance, (LivingEntity) n, damage);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        continue;
                    }

                    damage = event.getDamage();
                }

                if (n.hasMetadata(METADATA_KEY)) {
                    n.removeMetadata(METADATA_KEY, Slimefun.instance());
                }

                n.setMetadata(METADATA_KEY, new FixedMetadataValue(Slimefun.instance(), instance));

                ((LivingEntity) n).damage(damage);
                break;
            }
        }
    }

}

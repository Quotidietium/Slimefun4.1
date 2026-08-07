package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunArmorChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunArmorEffectEvent;
import io.github.thebusybiscuit.slimefun4.api.items.HashedArmorpiece;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;

/**
 * The {@link SlimefunArmorTask} is responsible for handling {@link SlimefunArmorPiece}
 *
 * @author TheBusyBiscuit
 * @author martinbrom
 * @author Semisol
 */
public class SlimefunArmorTask extends AbstractArmorTask {

    @Override
    @ParametersAreNonnullByDefault
    protected void onPlayerTick(Player p, PlayerProfile profile) {
        ItemStack[] armor = p.getInventory().getArmorContents();
        updateAndHandleArmor(p, armor, profile.getArmor());
    }

    @ParametersAreNonnullByDefault
    private void updateAndHandleArmor(Player p, ItemStack[] armor, HashedArmorpiece[] cachedArmor) {
        for (int slot = 0; slot < 4; slot++) {
            ItemStack item = armor[slot];
            HashedArmorpiece armorPiece = cachedArmor[slot];

            if (armorPiece.hasDiverged(item)) {
                SlimefunItem sfItem = SlimefunItem.getByItem(item);

                if (!(sfItem instanceof SlimefunArmorPiece)) {
                    // If it isn't actually Armor, then we won't care about it.
                    sfItem = null;
                }

                if (SlimefunArmorChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    SlimefunArmorPiece previousArmor = armorPiece.getItem().orElse(null);

                    armorPiece.update(item, sfItem);

                    SlimefunArmorPiece newArmor = armorPiece.getItem().orElse(null);

                    if (previousArmor != null || newArmor != null) {
                        /*
                         * Notify addons about the armor change. The cache was already updated
                         * above, so this is a plain notification with no veto semantics. A swap
                         * between two non-Slimefun pieces stays silent by design.
                         */
                        SlimefunArmorChangeEvent event = new SlimefunArmorChangeEvent(p, slot, previousArmor, item, newArmor);
                        Bukkit.getPluginManager().callEvent(event);
                    }
                } else {
                    armorPiece.update(item, sfItem);
                }
            }

            if (item != null && armorPiece.getItem().isPresent()) {
                Slimefun.runSync(() -> {
                    SlimefunArmorPiece sfArmorPiece = armorPiece.getItem().get();

                    if (sfArmorPiece.canUse(p, true)) {
                        onArmorPieceTick(p, sfArmorPiece, item);
                    }
                });
            }
        }
    }

    /**
     * Method to handle behavior for pieces of armor.
     * It is called per-player and per piece of armor.
     *
     * @param p
     *            The {@link Player} wearing the piece of armor
     * @param sfArmorPiece
     *            {@link SlimefunArmorPiece} Slimefun instance of the piece of armor
     * @param armorPiece
     *            The actual {@link ItemStack} of the armor piece
     */
    @ParametersAreNonnullByDefault
    protected void onArmorPieceTick(Player p, SlimefunArmorPiece sfArmorPiece, ItemStack armorPiece) {
        PotionEffect[] effects = sfArmorPiece.getPotionEffects();

        if (effects.length == 0) {
            // Nothing to apply - do not spam events for effect-less armor (e.g. Hazmat)
            return;
        }

        /*
         * Fire a SlimefunArmorEffectEvent before applying the effects. Cancellation
         * skips this armor piece for this tick; the effects are re-evaluated on the
         * next armor tick. Gated on registered listeners to keep this hot path
         * (per player, per piece, per armor interval) allocation-free by default.
         */
        if (SlimefunArmorEffectEvent.getHandlerList().getRegisteredListeners().length > 0) {
            SlimefunArmorEffectEvent event = new SlimefunArmorEffectEvent(p, sfArmorPiece, armorPiece, effects);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }
        }

        for (PotionEffect effect : effects) {
            p.removePotionEffect(effect.getType());
            p.addPotionEffect(effect);
        }
    }
}

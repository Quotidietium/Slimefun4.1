package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import io.github.thebusybiscuit.slimefun4.api.events.RainbowArmorCycleEvent;
import io.github.thebusybiscuit.slimefun4.api.items.HashedArmorpiece;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.RainbowArmorPiece;

/**
 * The {@link RainbowArmorTask} is responsible for handling the change in color of any Rainbow Armor piece.
 *
 * @author martinbrom
 */
public class RainbowArmorTask extends AbstractArmorTask {

    private long currentColorIndex = 0;

    @Override
    protected void onTick() {
        currentColorIndex++;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void onPlayerTick(Player p, PlayerProfile profile) {
        for (int i = 0; i < 4; i++) {
            ItemStack item = p.getInventory().getArmorContents()[i];

            if (item != null && item.hasItemMeta()) {
                HashedArmorpiece armorPiece = profile.getArmor()[i];

                armorPiece.getItem().ifPresent(sfArmorPiece -> {
                    if (sfArmorPiece instanceof RainbowArmorPiece rainbowArmorPiece && rainbowArmorPiece.canUse(p, true)) {
                        updateRainbowArmor(p, item, rainbowArmorPiece);
                    }
                });
            }
        }
    }

    /**
     * Applies the next color of the given {@link RainbowArmorPiece}'s sequence to the
     * worn {@link ItemStack}. Extracted and widened to package-private visibility so the
     * color change can be driven directly: the profile armor cache cannot be populated
     * under MockBukkit. Without any {@link RainbowArmorCycleEvent} listeners the behavior
     * is identical to the original inline body.
     *
     * @param p
     *            The {@link Player} wearing the armor piece
     * @param itemStack
     *            The live armor {@link ItemStack}
     * @param armorPiece
     *            The {@link RainbowArmorPiece} to cycle
     */
    @ParametersAreNonnullByDefault
    void updateRainbowArmor(Player p, ItemStack itemStack, RainbowArmorPiece armorPiece) {
        if (!(itemStack.getItemMeta() instanceof LeatherArmorMeta leatherArmorMeta)) {
            return;
        }

        Color[] colors = armorPiece.getColors();
        Color newColor = colors[(int) (currentColorIndex % colors.length)];

        if (RainbowArmorCycleEvent.getHandlerList().getRegisteredListeners().length > 0) {
            RainbowArmorCycleEvent event = new RainbowArmorCycleEvent(p, armorPiece, itemStack, leatherArmorMeta.getColor(), newColor);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed this color change; the armor keeps its current color.
                return;
            }

            newColor = event.getNewColor();
        }

        leatherArmorMeta.setColor(newColor);

        // Apply the color change on the main thread: this task runs asynchronously, and writing
        // the full ItemMeta back to the live armor item off-thread races with main-thread meta
        // changes (e.g. durability damage), which could clobber them (effectively granting the
        // rainbow armor piece free durability). Re-reading the meta on the main thread also
        // ensures we don't apply a color to an item that was swapped in the meantime.
        Color finalColor = newColor;
        io.github.thebusybiscuit.slimefun4.implementation.Slimefun.runSync(() -> {
            if (itemStack.getItemMeta() instanceof LeatherArmorMeta liveMeta) {
                liveMeta.setColor(finalColor);
                itemStack.setItemMeta(liveMeta);
            }
        });
    }
}

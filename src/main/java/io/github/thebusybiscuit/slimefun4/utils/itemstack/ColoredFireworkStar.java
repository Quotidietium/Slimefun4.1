package io.github.thebusybiscuit.slimefun4.utils.itemstack;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedItemFlag;

/**
 * This simple {@link ItemStack} implementation allows us to obtain
 * a colored {@code Material.FIREWORK_STAR} {@link ItemStack} quickly.
 *
 * @author TheBusyBiscuit
 *
 */
public class ColoredFireworkStar {

    @ParametersAreNonnullByDefault
    public static ItemStack create(Color color, String name, String... lore) {
        FireworkEffect effect = FireworkEffect.builder().with(Type.BURST).withColor(color).build();
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(ChatColors.color(name));
            }

            ((FireworkEffectMeta) meta).setEffect(effect);

            if (lore.length > 0) {
                List<String> lines = new ArrayList<>();

                for (String line : lore) {
                    lines.add(ChatColors.color(line));
                }

                meta.setLore(lines);
            }

            meta.addItemFlags(VersionedItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }

        return item;
    }

}

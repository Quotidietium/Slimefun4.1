package io.github.thebusybiscuit.slimefun4.core.guide.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.events.GuideModeChangeEvent;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

class GuideModeOption implements SlimefunGuideOption<SlimefunGuideMode> {

    @Nonnull
    @Override
    public SlimefunAddon getAddon() {
        return Slimefun.instance();
    }

    @Nonnull
    @Override
    public NamespacedKey getKey() {
        return new NamespacedKey(Slimefun.instance(), "guide_mode");
    }

    @Nonnull
    @Override
    public Optional<ItemStack> getDisplayItem(Player p, ItemStack guide) {
        if (!p.hasPermission("slimefun.cheat.items")) {
            // Only Players with the appropriate permission can access the cheat sheet
            return Optional.empty();
        }

        Optional<SlimefunGuideMode> current = getSelectedOption(p, guide);

        if (current.isPresent()) {
            SlimefunGuideMode selectedMode = current.get();
            ItemStack item = new ItemStack(Material.AIR);

            if (selectedMode == SlimefunGuideMode.SURVIVAL_MODE) {
                item.setType(Material.CHEST);
            } else {
                item.setType(Material.COMMAND_BLOCK);
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + Slimefun.getLocalization().getMessage(p, "guide.modes.selected") +
                    ChatColor.YELLOW + Slimefun.getLocalization().getMessage(p, "guide.modes." + selectedMode.name()));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add((selectedMode == SlimefunGuideMode.SURVIVAL_MODE ? ChatColor.GREEN : ChatColor.GRAY) + Slimefun.getLocalization().getMessage(p, "guide.modes.SURVIVAL_MODE"));
            lore.add((selectedMode == SlimefunGuideMode.CHEAT_MODE ? ChatColor.GREEN : ChatColor.GRAY) + Slimefun.getLocalization().getMessage(p, "guide.modes.CHEAT_MODE"));

            lore.add("");
            lore.add(ChatColor.GRAY + "\u21E8 " + ChatColor.YELLOW + Slimefun.getLocalization().getMessage(p, "guide.modes.change"));
            meta.setLore(lore);
            item.setItemMeta(meta);

            return Optional.of(item);
        }

        return Optional.empty();
    }

    @Override
    public void onClick(@Nonnull Player p, @Nonnull ItemStack guide) {
        Optional<SlimefunGuideMode> current = getSelectedOption(p, guide);

        if (current.isPresent()) {
            SlimefunGuideMode next = getNextMode(p, current.get());
            applyModeChange(p, guide, current.get(), next);
        }

        SlimefunGuideSettings.openSettings(p, guide);
    }

    /**
     * Applies a guide mode change to the given guide item.
     * Extracted from {@link #onClick(Player, ItemStack)} so the switch logic can be
     * driven directly: reopening the settings menu touches services that cannot run
     * under MockBukkit. Without any {@link GuideModeChangeEvent} listeners the behavior
     * is identical to the original inline call.
     *
     * @param p
     *            The {@link Player} who clicked the option
     * @param guide
     *            The guide item to rewrite
     * @param current
     *            The current {@link SlimefunGuideMode}
     * @param next
     *            The {@link SlimefunGuideMode} to switch to
     */
    @ParametersAreNonnullByDefault
    void applyModeChange(Player p, ItemStack guide, SlimefunGuideMode current, SlimefunGuideMode next) {
        if (next != current && GuideModeChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            GuideModeChangeEvent event = new GuideModeChangeEvent(p, guide, current, next);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed the mode change; the guide keeps its current mode.
                return;
            }
        }

        setSelectedOption(p, guide, next);
    }

    @Nonnull
    private SlimefunGuideMode getNextMode(@Nonnull Player p, @Nonnull SlimefunGuideMode mode) {
        if (p.hasPermission("slimefun.cheat.items")) {
            if (mode == SlimefunGuideMode.SURVIVAL_MODE) {
                return SlimefunGuideMode.CHEAT_MODE;
            } else {
                return SlimefunGuideMode.SURVIVAL_MODE;
            }
        } else {
            return SlimefunGuideMode.SURVIVAL_MODE;
        }
    }

    @Nonnull
    @Override
    public Optional<SlimefunGuideMode> getSelectedOption(@Nonnull Player p, @Nonnull ItemStack guide) {
        if (SlimefunUtils.isItemSimilar(guide, SlimefunGuide.getItem(SlimefunGuideMode.CHEAT_MODE), true, false)) {
            return Optional.of(SlimefunGuideMode.CHEAT_MODE);
        } else {
            return Optional.of(SlimefunGuideMode.SURVIVAL_MODE);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void setSelectedOption(Player p, ItemStack guide, SlimefunGuideMode value) {
        guide.setItemMeta(SlimefunGuide.getItem(value).getItemMeta());
    }

}

package io.github.thebusybiscuit.slimefun4.core.guide.options;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.data.persistent.PersistentDataAPI;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerGuideOptionChangeEvent;
import io.github.thebusybiscuit.slimefun4.core.SlimefunRegistry;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * {@link LearningAnimationOption} represents a setting in the Slimefun guide book.
 * It allows users to disable/enable the "learning animation",
 * the information in chat when doing a Slimefun research.
 *
 * @author martinbrom
 */
class LearningAnimationOption implements SlimefunGuideOption<Boolean> {

    @Nonnull
    @Override
    public SlimefunAddon getAddon() {
        return Slimefun.instance();
    }

    @Nonnull
    @Override
    public NamespacedKey getKey() {
        return new NamespacedKey(Slimefun.instance(), "research_learning_animation");
    }

    @Nonnull
    @Override
    public Optional<ItemStack> getDisplayItem(@Nonnull Player p, @Nonnull ItemStack guide) {
        SlimefunRegistry registry = Slimefun.getRegistry();

        if (!registry.isResearchingEnabled() || registry.isLearningAnimationDisabled()) {
            return Optional.empty();
        } else {
            boolean enabled = getSelectedOption(p, guide).orElse(true);
            String optionState = enabled ? "enabled" : "disabled";
            List<String> lore = Slimefun.getLocalization().getMessages(p, "guide.options.learning-animation." + optionState + ".text");
            lore.add("");
            lore.add("&7\u21E8 " + Slimefun.getLocalization().getMessage(p, "guide.options.learning-animation." + optionState + ".click"));

            ItemStack item = CustomItemStack.create(enabled ? Material.MAP : Material.PAPER, lore);
            return Optional.of(item);
        }
    }

    @Override
    public void onClick(@Nonnull Player p, @Nonnull ItemStack guide) {
        applyOptionChange(p, guide, !getSelectedOption(p, guide).orElse(true));
        SlimefunGuideSettings.openSettings(p, guide);
    }

    /**
     * Applies a learning-animation-setting change: fires a vetoable
     * {@link PlayerGuideOptionChangeEvent} first (only if anyone is listening), then stores
     * the new value. A vetoed change leaves the stored value untouched. Extracted from
     * {@link #onClick(Player, ItemStack)} so the switch logic can be driven directly:
     * reopening the settings menu touches services that cannot run under MockBukkit.
     * Without listeners the behavior is identical to the original inline call.
     *
     * @param p
     *            The {@link Player} who clicked the option
     * @param guide
     *            The guide item
     * @param newValue
     *            The toggled value
     */
    void applyOptionChange(@Nonnull Player p, @Nonnull ItemStack guide, boolean newValue) {
        boolean previousValue = getSelectedOption(p, guide).orElse(true);

        if (newValue != previousValue && PlayerGuideOptionChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            PlayerGuideOptionChangeEvent event = new PlayerGuideOptionChangeEvent(p, PlayerGuideOptionChangeEvent.Reason.LEARNING_ANIMATION, previousValue, newValue);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed this change; the stored value stays as it is.
                return;
            }

            newValue = event.getNewValue();
        }

        setSelectedOption(p, guide, newValue);
    }

    @Override
    public Optional<Boolean> getSelectedOption(@Nonnull Player p, @Nonnull ItemStack guide) {
        NamespacedKey key = getKey();
        boolean value = !PersistentDataAPI.hasByte(p, key) || PersistentDataAPI.getByte(p, key) == (byte) 1;
        return Optional.of(value);
    }

    @Override
    public void setSelectedOption(@Nonnull Player p, @Nonnull ItemStack guide, @Nonnull Boolean value) {
        PersistentDataAPI.setByte(p, getKey(), (byte) (value.booleanValue() ? 1 : 0));
    }

}

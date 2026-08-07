package io.github.thebusybiscuit.slimefun4.core.guide.options;

import java.util.List;
import java.util.Optional;

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

class FireworksOption implements SlimefunGuideOption<Boolean> {

    @Override
    public SlimefunAddon getAddon() {
        return Slimefun.instance();
    }

    @Override
    public NamespacedKey getKey() {
        return new NamespacedKey(Slimefun.instance(), "research_fireworks");
    }

    @Override
    public Optional<ItemStack> getDisplayItem(Player p, ItemStack guide) {
        SlimefunRegistry registry = Slimefun.getRegistry();

        if (registry.isResearchingEnabled() && registry.isResearchFireworkEnabled()) {
            boolean enabled = getSelectedOption(p, guide).orElse(true);

            String optionState = enabled ? "enabled" : "disabled";
            List<String> lore = Slimefun.getLocalization().getMessages(p, "guide.options.fireworks." + optionState + ".text");
            lore.add("");
            lore.add("&7\u21E8 " + Slimefun.getLocalization().getMessage(p, "guide.options.fireworks." + optionState + ".click"));

            ItemStack item = CustomItemStack.create(Material.FIREWORK_ROCKET, lore);
            return Optional.of(item);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void onClick(Player p, ItemStack guide) {
        applyOptionChange(p, guide, !getSelectedOption(p, guide).orElse(true));
        SlimefunGuideSettings.openSettings(p, guide);
    }

    /**
     * Applies a fireworks-setting change: fires a vetoable
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
    void applyOptionChange(Player p, ItemStack guide, boolean newValue) {
        boolean previousValue = getSelectedOption(p, guide).orElse(true);

        if (newValue != previousValue && PlayerGuideOptionChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            PlayerGuideOptionChangeEvent event = new PlayerGuideOptionChangeEvent(p, PlayerGuideOptionChangeEvent.Reason.RESEARCH_FIREWORKS, previousValue, newValue);
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
    public Optional<Boolean> getSelectedOption(Player p, ItemStack guide) {
        NamespacedKey key = getKey();
        boolean value = !PersistentDataAPI.hasByte(p, key) || PersistentDataAPI.getByte(p, key) == (byte) 1;
        return Optional.of(value);
    }

    @Override
    public void setSelectedOption(Player p, ItemStack guide, Boolean value) {
        PersistentDataAPI.setByte(p, getKey(), value.booleanValue() ? (byte) 1 : (byte) 0);
    }

}

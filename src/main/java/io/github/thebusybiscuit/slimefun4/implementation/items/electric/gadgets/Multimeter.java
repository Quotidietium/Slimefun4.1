package io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.MultimeterReadEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;

/**
 * The {@link Multimeter} is used to measure charge and capacity of any {@link EnergyNetComponent}.
 * 
 * @author TheBusyBiscuit
 * 
 * @see EnergyNet
 * @see EnergyNetComponent
 *
 */
public class Multimeter extends SimpleSlimefunItem<ItemUseHandler> {

    @ParametersAreNonnullByDefault
    public Multimeter(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            Optional<SlimefunItem> block = e.getSlimefunBlock();

            if (e.getClickedBlock().isPresent() && block.isPresent()) {
                SlimefunItem item = block.get();

                if (item instanceof EnergyNetComponent component && component.isChargeable()) {
                    e.cancel();

                    Location l = e.getClickedBlock().get().getLocation();
                    int stored = component.getCharge(l);
                    int capacity = component.getCapacity();
                    Player p = e.getPlayer();

                    if (MultimeterReadEvent.getHandlerList().getRegisteredListeners().length > 0) {
                        MultimeterReadEvent event = new MultimeterReadEvent(p, this, l, component, stored, capacity);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            // An addon handled or vetoed the readout; send nothing.
                            return;
                        }
                    }

                    String storedText = NumberUtils.getCompactDouble(stored) + " J";
                    String capacityText = NumberUtils.getCompactDouble(capacity) + " J";

                    p.sendMessage("");
                    Slimefun.getLocalization().sendMessage(p, "messages.multimeter", false, str -> str.replace("%stored%", storedText).replace("%capacity%", capacityText));
                    p.sendMessage("");
                }
            }
        };
    }
}

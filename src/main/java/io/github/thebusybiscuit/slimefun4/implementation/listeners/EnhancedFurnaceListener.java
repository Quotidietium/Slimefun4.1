package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.EnhancedFurnaceBurnEvent;
import io.github.thebusybiscuit.slimefun4.api.events.EnhancedFurnaceSmeltEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import io.papermc.lib.PaperLib;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * This {@link Listener} is responsible for enforcing the "fuel efficiency" and "fortune" policies
 * of an {@link EnhancedFurnace}.
 * 
 * @author TheBusyBiscuit
 * 
 * @see EnhancedFurnace
 *
 */
public class EnhancedFurnaceListener implements Listener {

    public EnhancedFurnaceListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFuelBurn(FurnaceBurnEvent e) {
        if (e.getBlock().getType() != Material.FURNACE) {
            // We don't care about Smokers, Blast Furnaces and all that fancy stuff
            return;
        }

        SlimefunItem furnace = BlockStorage.check(e.getBlock());

        // Fixes #2958
        if (furnace instanceof EnhancedFurnace enhancedFurnace
            && !enhancedFurnace.isDisabledIn(e.getBlock().getWorld())
            && enhancedFurnace.getFuelEfficiency() > 0
        ) {
            EnhancedFurnaceBurnEvent event = new EnhancedFurnaceBurnEvent(enhancedFurnace, e.getBlock(), e);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            int burnTime = e.getBurnTime();
            // Multiply as a long before clamping: an addon can raise the fuel efficiency via
            // EnhancedFurnaceBurnEvent#setFuelEfficiency, and an int * int here would overflow
            // (producing a negative burn time) before the Short.MAX_VALUE clamp could take effect.
            long newBurnTime = (long) event.getFuelEfficiency() * burnTime;

            e.setBurnTime((int) Math.min(newBurnTime, Short.MAX_VALUE - 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSmelt(FurnaceSmeltEvent e) {
        if (e.getBlock().getType() != Material.FURNACE) {
            // We don't care about Smokers, Blast Furnaces and all that fancy stuff
            return;
        }

        SlimefunItem sfItem = BlockStorage.check(e.getBlock());

        if (sfItem instanceof EnhancedFurnace enhancedFurnace && !enhancedFurnace.isDisabledIn(e.getBlock().getWorld())) {
            BlockState state = PaperLib.getBlockState(e.getBlock(), false).getState();

            if (state instanceof Furnace furnace) {
                FurnaceInventory inventory = furnace.getInventory();

                // This if statement fixes #3741
                if (inventory.getSmelting() == null) {
                    return;
                }

                boolean multiplier = SlimefunTag.ENHANCED_FURNACE_LUCK_MATERIALS.isTagged(inventory.getSmelting().getType());
                int amount = multiplier ? enhancedFurnace.getRandomOutputAmount() : 1;
                Optional<ItemStack> result = Slimefun.getMinecraftRecipeService().getFurnaceOutput(inventory.getSmelting());

                if (result.isPresent()) {
                    ItemStack item = result.get();
                    int previous = inventory.getResult() != null ? inventory.getResult().getAmount() : 0;
                    amount = Math.min(item.getMaxStackSize() - previous, amount);

                    // amount may resolve to 0 when the result slot is already full. ItemStack cannot
                    // be constructed with amount 0 (throws IllegalArgumentException), so skip in that case.
                    if (amount > 0) {
                        EnhancedFurnaceSmeltEvent event = new EnhancedFurnaceSmeltEvent(enhancedFurnace, e.getBlock(), e, amount);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            return;
                        }

                        // Read the amount back from the event: a listener may have called
                        // setAmount() to override the fortune roll without cancelling.
                        // Re-cap it to the space left in the result slot so a listener-set amount
                        // (which the event only validates as >= 1) cannot overstack the result.
                        // Clone the recipe output so its ItemMeta (display name, lore, ...)
                        // survives - a plain new ItemStack(type, amount) would strip it.
                        int finalAmount = Math.min(item.getMaxStackSize() - previous, event.getAmount());

                        if (finalAmount > 0) {
                            ItemStack newResult = item.clone();
                            newResult.setAmount(finalAmount);

                            e.setResult(newResult);
                        }
                    }
                }
            }
        }
    }

}

package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.ChargingBenchChargeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * The {@link ChargingBench} is a powered machine that can be used to charge any {@link Rechargeable} item.
 * 
 * @author TheBusyBiscuit
 * 
 * @see Rechargeable
 *
 */
public class ChargingBench extends AContainer {

    public ChargingBench(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.GOLDEN_PICKAXE);
    }

    @Override
    protected void tick(Block b) {
        if (getCharge(b.getLocation()) < getEnergyConsumption()) {
            return;
        }

        BlockMenu inv = BlockStorage.getInventory(b);

        for (int slot : getInputSlots()) {
            ItemStack item = inv.getItemInSlot(slot);

            if (item == null || item.getAmount() != 1) {
                continue;
            }

            if (charge(b, inv, slot, item)) {
                return;
            }
        }
    }

    private boolean charge(Block b, BlockMenu inv, int slot, ItemStack item) {
        SlimefunItem sfItem = SlimefunItem.getByItem(item);

        if (sfItem instanceof Rechargeable rechargeable) {
            float charge = getEnergyConsumption() / 2F;

            /*
             * Fire a ChargingBenchChargeEvent before charging. Cancellation skips
             * the charging for this tick: the item stays in the input slot and the
             * bench's stored energy is not consumed. Gated on registered listeners
             * to keep the default path allocation-free.
             */
            if (ChargingBenchChargeEvent.getHandlerList().getRegisteredListeners().length > 0) {
                ChargingBenchChargeEvent event = new ChargingBenchChargeEvent(b, item, rechargeable, charge);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return true;
                }

                charge = event.getCharge();
            }

            if (rechargeable.addItemCharge(item, charge)) {
                removeCharge(b.getLocation(), getEnergyConsumption());
            } else if (inv.fits(item, getOutputSlots())) {
                inv.pushItem(item, getOutputSlots());
                inv.replaceExistingItem(slot, null);
            }

            return true;
        } else if (sfItem != null && inv.fits(item, getOutputSlots())) {
            inv.pushItem(item, getOutputSlots());
            inv.replaceExistingItem(slot, null);
        }

        return false;
    }

    @Override
    public String getMachineIdentifier() {
        return "CHARGING_BENCH";
    }

}

package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.events.AutoAnvilRepairEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * The {@link AutoAnvil} is an electric machine which can repair any {@link ItemStack} using
 * Duct tape.
 * 
 * @author TheBusyBiscuit
 *
 */
public class AutoAnvil extends AContainer {

    private final int repairFactor;

    public AutoAnvil(ItemGroup itemGroup, int repairFactor, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        this.repairFactor = repairFactor;
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.IRON_PICKAXE);
    }

    @Override
    public String getMachineIdentifier() {
        return "AUTO_ANVIL";
    }

    @Override
    protected MachineRecipe findNextRecipe(BlockMenu menu) {
        for (int slot : getInputSlots()) {
            ItemStack ductTape = menu.getItemInSlot(slot == getInputSlots()[0] ? getInputSlots()[1] : getInputSlots()[0]);
            ItemStack item = menu.getItemInSlot(slot);

            if (item != null && item.getType().getMaxDurability() > 0 && ((Damageable) item.getItemMeta()).getDamage() > 0) {
                if (SlimefunUtils.isItemSimilar(ductTape, SlimefunItems.DUCT_TAPE.item(), true, false)) {
                    ItemStack repairedItem = repair(item);

                    if (!menu.fits(repairedItem, getOutputSlots())) {
                        return null;
                    }

                    if (AutoAnvilRepairEvent.getHandlerList().getRegisteredListeners().length > 0) {
                        AutoAnvilRepairEvent event = new AutoAnvilRepairEvent(this, menu.getBlock().getLocation(), ductTape, item, repairedItem);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            // An addon vetoed this repair; the inputs stay and no operation is started.
                            return null;
                        }

                        repairedItem = event.getResult();
                    }

                    int otherSlot = slot == getInputSlots()[0] ? getInputSlots()[1] : getInputSlots()[0];
                    ItemStack liveItem = menu.getItemInSlot(slot);
                    ItemStack liveTape = menu.getItemInSlot(otherSlot);

                    // Re-validate: this machine ticks asynchronously while players interact with
                    // its menu, so the item/duct-tape read above could have been taken or swapped
                    // in the meantime. Consuming without re-checking could repair for free (slot
                    // emptied) or consume the wrong item (slot swapped). Mirrors AContainer#scanForRecipe.
                    if (liveItem == null || liveTape == null || !SlimefunUtils.isItemSimilar(liveItem, item, true) || !SlimefunUtils.isItemSimilar(liveTape, ductTape, true)) {
                        return null;
                    }

                    for (int inputSlot : getInputSlots()) {
                        menu.consumeItem(inputSlot);
                    }

                    return new MachineRecipe(30, new ItemStack[] { ductTape, item }, new ItemStack[] { repairedItem });
                }

                break;
            }
        }

        return null;
    }

    private ItemStack repair(ItemStack item) {
        ItemStack repaired = item.clone();
        ItemMeta meta = repaired.getItemMeta();

        short maxDurability = item.getType().getMaxDurability();
        int repairPercentage = 100 / repairFactor;
        short durability = (short) (((Damageable) meta).getDamage() - (maxDurability / repairPercentage));

        if (durability < 0) {
            durability = 0;
        }

        ((Damageable) meta).setDamage(durability);
        repaired.setItemMeta(meta);
        return repaired;
    }

}

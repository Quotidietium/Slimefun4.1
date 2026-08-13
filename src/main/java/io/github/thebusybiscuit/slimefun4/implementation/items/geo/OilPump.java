package io.github.thebusybiscuit.slimefun4.implementation.items.geo;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.OilPumpExtractEvent;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

public class OilPump extends AContainer implements RecipeDisplayItem {

    private final GEOResource oil;

    private final ItemStack emptyBucket = new ItemStack(Material.BUCKET);

    @ParametersAreNonnullByDefault
    public OilPump(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        oil = Slimefun.getRegistry().getGEOResources().get(new NamespacedKey(Slimefun.instance(), "oil")).orElse(null);

        new BlockMenuPreset(getId(), getInventoryTitle()) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                if (!(p.hasPermission("slimefun.inventory.bypass") || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK))) {
                    return false;
                }

                if (oil == null || !Slimefun.getGPSNetwork().getResourceManager().getSupplies(oil, b.getWorld(), b.getX() >> 4, b.getZ() >> 4).isPresent()) {
                    Slimefun.getLocalization().sendMessage(p, "gps.geo.scan-required", true);
                    return false;
                }

                return true;
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }
        };
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        return Arrays.asList(emptyBucket, SlimefunItems.OIL_BUCKET.item());
    }

    @Override
    public String getMachineIdentifier() {
        return "OIL_PUMP";
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.DIAMOND_SHOVEL);
    }

    @Override
    protected MachineRecipe findNextRecipe(BlockMenu inv) {
        if (oil == null) {
            return null;
        }

        if (inv.fits(SlimefunItems.OIL_BUCKET.item(), getOutputSlots())) {
            Block b = inv.getBlock();

            for (int slot : getInputSlots()) {
                if (SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), emptyBucket, true, false)) {
                    OptionalInt supplies = Slimefun.getGPSNetwork().getResourceManager().getSupplies(oil, b.getWorld(), b.getX() >> 4, b.getZ() >> 4);

                    if (supplies.isPresent() && supplies.getAsInt() > 0) {
                        int suppliesCost = 1;

                        if (OilPumpExtractEvent.getHandlerList().getRegisteredListeners().length > 0) {
                            OilPumpExtractEvent event = new OilPumpExtractEvent(this, b.getLocation(), oil, slot, supplies.getAsInt());
                            Bukkit.getPluginManager().callEvent(event);

                            if (event.isCancelled()) {
                                // An addon vetoed this extraction; the bucket and supplies stay, no recipe is started.
                                return null;
                            }

                            // An addon may have adjusted how many supplies this extraction consumes
                            suppliesCost = event.getSuppliesCost();
                        }

                        MachineRecipe recipe = new MachineRecipe(26, new ItemStack[] { emptyBucket }, new ItemStack[] { SlimefunItems.OIL_BUCKET.item() });

                        ItemStack liveBucket = inv.getItemInSlot(slot);

                        // Re-validate the live slot: this machine ticks asynchronously while players
                        // interact with its menu, so the bucket matched above could have been taken
                        // or swapped. Consuming without re-checking could pump oil for free or consume
                        // the wrong item. Mirrors AContainer#scanForRecipe.
                        if (liveBucket == null || !SlimefunUtils.isItemSimilar(liveBucket, emptyBucket, true, false)) {
                            return null;
                        }

                        inv.consumeItem(slot);
                        Slimefun.getGPSNetwork().getResourceManager().setSupplies(oil, b.getWorld(), b.getX() >> 4, b.getZ() >> 4, supplies.getAsInt() - suppliesCost);
                        return recipe;
                    } else {
                        /*
                         * Move the empty bucket to the output slot to prevent this
                         * from immediately starting all over again (to prevent lag).
                         * Only move if it fits; otherwise leave it in place (the pump
                         * will re-scan next tick, which is harmless and avoids voiding
                         * the bucket when the output is full).
                         */
                        ItemStack item = inv.getItemInSlot(slot).clone();

                        if (inv.fits(item, getOutputSlots())) {
                            inv.replaceExistingItem(slot, null);
                            inv.pushItem(item, getOutputSlots());
                        }

                        return null;
                    }
                }
            }
        }

        return null;
    }
}

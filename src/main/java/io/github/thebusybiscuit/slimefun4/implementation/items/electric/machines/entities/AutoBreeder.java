package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.AutoBreedEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

public class AutoBreeder extends SlimefunItem implements InventoryBlock, EnergyNetComponent {

    private final int[] border = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };

    private static final int ENERGY_CONSUMPTION = 60;

    // We wanna strip the Slimefun Item id here
    private static final ItemStack organicFood = ItemStackWrapper.wrap(SlimefunItems.ORGANIC_FOOD.item());

    @ParametersAreNonnullByDefault
    public AutoBreeder(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemHandler(onBreak());
        addItemHandler(SlimefunUtils.ownerTrackingPlaceHandler());
        createPreset(this, this::constructMenu);
    }

    @Nonnull
    private ItemHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(Block b) {
                BlockMenu inv = BlockStorage.getInventory(b);

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                }
            }
        };
    }

    protected void constructMenu(BlockMenuPreset preset) {
        for (int i : border) {
            preset.addItem(i, CustomItemStack.create(new ItemStack(Material.CYAN_STAINED_GLASS_PANE), " "), (p, slot, item, action) -> false);
        }
    }

    @Override
    public int[] getInputSlots() {
        return new int[] { 10, 11, 12, 13, 14, 15, 16 };
    }

    @Override
    public int[] getOutputSlots() {
        return new int[0];
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return 1024;
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, Config data) {
                AutoBreeder.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return true;
            }

        });
    }

    protected void tick(Block b) {
        BlockMenu inv = BlockStorage.getInventory(b);
        OfflinePlayer owner = SlimefunUtils.getOwner(b.getLocation());

        // Only breed animals the machine's owner may interact with, so an Auto Breeder at a claim
        // border cannot trigger breeding in a neighbour's pen.
        for (Entity n : b.getWorld().getNearbyEntities(b.getLocation(), 4.0, 2.0, 4.0, en -> canBreed(en, owner))) {
            for (int slot : getInputSlots()) {
                if (SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), organicFood, false)) {
                    if (getCharge(b.getLocation()) < ENERGY_CONSUMPTION) {
                        return;
                    }

                    int loveTicks = 600;

                    if (AutoBreedEvent.getHandlerList().getRegisteredListeners().length > 0) {
                        AutoBreedEvent event = new AutoBreedEvent(this, b, (Animals) n, inv.getItemInSlot(slot));
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            return;
                        }

                        loveTicks = event.getLoveModeTicks();
                    }

                    removeCharge(b.getLocation(), ENERGY_CONSUMPTION);
                    inv.consumeItem(slot);

                    ((Animals) n).setLoveModeTicks(loveTicks);
                    n.getWorld().spawnParticle(Particle.HEART, ((LivingEntity) n).getEyeLocation(), 8, 0.2F, 0.2F, 0.2F);
                    return;
                }
            }
        }
    }

    private boolean canBreed(@Nonnull Entity n, OfflinePlayer owner) {
        if (n.isValid() && n instanceof Animals animal && animal.isAdult() && animal.canBreed() && !animal.isLoveMode()) {
            return owner == null || Slimefun.getProtectionManager().hasPermission(owner, n.getLocation(), Interaction.INTERACT_ENTITY);
        }

        return false;
    }

}

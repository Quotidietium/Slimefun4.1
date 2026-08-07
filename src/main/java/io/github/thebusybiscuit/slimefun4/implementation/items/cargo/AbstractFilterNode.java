package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeFilterChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

/**
 * This abstract super class represents all filtered Cargo nodes.
 *
 * @author TheBusyBiscuit
 *
 * @see CargoInputNode
 * @see AdvancedCargoOutputNode
 *
 */
abstract class AbstractFilterNode extends AbstractCargoNode {

    protected static final int[] SLOTS = { 19, 20, 21, 28, 29, 30, 37, 38, 39 };
    private static final String FILTER_TYPE = "filter-type";
    private static final String FILTER_LORE = "filter-lore";

    @ParametersAreNonnullByDefault
    protected AbstractFilterNode(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, @Nullable ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        addItemHandler(onBreak());
    }

    @Override
    public boolean hasItemFilter() {
        return true;
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                BlockMenu inv = BlockStorage.getInventory(b);

                if (inv != null) {
                    inv.dropItems(b.getLocation(), SLOTS);
                }
            }
        };
    }

    @Nonnull
    protected abstract int[] getBorder();

    @Override
    protected void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlock();
        BlockStorage.addBlockInfo(b, "index", "0");
        BlockStorage.addBlockInfo(b, FILTER_TYPE, "whitelist");
        BlockStorage.addBlockInfo(b, FILTER_LORE, String.valueOf(true));
        BlockStorage.addBlockInfo(b, "filter-durability", String.valueOf(false));
    }

    @Override
    protected void createBorder(BlockMenuPreset preset) {
        for (int i : getBorder()) {
            preset.addItem(i, CustomItemStack.create(Material.CYAN_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        }

        preset.addItem(2, CustomItemStack.create(Material.PAPER, "&3Items", "", "&bPut in all Items you want to", "&bblacklist/whitelist"), ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    protected void updateBlockMenu(BlockMenu menu, Block b) {
        Location loc = b.getLocation();
        String filterType = BlockStorage.getLocationInfo(loc, FILTER_TYPE);

        if (!BlockStorage.hasBlockInfo(b) || filterType == null || filterType.equals("whitelist")) {
            menu.replaceExistingItem(15, CustomItemStack.create(Material.WHITE_WOOL, "&7Type: &rWhitelist", "", "&e> Click to change it to Blacklist"));
            menu.addMenuClickHandler(15, (p, slot, item, action) -> {
                if (applyFilterChange(p, b, FILTER_TYPE, CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, false)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        } else {
            menu.replaceExistingItem(15, CustomItemStack.create(Material.BLACK_WOOL, "&7Type: &8Blacklist", "", "&e> Click to change it to Whitelist"));
            menu.addMenuClickHandler(15, (p, slot, item, action) -> {
                if (applyFilterChange(p, b, FILTER_TYPE, CargoNodeFilterChangeEvent.Reason.FILTER_TYPE, true)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        }

        String lore = BlockStorage.getLocationInfo(b.getLocation(), FILTER_LORE);

        if (!BlockStorage.hasBlockInfo(b) || lore == null || lore.equals(String.valueOf(true))) {
            menu.replaceExistingItem(25, CustomItemStack.create(Material.MAP, "&7Include Lore: &2\u2714", "", "&e> Click to toggle whether the Lore has to match"));
            menu.addMenuClickHandler(25, (p, slot, item, action) -> {
                if (applyFilterChange(p, b, FILTER_LORE, CargoNodeFilterChangeEvent.Reason.LORE_MATCHING, false)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        } else {
            menu.replaceExistingItem(25, CustomItemStack.create(Material.MAP, "&7Include Lore: &4\u2718", "", "&e> Click to toggle whether the Lore has to match"));
            menu.addMenuClickHandler(25, (p, slot, item, action) -> {
                if (applyFilterChange(p, b, FILTER_LORE, CargoNodeFilterChangeEvent.Reason.LORE_MATCHING, true)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        }

        addChannelSelector(b, menu, 41, 42, 43);
        markDirty(loc);
    }

    /**
     * Applies a filter setting change triggered from the filter GUI: fires a vetoable
     * {@link CargoNodeFilterChangeEvent} first (only if anyone is listening), then stores
     * the new value. A vetoed change leaves the stored setting untouched. Without listeners
     * this is a plain {@link BlockStorage} write, exactly as before.
     *
     * @param p
     *            The {@link Player} who clicked the setting
     * @param b
     *            The {@link Block} of this node
     * @param key
     *            The {@link BlockStorage} key of the setting
     * @param reason
     *            The {@link CargoNodeFilterChangeEvent.Reason} identifying the setting
     * @param newValue
     *            The toggled value (whitelist/lore-include semantics)
     *
     * @return Whether the change was applied and the menu should refresh
     */
    @ParametersAreNonnullByDefault
    protected final boolean applyFilterChange(Player p, Block b, String key, CargoNodeFilterChangeEvent.Reason reason, boolean newValue) {
        boolean previousValue = readFilterBoolean(b, key, reason);

        if (CargoNodeFilterChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            CargoNodeFilterChangeEvent event = new CargoNodeFilterChangeEvent(p, this, b, reason, previousValue, newValue);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed this change; the stored setting and the menu stay as they are.
                return false;
            }

            newValue = event.getNewValue();
        }

        String stored = reason == CargoNodeFilterChangeEvent.Reason.FILTER_TYPE ? (newValue ? "whitelist" : "blacklist") : String.valueOf(newValue);
        BlockStorage.addBlockInfo(b, key, stored);
        return true;
    }

    @ParametersAreNonnullByDefault
    private boolean readFilterBoolean(Block b, String key, CargoNodeFilterChangeEvent.Reason reason) {
        String value = BlockStorage.getLocationInfo(b.getLocation(), key);

        if (reason == CargoNodeFilterChangeEvent.Reason.FILTER_TYPE) {
            // Default and "whitelist" both mean whitelist mode
            return value == null || value.equals("whitelist");
        } else {
            // Default and "true" both mean lore must match
            return value == null || value.equals(String.valueOf(true));
        }
    }

    @Override
    protected void markDirty(@Nonnull Location loc) {
        CargoNet network = CargoNet.getNetworkFromLocation(loc);

        if (network != null) {
            network.markCargoNodeConfigurationDirty(loc);
        }
    }

}

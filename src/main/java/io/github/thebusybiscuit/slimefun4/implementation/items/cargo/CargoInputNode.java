package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeDistributionModeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

public class CargoInputNode extends AbstractFilterNode {

    private static final int[] BORDER = { 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 17, 18, 22, 23, 26, 27, 31, 32, 33, 34, 35, 36, 40, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53 };

    private static final String ROUND_ROBIN_MODE = "round-robin";
    private static final String SMART_FILL_MODE = "smart-fill";

    @ParametersAreNonnullByDefault
    public CargoInputNode(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
    }

    @Override
    protected int[] getBorder() {
        return BORDER;
    }

    @Override
    protected void onPlace(BlockPlaceEvent e) {
        super.onPlace(e);

        BlockStorage.addBlockInfo(e.getBlock(), ROUND_ROBIN_MODE, String.valueOf(false));
        BlockStorage.addBlockInfo(e.getBlock(), SMART_FILL_MODE, String.valueOf(false));
    }

    @Override
    protected void updateBlockMenu(BlockMenu menu, Block b) {
        super.updateBlockMenu(menu, b);

        String roundRobinMode = BlockStorage.getLocationInfo(b.getLocation(), ROUND_ROBIN_MODE);
        if (!BlockStorage.hasBlockInfo(b) || roundRobinMode == null || roundRobinMode.equals(String.valueOf(false))) {
            menu.replaceExistingItem(24, CustomItemStack.create(HeadTexture.ENERGY_REGULATOR.getAsItemStack(), "&7Round-Robin Mode: &4\u2718", "", "&e> Click to enable Round Robin Mode", "&e(Items will be equally distributed on the Channel)"));
            menu.addMenuClickHandler(24, (p, slot, item, action) -> {
                if (applyDistributionChange(p, b, ROUND_ROBIN_MODE, CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, true)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        } else {
            menu.replaceExistingItem(24, CustomItemStack.create(HeadTexture.ENERGY_REGULATOR.getAsItemStack(), "&7Round-Robin Mode: &2\u2714", "", "&e> Click to disable Round Robin Mode", "&e(Items will be equally distributed on the Channel)"));
            menu.addMenuClickHandler(24, (p, slot, item, action) -> {
                if (applyDistributionChange(p, b, ROUND_ROBIN_MODE, CargoNodeDistributionModeEvent.Reason.ROUND_ROBIN, false)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        }

        String smartFillNode = BlockStorage.getLocationInfo(b.getLocation(), SMART_FILL_MODE);
        if (!BlockStorage.hasBlockInfo(b) || smartFillNode == null || smartFillNode.equals(String.valueOf(false))) {
            menu.replaceExistingItem(16, CustomItemStack.create(Material.WRITABLE_BOOK, "&7\"Smart-Filling\" Mode: &4\u2718", "", "&e> Click to enable \"Smart-Filling\" Mode", "", "&fIn this mode, the Cargo node will attempt", "&fto keep a constant amount of items", "&fin the inventory. This is not perfect", "&fand will still fill in empty slots that", "&fcome before a stack of a configured item."));
            menu.addMenuClickHandler(16, (p, slot, item, action) -> {
                if (applyDistributionChange(p, b, SMART_FILL_MODE, CargoNodeDistributionModeEvent.Reason.SMART_FILL, true)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        } else {
            menu.replaceExistingItem(16, CustomItemStack.create(Material.WRITTEN_BOOK, "&7\"Smart-Filling\" Mode: &2\u2714", "", "&e> Click to disable \"Smart-Filling\" Mode", "", "&fIn this mode, the Cargo node will attempt", "&fto keep a constant amount of items", "&fin the inventory. This is not perfect", "&fand will still fill in empty slots that", "&fcome before a stack of a configured item."));
            menu.addMenuClickHandler(16, (p, slot, item, action) -> {
                if (applyDistributionChange(p, b, SMART_FILL_MODE, CargoNodeDistributionModeEvent.Reason.SMART_FILL, false)) {
                    updateBlockMenu(menu, b);
                }

                return false;
            });
        }
    }

    /**
     * Applies a distribution-mode change triggered from the input node GUI: fires a
     * vetoable {@link CargoNodeDistributionModeEvent} first (only if anyone is listening),
     * then stores the new value. A vetoed change leaves the stored mode untouched.
     * Without listeners this is a plain {@link BlockStorage} write, exactly as before.
     *
     * @param p
     *            The {@link Player} who clicked the toggle
     * @param b
     *            The {@link Block} of this node
     * @param key
     *            The {@link BlockStorage} key of the mode
     * @param reason
     *            The {@link CargoNodeDistributionModeEvent.Reason} identifying the mode
     * @param newValue
     *            The toggled value (whether the mode is enabled)
     *
     * @return Whether the change was applied and the menu should refresh
     */
    @ParametersAreNonnullByDefault
    protected final boolean applyDistributionChange(Player p, Block b, String key, CargoNodeDistributionModeEvent.Reason reason, boolean newValue) {
        String stored = BlockStorage.getLocationInfo(b.getLocation(), key);
        boolean previousValue = stored != null && stored.equals(String.valueOf(true));

        if (CargoNodeDistributionModeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            CargoNodeDistributionModeEvent event = new CargoNodeDistributionModeEvent(p, this, b, reason, previousValue, newValue);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed this change; the stored mode and the menu stay as they are.
                return false;
            }

            newValue = event.getNewValue();
        }

        BlockStorage.addBlockInfo(b, key, String.valueOf(newValue));
        return true;
    }

}

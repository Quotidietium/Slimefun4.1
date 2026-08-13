package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.TrashCanVoidEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

/**
 * The {@link TrashCan} is a simple container which simply voids all
 * items that enter it.
 *
 * @author TheBusyBiscuit
 *
 */
public class TrashCan extends SlimefunItem implements InventoryBlock {

    private final int[] border = { 0, 1, 2, 3, 5, 4, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };
    private final ItemStack background = CustomItemStack.create(Material.RED_STAINED_GLASS_PANE, " ");

    @ParametersAreNonnullByDefault
    public TrashCan(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        createPreset(this, this::constructMenu);
    }

    private void constructMenu(BlockMenuPreset preset) {
        for (int i : border) {
            preset.addItem(i, background, ChestMenuUtils.getEmptyClickHandler());
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
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                BlockMenu menu = BlockStorage.getInventory(b);

                if (menu == null) {
                    return;
                }

                if (TrashCanVoidEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    List<ItemStack> items = new ArrayList<>();
                    List<Integer> occupiedSlots = new ArrayList<>();

                    for (int slot : getInputSlots()) {
                        ItemStack stack = menu.getItemInSlot(slot);

                        if (stack != null) {
                            items.add(stack);
                            occupiedSlots.add(slot);
                        }
                    }

                    if (!items.isEmpty()) {
                        TrashCanVoidEvent event = new TrashCanVoidEvent(TrashCan.this, b, items);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            // An addon vetoed the voiding; the items stay in the trash can.
                            return;
                        }

                        List<ItemStack> spared = new ArrayList<>(event.getSparedItems());

                        if (!spared.isEmpty()) {
                            // Void everything that was not spared, one slot at a time.
                            // Compare against the exact ItemStack references the event exposed:
                            // addons pass those into spareItem(...) and a fresh getItemInSlot(...)
                            // may be a copy, breaking identity-based matching.
                            for (int i = 0; i < items.size(); i++) {
                                if (!removeSpared(spared, items.get(i))) {
                                    menu.replaceExistingItem(occupiedSlots.get(i), null);
                                }
                            }

                            return;
                        }
                    }
                }

                for (int slot : getInputSlots()) {
                    menu.replaceExistingItem(slot, null);
                }
            }

            @Override
            public boolean isSynchronized() {
                // Clearing slots must happen on the main Thread: Players can have
                // this menu open and Bukkit inventories are not thread-safe.
                return true;
            }
        });
    }

    /**
     * Removes one matching entry from the spared list, returning whether the given
     * stack was spared.
     * <p>
     * {@link ItemStack#equals(Object)} deliberately ignores item meta (it only compares
     * type, amount and durability), so a plain {@code List#remove(Object)} would consider
     * an enchanted pickaxe "equal" to a plain one and could spare the wrong slot, voiding
     * the exact item an addon asked to keep. Identity is preferred because addons usually
     * pass a reference straight from {@link TrashCanVoidEvent#getItems()}; the meta-aware
     * {@link SlimefunUtils#isItemSimilar(ItemStack, ItemStack, boolean, boolean)} + amount
     * comparison covers defensive clones.
     */
    private static boolean removeSpared(List<ItemStack> spared, ItemStack stack) {
        int similarFallback = -1;

        for (int i = 0; i < spared.size(); i++) {
            ItemStack entry = spared.get(i);

            if (entry == stack) {
                spared.remove(i);
                return true;
            }

            if (similarFallback < 0 && entry.getAmount() == stack.getAmount() && SlimefunUtils.isItemSimilar(entry, stack, true, false)) {
                similarFallback = i;
            }
        }

        if (similarFallback >= 0) {
            spared.remove(similarFallback);
            return true;
        }

        return false;
    }

}

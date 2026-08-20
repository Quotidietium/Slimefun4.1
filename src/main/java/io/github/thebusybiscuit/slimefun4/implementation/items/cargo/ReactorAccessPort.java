package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;
import io.github.thebusybiscuit.slimefun4.implementation.items.misc.CoolantCell;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * The {@link ReactorAccessPort} is a block which acts as an interface
 * between a {@link Reactor} and a {@link CargoNet}.
 * Any item placed into the port will get transferred to the {@link Reactor}.
 *
 * @author TheBusyBiscuit
 * @author AlexLander123
 *
 */
public class ReactorAccessPort extends SlimefunItem {

    private static final int INFO_SLOT = 49;

    private final int[] background = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13, 14, 21, 23 };
    private final int[] fuelBorder = { 9, 10, 11, 18, 20, 27, 29, 36, 38, 45, 46, 47 };
    private final int[] inputBorder = { 15, 16, 17, 24, 26, 33, 35, 42, 44, 51, 52, 53 };
    private final int[] outputBorder = { 30, 31, 32, 39, 41, 48, 50 };

    @ParametersAreNonnullByDefault
    public ReactorAccessPort(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemHandler(onBreak());
        addItemHandler(onPlace());

        new BlockMenuPreset(getId(), "&2反应堆接入端口") {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.inventory.bypass") || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                BlockMenu reactor = getReactor(b.getLocation());

                if (reactor != null) {
                    menu.replaceExistingItem(INFO_SLOT, CustomItemStack.create(Material.GREEN_WOOL, "&7反应堆", "", "&6已检测到", "", "&7> 点击查看反应堆"));
                    menu.addMenuClickHandler(INFO_SLOT, (p, slot, item, action) -> {
                        if (reactor != null) {
                            reactor.open(p);
                        }

                        newInstance(menu, b);

                        return false;
                    });
                } else {
                    menu.replaceExistingItem(INFO_SLOT, CustomItemStack.create(Material.RED_WOOL, "&7反应堆", "", "&c未检测到", "", "&7反应堆必须放置在", "&7接入端口下方 3 格处", "&7（接入端口上方需为反应堆！）"));
                    menu.addMenuClickHandler(INFO_SLOT, (p, slot, item, action) -> {
                        newInstance(menu, b);
                        return false;
                    });
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.INSERT) {
                    if (SlimefunItem.getByItem(item) instanceof CoolantCell) {
                        return getCoolantSlots();
                    } else {
                        return getFuelSlots();
                    }
                } else {
                    return getOutputSlots();
                }
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                BlockMenu inv = BlockStorage.getInventory(b);

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getFuelSlots());
                    inv.dropItems(b.getLocation(), getCoolantSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }
            }
        };
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(@Nonnull BlockPlaceEvent e) {
                // Record the owner so getReactor() can refuse to interface with a stranger's
                // reactor placed below this access port.
                BlockStorage.addBlockInfo(e.getBlock(), "owner", e.getPlayer().getUniqueId().toString());
            }
        };
    }

    private void constructMenu(@Nonnull BlockMenuPreset preset) {
        preset.drawBackground(ChestMenuUtils.getBackground(), background);

        preset.drawBackground(CustomItemStack.create(Material.LIME_STAINED_GLASS_PANE, " "), fuelBorder);
        preset.drawBackground(CustomItemStack.create(Material.CYAN_STAINED_GLASS_PANE, " "), inputBorder);
        preset.drawBackground(CustomItemStack.create(Material.GREEN_STAINED_GLASS_PANE, " "), outputBorder);

        preset.addItem(1, CustomItemStack.create(SlimefunItems.URANIUM.item(), "&7燃料槽", "", "&r此槽位接受放射性燃料，例如：", "&2Uranium &ror &aNeptunium"), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(22, CustomItemStack.create(SlimefunItems.PLUTONIUM.item(), "&7副产物槽", "", "&r此槽位存放反应堆的副产物", "&r例如 &a镎 &r或 &7钚"), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(7, CustomItemStack.create(SlimefunItems.REACTOR_COOLANT_CELL.item(), "&b冷却槽", "", "&r此槽位接受冷却单元", "&4如果没有供给冷却单元，", "&4您的反应堆将会爆炸"), ChestMenuUtils.getEmptyClickHandler());
    }

    @Nonnull
    public int[] getInputSlots() {
        return new int[] { 19, 28, 37, 25, 34, 43 };
    }

    @Nonnull
    public int[] getFuelSlots() {
        return new int[] { 19, 28, 37 };
    }

    @Nonnull
    public int[] getCoolantSlots() {
        return new int[] { 25, 34, 43 };
    }

    @Nonnull
    public static int[] getOutputSlots() {
        return new int[] { 40 };
    }

    @Nullable
    private BlockMenu getReactor(@Nonnull Location l) {
        Location location = new Location(l.getWorld(), l.getX(), l.getY() - 3, l.getZ());
        SlimefunItem item = BlockStorage.check(location.getBlock());

        if (item instanceof Reactor) {
            // Only interface with a reactor owned by the same player as this port, otherwise a
            // player could open this port and peek into / fill a stranger's reactor below.
            if (!SlimefunUtils.isSameOwner(l, location)) {
                return null;
            }

            return BlockStorage.getInventory(location);
        }

        return null;
    }

}

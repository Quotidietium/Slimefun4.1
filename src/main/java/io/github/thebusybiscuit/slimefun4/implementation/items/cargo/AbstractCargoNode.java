package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.CargoNodeChannelChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.ColoredMaterial;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * This abstract class is the super class of all cargo nodes.
 *
 * @author TheBusyBiscuit
 *
 */
abstract class AbstractCargoNode extends SimpleSlimefunItem<BlockPlaceHandler> implements CargoNode {

    protected static final String FREQUENCY = "frequency";

    @ParametersAreNonnullByDefault
    AbstractCargoNode(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, @Nullable ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        new BlockMenuPreset(getId(), ChatUtils.removeColorCodes(item.getItemMeta().getDisplayName())) {

            @Override
            public void init() {
                createBorder(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                menu.addMenuCloseHandler(p -> markDirty(b.getLocation()));
                updateBlockMenu(menu, b);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.cargo.bypass") || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }

    @Override
    public BlockPlaceHandler getItemHandler() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                Block b = e.getBlock();

                // The owner and frequency are required by every node
                BlockStorage.addBlockInfo(b, "owner", e.getPlayer().getUniqueId().toString());
                BlockStorage.addBlockInfo(b, FREQUENCY, "0");

                onPlace(e);
            }

        };
    }

    @ParametersAreNonnullByDefault
    protected void addChannelSelector(Block b, BlockMenu menu, int slotPrev, int slotCurrent, int slotNext) {
        int channel = getSelectedChannel(b);

        menu.replaceExistingItem(slotPrev, CustomItemStack.create(HeadTexture.CARGO_ARROW_LEFT.getAsItemStack(), "&b上一频道", "", "&e> 点击将频道编号减 1"));
        menu.addMenuClickHandler(slotPrev, (p, slot, item, action) -> {
            int newChannel = channel - 1;

            if (newChannel < 0) {
                newChannel = 15;
            }

            if (applyChannelChange(p, b, newChannel)) {
                updateBlockMenu(menu, b);
            }

            return false;
        });

        if (channel == 16) {
            menu.replaceExistingItem(slotCurrent, CustomItemStack.create(HeadTexture.CHEST_TERMINAL.getAsItemStack(), "&b频道编号： &3" + (channel + 1)));
            menu.addMenuClickHandler(slotCurrent, ChestMenuUtils.getEmptyClickHandler());
        } else {
            menu.replaceExistingItem(slotCurrent, CustomItemStack.create(ColoredMaterial.WOOL.get(channel), "&b频道编号： &3" + (channel + 1)));
            menu.addMenuClickHandler(slotCurrent, ChestMenuUtils.getEmptyClickHandler());
        }

        menu.replaceExistingItem(slotNext, CustomItemStack.create(HeadTexture.CARGO_ARROW_RIGHT.getAsItemStack(), "&b下一频道", "", "&e> 点击将频道编号加 1"));
        menu.addMenuClickHandler(slotNext, (p, slot, item, action) -> {
            int newChannel = channel + 1;

            if (newChannel > 15) {
                newChannel = 0;
            }

            if (applyChannelChange(p, b, newChannel)) {
                updateBlockMenu(menu, b);
            }

            return false;
        });
    }

    /**
     * Applies a channel change triggered from the channel selector: fires a vetoable
     * {@link CargoNodeChannelChangeEvent} first (only if anyone is listening), then
     * stores the new frequency. A vetoed change leaves the stored frequency untouched.
     * Without listeners this is a plain {@link BlockStorage} write, exactly as before.
     *
     * @param p
     *            The {@link Player} who clicked the channel selector
     * @param b
     *            The {@link Block} of this node
     * @param newChannel
     *            The newly selected channel, zero-based
     *
     * @return Whether the change was applied and the menu should be refreshed
     */
    protected final boolean applyChannelChange(@Nonnull Player p, @Nonnull Block b, int newChannel) {
        if (CargoNodeChannelChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            CargoNodeChannelChangeEvent event = new CargoNodeChannelChangeEvent(p, this, b, getSelectedChannel(b), newChannel);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed this change; the stored frequency and the menu stay as they are.
                return false;
            }

            // An addon may have redirected the channel change
            newChannel = event.getNewChannel();
        }

        BlockStorage.addBlockInfo(b, FREQUENCY, String.valueOf(newChannel));
        return true;
    }

    @Override
    public int getSelectedChannel(@Nonnull Block b) {
        Validate.notNull(b, "Block must not be null");

        if (!BlockStorage.hasBlockInfo(b)) {
            return 0;
        } else {
            String frequency = BlockStorage.getLocationInfo(b.getLocation(), FREQUENCY);

            if (frequency == null) {
                return 0;
            } else {
                try {
                    int channel = Integer.parseInt(frequency);
                    return NumberUtils.clamp(0, channel, 16);
                } catch (NumberFormatException x) {
                    return 0;
                }
            }
        }
    }

    abstract void onPlace(@Nonnull BlockPlaceEvent e);

    abstract void createBorder(@Nonnull BlockMenuPreset preset);

    abstract void updateBlockMenu(@Nonnull BlockMenu menu, @Nonnull Block b);

    abstract void markDirty(@Nonnull Location loc);

}

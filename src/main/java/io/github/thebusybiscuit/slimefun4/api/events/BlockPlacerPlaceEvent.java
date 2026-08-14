package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.BlockPlacer;

/**
 * This {@link Event} is fired whenever a {@link BlockPlacer} wants to place a {@link Block}.
 * <p>
 * Cancelling this event skips the placement entirely: the dispensed item stays in the
 * {@link BlockPlacer}'s inventory. Addons may also replace the placed item via
 * {@link #setItemStack(ItemStack)}.
 *
 * @author TheBusyBiscuit
 *
 */
public class BlockPlacerPlaceEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block blockPlacer;
    private ItemStack placedItem;

    private boolean cancelled = false;
    private boolean locked = false;

    /**
     * This creates a new {@link BlockPlacerPlaceEvent}.
     * 
     * @param blockPlacer
     *            The {@link BlockPlacer}
     * @param placedItem
     *            The {@link ItemStack} of the {@link Block} that was placed
     * @param block
     *            The placed {@link Block}
     */
    @ParametersAreNonnullByDefault
    public BlockPlacerPlaceEvent(Block blockPlacer, ItemStack placedItem, Block block) {
        super(block);

        // Mirror the setter invariant: the @Nonnull getItemStack() contract must not be
        // bypassable via the constructor.
        Validate.notNull(placedItem, "The ItemStack must not be null!");

        this.placedItem = placedItem;
        this.blockPlacer = blockPlacer;
    }

    /**
     * This method returns the {@link BlockPlacer}
     *
     * @return The {@link BlockPlacer}
     */
    @Nonnull
    public Block getBlockPlacer() {
        return blockPlacer;
    }

    /**
     * This returns the placed {@link ItemStack}.
     * 
     * @return The placed {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return placedItem;
    }

    /**
     * This sets the placed {@link ItemStack}.
     * <p>
     * The replacement is placed as-is: its material is set on the faced {@link Block} and,
     * for nameable blocks, a custom display name is applied. The replacement is re-validated
     * against the same placement rules the original item had to pass (a placeable block
     * material, not blacklisted via the {@code unplaceable-blocks} setting); if it does not
     * qualify, the placement is skipped and nothing is consumed.
     * The originally dispensed item is still the one consumed from the {@link BlockPlacer}'s
     * inventory. When a {@link SlimefunItem} is being placed, the placed {@link Block} keeps
     * the original Slimefun identity (its stored id and {@link io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler}),
     * the replacement only affects the placed material.
     *
     * @param item
     *            The {@link ItemStack} to be placed
     */
    public void setItemStack(@Nonnull ItemStack item) {
        Validate.notNull(item, "The ItemStack must not be null!");

        if (!locked) {
            this.placedItem = item;
        } else {
            SlimefunItem.getByItem(placedItem).warn("A BlockPlacerPlaceEvent cannot be modified from within a BlockPlaceHandler!");
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        if (!locked) {
            cancelled = cancel;
        } else {
            SlimefunItem.getByItem(placedItem).warn("A BlockPlacerPlaceEvent cannot be modified from within a BlockPlaceHandler!");
        }
    }

    /**
     * This marks this {@link Event} as immutable, it can no longer be modified afterwards.
     */
    public void setImmutable() {
        locked = true;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

}

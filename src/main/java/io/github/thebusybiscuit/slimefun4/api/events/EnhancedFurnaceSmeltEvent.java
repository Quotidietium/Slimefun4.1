package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;

/**
 * This {@link Event} is fired whenever an {@link EnhancedFurnace} smelts an item and
 * its fortune roll produced a bonus, before the smelting result is replaced with the
 * bonus output.
 * <p>
 * Cancelling this event skips the fortune bonus: the smelting result stays the vanilla
 * one. Listeners that want a custom result can cancel this event and set it via
 * {@link FurnaceSmeltEvent#setResult(ItemStack)} themselves.
 *
 * @author Zurker
 *
 * @see EnhancedFurnaceBurnEvent
 * @see EnhancedFurnace
 */
public class EnhancedFurnaceSmeltEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EnhancedFurnace furnace;
    private final Block block;
    private final FurnaceSmeltEvent smeltEvent;
    private final int amount;

    private boolean cancelled;

    public EnhancedFurnaceSmeltEvent(@Nonnull EnhancedFurnace furnace, @Nonnull Block block, @Nonnull FurnaceSmeltEvent smeltEvent, int amount) {
        Validate.notNull(furnace, "The EnhancedFurnace must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(smeltEvent, "The FurnaceSmeltEvent must not be null");

        this.furnace = furnace;
        this.block = block;
        this.smeltEvent = smeltEvent;
        this.amount = amount;
    }

    /**
     * This returns the {@link EnhancedFurnace} the item is smelted in.
     *
     * @return The {@link EnhancedFurnace}
     */
    @Nonnull
    public EnhancedFurnace getFurnace() {
        return furnace;
    }

    /**
     * This returns the {@link Block} of the {@link EnhancedFurnace}.
     *
     * @return The furnace {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the underlying {@link FurnaceSmeltEvent}. Its result has not
     * been replaced with the bonus output yet and can be adjusted directly.
     *
     * @return The underlying {@link FurnaceSmeltEvent}
     */
    @Nonnull
    public FurnaceSmeltEvent getSmeltEvent() {
        return smeltEvent;
    }

    /**
     * This returns the amount of items the fortune roll of the {@link EnhancedFurnace}
     * produced for this smelt, capped by the space left in the result slot.
     *
     * @return The bonus output amount
     */
    public int getAmount() {
        return amount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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

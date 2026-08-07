package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.TrashCan;

/**
 * This {@link Event} is fired whenever a {@link TrashCan} is about to void the items in
 * its input slots: at least one input slot holds an item and every one of them is about
 * to be destroyed.
 * <p>
 * Cancelling this event keeps every item in the {@link TrashCan}; nothing is voided in
 * that tick. An addon that only wants to rescue specific items can cancel and pull them
 * out through the block's inventory itself.
 *
 * @author Zurker
 *
 * @see TrashCan
 * @see CargoItemInsertEvent
 */
public class TrashCanVoidEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final TrashCan trashCan;
    private final Block block;
    private final List<ItemStack> items;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public TrashCanVoidEvent(TrashCan trashCan, Block block, List<ItemStack> items) {
        Validate.notNull(trashCan, "The TrashCan must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(items, "The items must not be null");

        this.trashCan = trashCan;
        this.block = block;
        this.items = List.copyOf(items);
    }

    /**
     * This returns the {@link TrashCan} that is about to void items.
     *
     * @return The {@link TrashCan}
     */
    @Nonnull
    public TrashCan getTrashCan() {
        return trashCan;
    }

    /**
     * This returns the {@link Block} of the {@link TrashCan}.
     *
     * @return The trash can {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns an unmodifiable view of the {@link ItemStack ItemStacks} that are about
     * to be voided, one per occupied input slot.
     *
     * @return The items about to be voided
     */
    @Nonnull
    public List<ItemStack> getItems() {
        return items;
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

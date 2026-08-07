package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link Event} is fired whenever a {@link SlimefunItem} is about to be blocked
 * from being used as an ingredient by the vanilla auto-{@link Crafter}.
 * <p>
 * The {@link Crafter} crafts on its own without any {@link org.bukkit.entity.Player}
 * context, so unlike the click-based workstations there is no player to expose here.
 * Cancelling this event vetoes the protection: the {@link CrafterCraftEvent} is left
 * untouched and the craft proceeds with the {@link SlimefunItem} ingredient.
 *
 * @author Zurker
 *
 * @see SlimefunItemWorkstationEvent
 * @see AutoCrafterCraftEvent
 */
public class SlimefunItemCrafterPreventEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final ItemStack itemStack;
    private final Block block;

    private boolean cancelled;

    public SlimefunItemCrafterPreventEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack itemStack, @Nonnull Block block) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(itemStack, "The ItemStack must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.slimefunItem = slimefunItem;
        this.itemStack = itemStack;
        this.block = block;
    }

    /**
     * This returns the {@link SlimefunItem} that is about to be blocked.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the actual {@link ItemStack} the {@link Crafter} tried to craft with.
     *
     * @return The used {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * This returns the {@link Block} of the {@link Crafter} attempting the craft.
     *
     * @return The {@link Crafter} {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
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

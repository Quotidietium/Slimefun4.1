package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link Event} is fired whenever a liquid (water or lava) is about to flow into a
 * fluid-sensitive {@link SlimefunItem} block (e.g. a head-based Slimefun block) and Slimefun
 * is about to cancel the flow: fluids must not wash away or override Slimefun block data.
 * <p>
 * Cancelling this event vetoes the protection: the liquid flows into the block like it
 * would into any vanilla fluid-sensitive block.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.listeners.BlockPhysicsListener
 * @see SlimefunBlockFallEvent
 */
public class SlimefunLiquidFlowEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;

    private boolean cancelled;

    public SlimefunLiquidFlowEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
    }

    /**
     * This returns the {@link SlimefunItem} of the block the liquid is flowing into.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the fluid-sensitive {@link Block} the liquid is flowing into.
     *
     * @return The {@link Block}
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

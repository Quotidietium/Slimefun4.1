package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.InfusedHopper;

/**
 * This {@link Event} is fired whenever an {@link InfusedHopper} is about to collect
 * a dropped {@link Item} by teleporting it onto itself.
 * <p>
 * The event fires once per item in range. Cancelling it skips that item for this
 * tick: it stays where it is and will be re-evaluated on the next tick.
 *
 * @author Zurker
 *
 * @see InfusedHopper
 * @see ItemMagnetPullEvent
 */
public class InfusedHopperCollectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final InfusedHopper hopper;
    private final Block block;
    private final Item item;

    private boolean cancelled;

    public InfusedHopperCollectEvent(@Nonnull InfusedHopper hopper, @Nonnull Block block, @Nonnull Item item) {
        Validate.notNull(hopper, "The InfusedHopper must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(item, "The Item must not be null");

        this.hopper = hopper;
        this.block = block;
        this.item = item;
    }

    /**
     * This returns the {@link InfusedHopper} that is collecting the item.
     *
     * @return The {@link InfusedHopper}
     */
    @Nonnull
    public InfusedHopper getHopper() {
        return hopper;
    }

    /**
     * This returns the {@link Block} of the {@link InfusedHopper}.
     *
     * @return The {@link InfusedHopper} {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the dropped {@link Item} that is about to be teleported onto
     * the {@link InfusedHopper}.
     *
     * @return The dropped {@link Item}
     */
    @Nonnull
    public Item getItem() {
        return item;
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

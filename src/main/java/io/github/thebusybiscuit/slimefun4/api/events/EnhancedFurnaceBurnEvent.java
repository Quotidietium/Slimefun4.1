package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.FurnaceBurnEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;

/**
 * This {@link Event} is fired whenever fuel is burnt in an {@link EnhancedFurnace},
 * before its fuel efficiency multiplier is applied to the burn time.
 * <p>
 * Cancelling this event skips the multiplier: the fuel burns with its vanilla burn
 * time. Listeners that want a custom burn time can cancel this event and set it via
 * {@link FurnaceBurnEvent#setBurnTime(int)} themselves.
 *
 * @author Zurker
 *
 * @see EnhancedFurnaceSmeltEvent
 * @see EnhancedFurnace
 */
public class EnhancedFurnaceBurnEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EnhancedFurnace furnace;
    private final Block block;
    private final FurnaceBurnEvent burnEvent;
    private int fuelEfficiency;

    private boolean cancelled;

    public EnhancedFurnaceBurnEvent(@Nonnull EnhancedFurnace furnace, @Nonnull Block block, @Nonnull FurnaceBurnEvent burnEvent) {
        Validate.notNull(furnace, "The EnhancedFurnace must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(burnEvent, "The FurnaceBurnEvent must not be null");

        this.furnace = furnace;
        this.block = block;
        this.burnEvent = burnEvent;
        this.fuelEfficiency = furnace.getFuelEfficiency();
    }

    /**
     * This returns the {@link EnhancedFurnace} the fuel is burnt in.
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
     * This returns the underlying {@link FurnaceBurnEvent}. Its burn time has not
     * been multiplied yet and can be adjusted directly.
     *
     * @return The underlying {@link FurnaceBurnEvent}
     */
    @Nonnull
    public FurnaceBurnEvent getBurnEvent() {
        return burnEvent;
    }

    /**
     * This returns the fuel efficiency multiplier that is about to be applied to the
     * burn time. Initialized from the {@link EnhancedFurnace}'s efficiency, but can be
     * overridden via {@link #setFuelEfficiency(int)}.
     *
     * @return The fuel efficiency multiplier
     */
    public int getFuelEfficiency() {
        return fuelEfficiency;
    }

    /**
     * This sets the fuel efficiency multiplier that will be applied to the burn time.
     * Use this to boost or penalize the fuel efficiency without cancelling the event.
     * A value of 0 is equivalent to cancelling (vanilla burn time is used as-is since
     * the multiplier is skipped when efficiency is 0).
     *
     * @param fuelEfficiency
     *            The new fuel efficiency multiplier, must be at least 0
     */
    public void setFuelEfficiency(int fuelEfficiency) {
        Validate.isTrue(fuelEfficiency >= 0, "The fuel efficiency must not be negative");
        this.fuelEfficiency = fuelEfficiency;
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

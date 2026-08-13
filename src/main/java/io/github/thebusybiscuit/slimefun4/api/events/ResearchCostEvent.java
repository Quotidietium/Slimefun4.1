package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.api.researches.Research;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} is about to pay the level
 * cost for unlocking a {@link Research} from the Slimefun guide, right before the levels
 * are deducted.
 * <p>
 * Cancelling this event makes the unlock free of charge: no levels are deducted, but the
 * research still unlocks. The cost can also be adjusted via {@link #setCost(int)} to grant
 * a discount or surcharge; the deduction and any failure refund use the adjusted cost.
 * <p>
 * The event is fired synchronously on the main thread. It fires only on the non-free path:
 * when the {@link Player} is in creative mode with free creative research enabled, no cost
 * is deducted and this event does not fire. Note that {@link ResearchUnlockEvent} fires
 * later, after the cost has already been paid and while the research is being unlocked.
 *
 * @author Zurker
 *
 * @see ResearchUnlockEvent
 * @see PlayerPreResearchEvent
 * @see Research
 */
public class ResearchCostEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Research research;
    private int cost;

    private boolean cancelled;

    public ResearchCostEvent(@Nonnull Player player, @Nonnull Research research, int cost) {
        super(player);
        Validate.notNull(research, "The Research must not be null");
        Validate.isTrue(cost >= 0, "The cost must not be negative");

        this.research = research;
        this.cost = cost;
    }

    /**
     * This returns the {@link Research} that is about to be paid for.
     *
     * @return The {@link Research}
     */
    @Nonnull
    public Research getResearch() {
        return research;
    }

    /**
     * This returns the level cost that is about to be deducted.
     *
     * @return The level cost
     */
    public int getCost() {
        return cost;
    }

    /**
     * This sets the level cost that will be deducted. A cost of zero makes the unlock
     * free of charge, equivalent to cancelling this event.
     * <p>
     * Note that the {@link Research#canUnlock(org.bukkit.entity.Player)} gate is
     * evaluated against the research's base cost <em>before</em> this event fires, so
     * lowering the cost cannot let a player in below the base cost. Raising the cost
     * (a surcharge) does further restrict the unlock: it is re-checked against the
     * player's current level at deduction time and the unlock is refused when they
     * cannot afford it.
     *
     * @param cost
     *            The new level cost, must not be negative
     */
    public void setCost(int cost) {
        Validate.isTrue(cost >= 0, "The cost must not be negative");
        this.cost = cost;
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

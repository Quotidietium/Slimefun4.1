package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner.IndustrialMiner;

/**
 * This {@link Event} is fired whenever a running {@link IndustrialMiner} is about to
 * mine an ore block.
 * <p>
 * The event fires before the outcome is pushed to the machine's chest; a full chest
 * or missing fuel still stops the miner afterwards. Cancelling this event vetoes this
 * one ore: it stays in the ground and the miner scans on, exactly as if it could not
 * mine it.
 * <p>
 * Addons may also replace the yield via {@link #setOutcome(ItemStack)}, e.g. for a
 * fortune-style upgrade or a custom ore mapping: the replacement is pushed to the
 * chest as-is and the ore is still consumed (cancel the event to spare the ore).
 * <p>
 * The miner keeps running while its owner is offline, so the owner is exposed as an
 * {@link OfflinePlayer} and this event deliberately has no {@code Player} context.
 *
 * @author Zurker
 *
 * @see IndustrialMiner
 * @see IndustrialMinerStartEvent
 */
public class IndustrialMinerMineEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final IndustrialMiner miner;
    private final OfflinePlayer owner;
    private final Block block;

    private ItemStack outcome;
    private boolean cancelled;

    public IndustrialMinerMineEvent(@Nonnull IndustrialMiner miner, @Nonnull OfflinePlayer owner, @Nonnull Block block, @Nonnull ItemStack outcome) {
        Validate.notNull(miner, "The IndustrialMiner must not be null");
        Validate.notNull(owner, "The owner must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(outcome, "The outcome must not be null");

        this.miner = miner;
        this.owner = owner;
        this.block = block;
        this.outcome = outcome;
    }

    /**
     * This returns the {@link IndustrialMiner} that is about to mine.
     *
     * @return The {@link IndustrialMiner}
     */
    @Nonnull
    public IndustrialMiner getMiner() {
        return miner;
    }

    /**
     * This returns the owner of the {@link IndustrialMiner}; they may be offline
     * while the miner keeps running.
     *
     * @return The owner of the {@link IndustrialMiner}
     */
    @Nonnull
    public OfflinePlayer getOwner() {
        return owner;
    }

    /**
     * This returns the ore {@link Block} that is about to be mined.
     *
     * @return The ore {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link ItemStack} the mined ore will yield (already rolled,
     * respecting the miner's silk touch setting).
     *
     * @return The mining outcome
     */
    @Nonnull
    public ItemStack getOutcome() {
        return outcome;
    }

    /**
     * This replaces the {@link ItemStack} the mined ore will yield. The replacement
     * is pushed to the machine's chest as-is; the ore is still consumed and the fuel
     * is still spent (cancel this event to spare the ore entirely).
     *
     * @param outcome
     *            The new mining outcome, must not be null
     */
    public void setOutcome(@Nonnull ItemStack outcome) {
        Validate.notNull(outcome, "The outcome must not be null");

        this.outcome = outcome;
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

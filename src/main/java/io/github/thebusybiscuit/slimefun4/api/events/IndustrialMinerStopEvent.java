package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner.IndustrialMiner;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner.MinerStoppingReason;

/**
 * This {@link Event} is fired whenever an {@link IndustrialMiner} stops due to an error
 * condition, right after the owning player has been notified.
 * <p>
 * The event is informational only: it is not cancellable, since the miner has already
 * decided to stop and any state it would have changed has settled. Which condition
 * triggered the stop is told by {@link #getReason()} and the number of ores mined so far
 * is available via {@link #getOresMined()}.
 * <p>
 * The event does not fire for a normal completion (the miner finishing its whole range):
 * only error stops go through {@code stop(reason)}. It is fired synchronously from the
 * mining task.
 *
 * @author Zurker
 *
 * @see IndustrialMinerStartEvent
 * @see IndustrialMinerMineEvent
 * @see IndustrialMiner
 * @see MinerStoppingReason
 */
public class IndustrialMinerStopEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final IndustrialMiner miner;
    private final Block chest;
    private final MinerStoppingReason reason;
    private final int oresMined;

    public IndustrialMinerStopEvent(@Nonnull IndustrialMiner miner, @Nonnull Block chest, @Nonnull MinerStoppingReason reason, int oresMined) {
        Validate.notNull(miner, "The IndustrialMiner must not be null");
        Validate.notNull(chest, "The chest Block must not be null");
        Validate.notNull(reason, "The stopping reason must not be null");
        Validate.isTrue(oresMined >= 0, "The mined ore count must not be negative");

        this.miner = miner;
        this.chest = chest;
        this.reason = reason;
        this.oresMined = oresMined;
    }

    /**
     * This returns the {@link IndustrialMiner} that stopped.
     *
     * @return The {@link IndustrialMiner}
     */
    @Nonnull
    public IndustrialMiner getMiner() {
        return miner;
    }

    /**
     * This returns the {@link Block} of the chest the {@link IndustrialMiner} deposits into.
     *
     * @return The chest {@link Block}
     */
    @Nonnull
    public Block getChest() {
        return chest;
    }

    /**
     * This returns why the {@link IndustrialMiner} stopped.
     *
     * @return The {@link MinerStoppingReason}
     */
    @Nonnull
    public MinerStoppingReason getReason() {
        return reason;
    }

    /**
     * This returns how many ores the {@link IndustrialMiner} mined before it stopped.
     *
     * @return The number of ores mined
     */
    public int getOresMined() {
        return oresMined;
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

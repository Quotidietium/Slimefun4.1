package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner.AdvancedIndustrialMiner;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner.IndustrialMiner;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} starts an {@link IndustrialMiner}
 * by interacting with the multiblock, after the already-running check has passed.
 * <p>
 * Cancelling this event vetoes the start: no mining task is created and the miner
 * stays idle. This is deliberately more specific than the generic
 * {@link MultiBlockInteractEvent}: it only fires when a mining operation would
 * actually begin and exposes the {@link IndustrialMiner} machine involved.
 *
 * @author Zurker
 *
 * @see IndustrialMiner
 * @see AdvancedIndustrialMiner
 * @see MultiBlockInteractEvent
 */
public class IndustrialMinerStartEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final IndustrialMiner miner;
    private final Block block;

    private boolean cancelled;

    public IndustrialMinerStartEvent(@Nonnull Player player, @Nonnull IndustrialMiner miner, @Nonnull Block block) {
        super(player);
        Validate.notNull(miner, "The IndustrialMiner must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.miner = miner;
        this.block = block;
    }

    /**
     * This returns the {@link IndustrialMiner} that is about to start mining.
     *
     * @return The {@link IndustrialMiner}
     */
    @Nonnull
    public IndustrialMiner getMiner() {
        return miner;
    }

    /**
     * This returns the {@link Block} at the center of the {@link IndustrialMiner}
     * multiblock (the blast furnace base block).
     *
     * @return The center {@link Block}
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

package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.LumberAxe;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} breaks a log with a
 * {@link LumberAxe} and the attached tree is about to be felled: the additional logs
 * connected to the broken log are about to be broken as well.
 * <p>
 * Cancelling this event skips felling the additional logs; the log the {@link Player}
 * broke directly is still broken by vanilla. Addons may also remove entries from
 * {@link #getAdditionalLogs()} to spare individual logs from being felled.
 *
 * @author Zurker
 *
 * @see LumberAxe
 * @see ExplosiveToolBreakBlocksEvent
 */
public class LumberAxeTreeFellEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final LumberAxe lumberAxe;
    private final Block primaryLog;
    private final List<Block> additionalLogs;

    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public LumberAxeTreeFellEvent(Player player, LumberAxe lumberAxe, Block primaryLog, List<Block> additionalLogs) {
        super(player);
        Validate.notNull(lumberAxe, "The LumberAxe must not be null");
        Validate.notNull(primaryLog, "The primary log must not be null");
        Validate.notNull(additionalLogs, "The additional logs must not be null");

        this.lumberAxe = lumberAxe;
        this.primaryLog = primaryLog;
        this.additionalLogs = additionalLogs;
    }

    /**
     * This returns the {@link LumberAxe} that felled the tree.
     *
     * @return The {@link LumberAxe}
     */
    @Nonnull
    public LumberAxe getLumberAxe() {
        return lumberAxe;
    }

    /**
     * This returns the log {@link Block} the {@link Player} broke directly.
     * It is not included in {@link #getAdditionalLogs()}.
     *
     * @return The primary broken log {@link Block}
     */
    @Nonnull
    public Block getPrimaryLog() {
        return primaryLog;
    }

    /**
     * This returns the live {@link List} of additional log {@link Block Blocks} that will
     * be broken by this fell. Addons may remove entries to spare individual logs.
     *
     * @return The additional logs about to be broken
     */
    @Nonnull
    public List<Block> getAdditionalLogs() {
        return additionalLogs;
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

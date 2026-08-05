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
 * This {@link PlayerEvent} is fired whenever a {@link Player} strips a log with a
 * {@link LumberAxe} and additional attached logs are about to be stripped as well.
 * <p>
 * The clicked log itself is stripped by vanilla mechanics; this event covers only the
 * additional logs found by the {@link LumberAxe}. Cancelling this event leaves those
 * additional logs untouched. Addons may also remove entries from
 * {@link #getAdditionalLogs()} to spare specific logs.
 *
 * @author Zurker
 *
 * @see LumberAxe
 * @see LumberAxeTreeFellEvent
 */
public class LumberAxeStripEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final LumberAxe lumberAxe;
    private final Block primaryLog;
    private final List<Block> additionalLogs;

    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public LumberAxeStripEvent(Player player, LumberAxe lumberAxe, Block primaryLog, List<Block> additionalLogs) {
        super(player);
        Validate.notNull(lumberAxe, "The LumberAxe must not be null");
        Validate.notNull(primaryLog, "The primary log must not be null");
        Validate.notNull(additionalLogs, "The additional logs must not be null");

        this.lumberAxe = lumberAxe;
        this.primaryLog = primaryLog;
        this.additionalLogs = additionalLogs;
    }

    /**
     * This returns the {@link LumberAxe} that is being used.
     *
     * @return The {@link LumberAxe}
     */
    @Nonnull
    public LumberAxe getLumberAxe() {
        return lumberAxe;
    }

    /**
     * This returns the log {@link Block} that was clicked.
     *
     * @return The clicked log {@link Block}
     */
    @Nonnull
    public Block getPrimaryLog() {
        return primaryLog;
    }

    /**
     * This returns the mutable {@link List} of additional log {@link Block Blocks} that are
     * about to be stripped. Removing entries spares those logs.
     *
     * @return The additional logs to strip
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

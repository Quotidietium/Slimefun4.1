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

import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SeismicAxe;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link SeismicAxe}:
 * a shockwave of jumping blocks is about to travel along the {@link Player}'s line of sight,
 * pushing and damaging any {@link org.bukkit.entity.LivingEntity} in its way.
 * <p>
 * Cancelling this event skips the shockwave entirely: no effects are played, no entity is
 * pushed or damaged and the axe is not damaged. Addons may also remove entries from
 * {@link #getBlocks()} to trim the wave's path; the first two entries are always skipped
 * by the {@link SeismicAxe} itself as they are too close to the {@link Player}.
 *
 * @author Zurker
 *
 * @see SeismicAxe
 * @see StomperBootsPushEvent
 */
public class SeismicAxeShockwaveEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SeismicAxe seismicAxe;
    private final List<Block> blocks;

    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public SeismicAxeShockwaveEvent(Player player, SeismicAxe seismicAxe, List<Block> blocks) {
        super(player);
        Validate.notNull(seismicAxe, "The SeismicAxe must not be null");
        Validate.notNull(blocks, "The line-of-sight blocks must not be null");

        this.seismicAxe = seismicAxe;
        this.blocks = blocks;
    }

    /**
     * This returns the {@link SeismicAxe} that is being used.
     *
     * @return The {@link SeismicAxe}
     */
    @Nonnull
    public SeismicAxe getSeismicAxe() {
        return seismicAxe;
    }

    /**
     * This returns the mutable {@link List} of line-of-sight {@link Block Blocks} the
     * shockwave is about to travel along. Removing entries trims the wave's path.
     *
     * @return The line-of-sight {@link Block Blocks}
     */
    @Nonnull
    public List<Block> getBlocks() {
        return blocks;
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

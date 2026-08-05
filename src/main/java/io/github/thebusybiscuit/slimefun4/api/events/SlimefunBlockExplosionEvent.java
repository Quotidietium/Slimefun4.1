package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.WitherProof;

/**
 * This {@link Event} is fired whenever an explosion is about to destroy a placed
 * {@link SlimefunItem} block. The {@link Cause} tells apart the two explosion sources
 * Slimefun reacts to.
 * <p>
 * Blocks that are {@link WitherProof} or whose {@code BlockBreakHandler} disallows
 * explosions already survive on their own and do not fire this event. Cancelling this
 * event protects the block anyway: it is left intact, its {@code BlockStorage} data is
 * kept and no explosion drops are produced.
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.implementation.listeners.ExplosionsListener
 */
public class SlimefunBlockExplosionEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The kind of explosion that is about to destroy the block.
     */
    public enum Cause {

        /**
         * An entity explosion, e.g. a creeper, TNT or a ghast fireball.
         */
        ENTITY_EXPLOSION,

        /**
         * A block explosion, e.g. a bed or a respawn anchor detonating.
         */
        BLOCK_EXPLOSION
    }

    private final SlimefunItem slimefunItem;
    private final Block block;
    private final Cause cause;

    private boolean cancelled;

    public SlimefunBlockExplosionEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block, @Nonnull Cause cause) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(cause, "The Cause must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
        this.cause = cause;
    }

    /**
     * This returns the {@link SlimefunItem} of the block that is about to be destroyed.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link Block} that is about to be destroyed by the explosion.
     *
     * @return The {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link Cause} of the explosion.
     *
     * @return The {@link Cause}
     */
    @Nonnull
    public Cause getCause() {
        return cause;
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

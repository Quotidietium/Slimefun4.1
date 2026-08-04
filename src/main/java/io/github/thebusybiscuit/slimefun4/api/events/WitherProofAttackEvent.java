package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Wither;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.WitherProof;

/**
 * This {@link Event} is fired whenever a {@link Wither} tries to destroy a placed
 * {@link SlimefunItem} block marked as {@link WitherProof} and the protection is about
 * to be applied: the block change is about to be cancelled and
 * {@link WitherProof#onAttack(Block, Wither)} is about to be called.
 * <p>
 * Cancelling this event skips the protection entirely: the {@link Wither} destroys the
 * block like any other block and {@link WitherProof#onAttack(Block, Wither)} is not
 * called.
 *
 * @author Zurker
 *
 * @see WitherProof
 */
public class WitherProofAttackEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;
    private final Wither wither;
    private final EntityChangeBlockEvent changeBlockEvent;

    private boolean cancelled;

    public WitherProofAttackEvent(@Nonnull SlimefunItem slimefunItem, @Nonnull Block block, @Nonnull Wither wither, @Nonnull EntityChangeBlockEvent changeBlockEvent) {
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(wither, "The Wither must not be null");
        Validate.notNull(changeBlockEvent, "The change block event must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
        this.wither = wither;
        this.changeBlockEvent = changeBlockEvent;
    }

    /**
     * This returns the {@link WitherProof} {@link SlimefunItem} whose protection is
     * about to be applied.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link Block} the {@link Wither} tried to destroy.
     *
     * @return The {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the attacking {@link Wither}.
     *
     * @return The {@link Wither}
     */
    @Nonnull
    public Wither getWither() {
        return wither;
    }

    /**
     * This returns the original {@link EntityChangeBlockEvent} for this block change.
     *
     * @return The {@link EntityChangeBlockEvent}
     */
    @Nonnull
    public EntityChangeBlockEvent getChangeBlockEvent() {
        return changeBlockEvent;
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

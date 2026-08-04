package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.AbstractEntityAssembler;

/**
 * This {@link Event} is fired asynchronously whenever an {@link AbstractEntityAssembler}
 * (e.g. the iron golem or wither assembler) has gathered all required resources and is
 * about to assemble its entity: the materials and energy are about to be consumed and
 * the entity about to be spawned.
 * <p>
 * Cancelling this event skips this assembly entirely: the materials and energy are
 * kept and no entity is spawned. The assembler retries on its next operation cycle.
 * <p>
 * Note that this event fires from the machine's ticker thread, not the main server
 * thread. Listeners should only read state and cancel; any world interaction should be
 * deferred to a synchronous task.
 *
 * @author Zurker
 *
 * @see AbstractEntityAssembler
 */
public class AsyncEntityAssembleEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AbstractEntityAssembler<?> assembler;
    private final Block block;

    private boolean cancelled;

    public AsyncEntityAssembleEvent(@Nonnull AbstractEntityAssembler<?> assembler, @Nonnull Block block) {
        super(true);
        Validate.notNull(assembler, "The AbstractEntityAssembler must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.assembler = assembler;
        this.block = block;
    }

    /**
     * This returns the {@link AbstractEntityAssembler} that is about to assemble.
     *
     * @return The {@link AbstractEntityAssembler}
     */
    @Nonnull
    public AbstractEntityAssembler<?> getAssembler() {
        return assembler;
    }

    /**
     * This returns the {@link Block} of the {@link AbstractEntityAssembler}.
     *
     * @return The assembler {@link Block}
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

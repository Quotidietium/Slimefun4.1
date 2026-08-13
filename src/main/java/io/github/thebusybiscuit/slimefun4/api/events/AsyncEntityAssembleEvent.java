package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
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
 * Addons may also redirect where the entity is spawned via
 * {@link #setSpawnLocation(Location)}, e.g. to assemble a wither inside a containment
 * arena instead of above the assembler. The location defaults to {@code null}, which
 * means the assembler's configured offset above the block is used; setting it back to
 * {@code null} restores that default.
 * <p>
 * Note that this event fires from the machine's ticker thread, not the main server
 * thread. Listeners should only read state, cancel or set the spawn location; any
 * other world interaction should be deferred to a synchronous task. The spawn
 * location is handed to the synchronous spawn task after the tick.
 *
 * @author Zurker
 *
 * @see AbstractEntityAssembler
 */
public class AsyncEntityAssembleEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AbstractEntityAssembler<?> assembler;
    private final Block block;

    @Nullable
    private Location spawnLocation;
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

    /**
     * This returns the {@link Location} the entity will be spawned at, or {@code null}
     * when the assembler's default is used: the configured offset above the assembler
     * {@link Block}.
     *
     * @return The spawn {@link Location}, or null for the default
     * @see #setSpawnLocation(Location)
     */
    @Nullable
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * This redirects where the entity is spawned. Passing {@code null} resets the
     * spawn location to the assembler's default (the configured offset above the
     * assembler {@link Block}).
     * <p>
     * The location must have a non-null {@link org.bukkit.World} and finite
     * coordinates; an invalid location would otherwise corrupt the synchronous
     * spawn task (or spawn the entity at a broken position).
     *
     * @param spawnLocation
     *            The spawn {@link Location}, or null for the default
     */
    public void setSpawnLocation(@Nullable Location spawnLocation) {
        if (spawnLocation != null) {
            Validate.notNull(spawnLocation.getWorld(), "The spawn location must have a world");
            Validate.isTrue(Double.isFinite(spawnLocation.getX()) && Double.isFinite(spawnLocation.getY()) && Double.isFinite(spawnLocation.getZ()), "The spawn location must have finite coordinates, received: " + spawnLocation);
        }

        this.spawnLocation = spawnLocation;
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

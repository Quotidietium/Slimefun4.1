package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;

/**
 * This {@link Event} is fired whenever a {@link PlayerBackpack} is about to be resized,
 * after the new size has been validated and before the inventory is actually swapped.
 * <p>
 * Cancelling this event vetoes the resize: the backpack keeps its current size and
 * contents, as if {@link PlayerBackpack#setSize(int)} had never been called.
 * <p>
 * The event is only fired for sizes that already passed validation: an out-of-range size
 * (not one of 9/18/27/36/45/54) or a shrink that would destroy items still throws as
 * usual, without firing this event. The event is not a {@link org.bukkit.event.player.PlayerEvent}
 * because {@link PlayerBackpack#setSize(int)} is a programmatic API with no player context.
 *
 * @author Zurker
 *
 * @see PlayerBackpackOpenEvent
 * @see PlayerBackpackCloseEvent
 * @see BackpackRestrictionEvent
 * @see PlayerBackpack
 */
public class BackpackResizeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerBackpack backpack;
    private final int oldSize;
    private final int newSize;

    private boolean cancelled;

    public BackpackResizeEvent(@Nonnull PlayerBackpack backpack, int oldSize, int newSize) {
        /* May be fired from asynchronous API calls (e.g. async profile access). */
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(backpack, "The PlayerBackpack must not be null");
        Validate.isTrue(oldSize > 0, "The old size must be positive");
        Validate.isTrue(newSize > 0, "The new size must be positive");

        this.backpack = backpack;
        this.oldSize = oldSize;
        this.newSize = newSize;
    }

    /**
     * This returns the {@link PlayerBackpack} that is about to be resized.
     *
     * @return The {@link PlayerBackpack}
     */
    @Nonnull
    public PlayerBackpack getBackpack() {
        return backpack;
    }

    /**
     * This returns the backpack size before this resize.
     *
     * @return The old size
     */
    public int getOldSize() {
        return oldSize;
    }

    /**
     * This returns the backpack size that will be applied.
     *
     * @return The new size
     */
    public int getNewSize() {
        return newSize;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.ReactorMode;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} clicks the focus selector
 * of a {@link Reactor} and the reactor is about to switch its {@link ReactorMode}.
 * <p>
 * Cancelling this event vetoes the change: the stored mode stays as it is and the menu
 * is not refreshed, as if the click had never happened.
 * <p>
 * The two modes govern how the {@link Reactor} behaves when the energy buffer is full:
 * {@link ReactorMode#GENERATOR} stops consuming fuel, while
 * {@link ReactorMode#PRODUCTION} keeps running to produce byproducts. The event is fired
 * before anything is written, synchronously, since menu clicks happen on the main thread.
 *
 * @author Zurker
 *
 * @see CargoNodeChannelChangeEvent
 * @see AndroidScriptChangeEvent
 * @see Reactor
 * @see ReactorMode
 */
public class ReactorModeChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Reactor reactor;
    private final Block block;
    private final ReactorMode previousMode;
    private final ReactorMode newMode;

    private boolean cancelled;

    public ReactorModeChangeEvent(@Nonnull Player player, @Nonnull Reactor reactor, @Nonnull Block block, @Nonnull ReactorMode previousMode, @Nonnull ReactorMode newMode) {
        super(player);
        Validate.notNull(reactor, "The Reactor must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(previousMode, "The previous mode must not be null");
        Validate.notNull(newMode, "The new mode must not be null");

        this.reactor = reactor;
        this.block = block;
        this.previousMode = previousMode;
        this.newMode = newMode;
    }

    /**
     * This returns the {@link Reactor} whose mode is being changed.
     *
     * @return The {@link Reactor}
     */
    @Nonnull
    public Reactor getReactor() {
        return reactor;
    }

    /**
     * This returns the {@link Block} of the {@link Reactor}.
     *
     * @return The reactor {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the mode that was selected before this change.
     *
     * @return The previously selected {@link ReactorMode}
     */
    @Nonnull
    public ReactorMode getPreviousMode() {
        return previousMode;
    }

    /**
     * This returns the mode that will be stored after this change.
     *
     * @return The newly selected {@link ReactorMode}
     */
    @Nonnull
    public ReactorMode getNewMode() {
        return newMode;
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

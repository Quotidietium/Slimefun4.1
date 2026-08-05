package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GrapplingHook;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link GrapplingHook}
 * (into the air) and the hook is about to fire: the lead is about to be consumed and an arrow +
 * bat setup is about to be spawned to pull the player.
 * <p>
 * Cancelling this event skips the fire entirely: no lead is consumed and no arrow/bat is spawned.
 *
 * @author Zurker
 *
 * @see GrapplingHook
 * @see GrapplingHookPullEvent
 */
public class GrapplingHookFireEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final GrapplingHook grapplingHook;

    private boolean cancelled;

    public GrapplingHookFireEvent(@Nonnull Player player, @Nonnull GrapplingHook grapplingHook) {
        super(player);
        Validate.notNull(grapplingHook, "The GrapplingHook must not be null");

        this.grapplingHook = grapplingHook;
    }

    /**
     * This returns the {@link GrapplingHook} that is firing.
     *
     * @return The {@link GrapplingHook}
     */
    @Nonnull
    public GrapplingHook getGrapplingHook() {
        return grapplingHook;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.Parachute;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} wearing a {@link Parachute}
 * is sneaking and falling and the parachute is about to slow their fall: a gentle upward
 * velocity is about to be applied and the fall distance reset.
 * <p>
 * Cancelling this event skips the slow-fall for this tick: no velocity is applied and the
 * player falls normally, but the parachute keeps running.
 *
 * @author Zurker
 *
 * @see Parachute
 */
public class ParachuteSlowFallEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Parachute parachute;

    private boolean cancelled;

    public ParachuteSlowFallEvent(@Nonnull Player player, @Nonnull Parachute parachute) {
        super(player);
        Validate.notNull(parachute, "The Parachute must not be null");

        this.parachute = parachute;
    }

    /**
     * This returns the {@link Parachute} that is slowing the fall.
     *
     * @return The {@link Parachute}
     */
    @Nonnull
    public Parachute getParachute() {
        return parachute;
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

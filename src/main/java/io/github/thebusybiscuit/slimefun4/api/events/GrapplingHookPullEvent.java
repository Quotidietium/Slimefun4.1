package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GrapplingHook;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player}'s {@link GrapplingHook}
 * arrow has landed and the {@link Player} is about to be pulled towards it.
 * <p>
 * Cancelling this event skips the pull: the {@link Player} is neither teleported
 * nor given velocity. The hook itself is still dropped and cleaned up as usual.
 *
 * @author Zurker
 *
 * @see GrapplingHook
 */
public class GrapplingHookPullEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Arrow arrow;
    private final Location target;

    private boolean cancelled;

    public GrapplingHookPullEvent(@Nonnull Player player, @Nonnull Arrow arrow, @Nonnull Location target) {
        super(player);

        Validate.notNull(arrow, "The arrow must not be null");
        Validate.notNull(target, "The target must not be null");

        this.arrow = arrow;
        this.target = target;
    }

    /**
     * This returns the hook {@link Arrow} that landed.
     *
     * @return The hook {@link Arrow}
     */
    @Nonnull
    public Arrow getArrow() {
        return arrow;
    }

    /**
     * This returns the {@link Location} the {@link Player} would have been
     * pulled towards.
     *
     * @return The pull target {@link Location}
     */
    @Nonnull
    public Location getTarget() {
        return target;
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

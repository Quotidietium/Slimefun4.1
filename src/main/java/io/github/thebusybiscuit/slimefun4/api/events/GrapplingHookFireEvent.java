package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GrapplingHook;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link GrapplingHook}
 * (into the air) and the hook is about to fire: the lead is about to be consumed and an arrow +
 * bat setup is about to be spawned to pull the player.
 * <p>
 * Cancelling this event skips the fire entirely: no lead is consumed and no arrow/bat is spawned.
 * <p>
 * Addons may also adjust the launch via {@link #setDirection(Vector)}: the arrow is spawned
 * offset along this vector and fired with it as its velocity, so a modified direction changes
 * both where the hook appears and where it flies to.
 *
 * @author Zurker
 *
 * @see GrapplingHook
 * @see GrapplingHookPullEvent
 */
public class GrapplingHookFireEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final GrapplingHook grapplingHook;

    private Vector direction;
    private boolean cancelled;

    /**
     * This creates a new {@link GrapplingHookFireEvent} with the default launch direction:
     * the {@link Player}'s eye direction, scaled by the {@link GrapplingHook}'s launch speed.
     *
     * @param player
     *            The {@link Player} firing the hook
     * @param grapplingHook
     *            The {@link GrapplingHook} being fired
     */
    public GrapplingHookFireEvent(@Nonnull Player player, @Nonnull GrapplingHook grapplingHook) {
        this(player, grapplingHook, player.getEyeLocation().getDirection().multiply(2.0));
    }

    /**
     * This creates a new {@link GrapplingHookFireEvent} with the given launch direction.
     *
     * @param player
     *            The {@link Player} firing the hook
     * @param grapplingHook
     *            The {@link GrapplingHook} being fired
     * @param direction
     *            The launch direction, also used as the arrow's spawn offset and velocity
     */
    public GrapplingHookFireEvent(@Nonnull Player player, @Nonnull GrapplingHook grapplingHook, @Nonnull Vector direction) {
        super(player);
        Validate.notNull(grapplingHook, "The GrapplingHook must not be null");
        Validate.notNull(direction, "The launch direction must not be null");

        this.grapplingHook = grapplingHook;
        this.direction = direction;
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

    /**
     * This returns the launch direction of the hook. The arrow is spawned offset
     * along this vector and fired with it as its velocity.
     *
     * @return The launch direction
     */
    @Nonnull
    public Vector getDirection() {
        return direction;
    }

    /**
     * This sets the launch direction of the hook, changing both where the arrow
     * appears and where it flies to.
     *
     * @param direction
     *            The new launch direction, must not be null
     */
    public void setDirection(@Nonnull Vector direction) {
        Validate.notNull(direction, "The launch direction must not be null");

        this.direction = direction;
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

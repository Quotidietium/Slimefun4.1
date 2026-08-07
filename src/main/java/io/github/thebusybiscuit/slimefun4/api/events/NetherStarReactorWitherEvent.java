package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.NetherStarReactor;

/**
 * This {@link Event} is fired whenever a running {@link NetherStarReactor} is about to
 * apply its withering effect to a nearby {@link LivingEntity}: the entity is inside the
 * reactor's range and the reactor's owner was verified to have attack permission against
 * it. The event fires once per affected entity.
 * <p>
 * Cancelling this event skips the withering of that entity only; other entities in range
 * are affected as usual.
 *
 * @author Zurker
 *
 * @see NetherStarReactor
 * @see ReactorCoolantConsumeEvent
 * @see ReactorExplodeEvent
 */
public class NetherStarReactorWitherEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final NetherStarReactor reactor;
    private final Location location;
    private final LivingEntity entity;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public NetherStarReactorWitherEvent(NetherStarReactor reactor, Location location, LivingEntity entity) {
        Validate.notNull(reactor, "The NetherStarReactor must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(entity, "The entity must not be null");

        this.reactor = reactor;
        this.location = location;
        this.entity = entity;
    }

    /**
     * This returns the {@link NetherStarReactor} that is emitting the withering effect.
     *
     * @return The {@link NetherStarReactor}
     */
    @Nonnull
    public NetherStarReactor getReactor() {
        return reactor;
    }

    /**
     * This returns the {@link Location} of the {@link NetherStarReactor}.
     *
     * @return The {@link Location} of the reactor
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the {@link LivingEntity} that is about to be withered.
     *
     * @return The {@link LivingEntity} in range
     */
    @Nonnull
    public LivingEntity getEntity() {
        return entity;
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

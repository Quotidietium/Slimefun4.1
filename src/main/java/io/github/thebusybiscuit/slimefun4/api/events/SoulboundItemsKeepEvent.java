package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} dies while carrying
 * {@link Soulbound} items, right before those items are stored away (and removed
 * from the death drops) for their return on respawn.
 * <p>
 * Cancelling this event disables the soulbound behavior for this death entirely:
 * every item, including the {@link Soulbound} ones, drops normally and nothing
 * is returned on respawn.
 *
 * @author Zurker
 *
 * @see SoulboundItemsReturnEvent
 * @see Soulbound
 */
public class SoulboundItemsKeepEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerDeathEvent deathEvent;

    private boolean cancelled;

    public SoulboundItemsKeepEvent(@Nonnull Player player, @Nonnull PlayerDeathEvent deathEvent) {
        super(player);

        Validate.notNull(deathEvent, "The death event must not be null");
        this.deathEvent = deathEvent;
    }

    /**
     * This returns the underlying {@link PlayerDeathEvent}, giving access to
     * the drops this event's cancellation will leave untouched.
     *
     * @return The underlying {@link PlayerDeathEvent}
     */
    @Nonnull
    public PlayerDeathEvent getDeathEvent() {
        return deathEvent;
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

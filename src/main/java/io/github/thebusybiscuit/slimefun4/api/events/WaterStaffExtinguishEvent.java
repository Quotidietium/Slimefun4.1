package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves.WaterStaff;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link WaterStaff}
 * and the staff is about to extinguish the player (reset their fire ticks).
 * <p>
 * Cancelling this event skips the extinguish: the player keeps burning.
 *
 * @author Zurker
 *
 * @see WaterStaff
 */
public class WaterStaffExtinguishEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final WaterStaff waterStaff;

    private boolean cancelled;

    public WaterStaffExtinguishEvent(@Nonnull Player player, @Nonnull WaterStaff waterStaff) {
        super(player);
        Validate.notNull(waterStaff, "The WaterStaff must not be null");

        this.waterStaff = waterStaff;
    }

    /**
     * This returns the {@link WaterStaff} that is extinguishing the player.
     *
     * @return The {@link WaterStaff}
     */
    @Nonnull
    public WaterStaff getWaterStaff() {
        return waterStaff;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.EnderBackpack;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks with
 * an {@link EnderBackpack} and is about to open their ender chest through it.
 * <p>
 * Cancelling this event vetoes the opening: the ender chest is not opened and
 * no sound is played. The right-click interaction itself is still consumed, so
 * it does not fall through to the clicked block.
 * <p>
 * This complements {@link PlayerBackpackOpenEvent}, which fires when a regular
 * Slimefun backpack opens its own inventory.
 *
 * @author Zurker
 *
 * @see PlayerBackpackOpenEvent
 * @see EnderBackpack
 */
public class EnderBackpackOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EnderBackpack backpack;

    private boolean cancelled;

    public EnderBackpackOpenEvent(@Nonnull Player player, @Nonnull EnderBackpack backpack) {
        super(player);

        Validate.notNull(backpack, "The EnderBackpack must not be null");
        this.backpack = backpack;
    }

    /**
     * This returns the {@link EnderBackpack} that was used.
     *
     * @return The {@link EnderBackpack}
     */
    @Nonnull
    public EnderBackpack getBackpack() {
        return backpack;
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

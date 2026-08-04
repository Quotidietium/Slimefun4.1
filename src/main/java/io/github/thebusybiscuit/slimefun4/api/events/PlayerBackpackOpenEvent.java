package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} is about to open
 * a {@link PlayerBackpack}.
 * <p>
 * Cancelling this event prevents the backpack inventory from opening.
 * This event is fired on the main thread.
 *
 * @author Zurker
 *
 * @see PlayerBackpackCloseEvent
 * @see PlayerBackpack
 */
public class PlayerBackpackOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack item;
    private final PlayerBackpack backpack;

    private boolean cancelled;

    public PlayerBackpackOpenEvent(@Nonnull Player player, @Nonnull ItemStack item, @Nonnull PlayerBackpack backpack) {
        super(player);

        this.item = item;
        this.backpack = backpack;
    }

    /**
     * This returns the backpack {@link ItemStack} the {@link Player} used.
     *
     * @return The backpack {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the {@link PlayerBackpack} that is about to be opened.
     *
     * @return The {@link PlayerBackpack}
     */
    @Nonnull
    public PlayerBackpack getBackpack() {
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

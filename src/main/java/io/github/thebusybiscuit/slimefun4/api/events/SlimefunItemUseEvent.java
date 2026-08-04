package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event.Result;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks with a
 * {@link SlimefunItem} in hand, after the permission checks but before its
 * {@link ItemUseHandler} is called.
 * <p>
 * Cancelling this event denies the item use: the {@link ItemUseHandler} is skipped
 * and the underlying interaction is denied, mirroring the behavior of a failed
 * permission check ({@link Result#DENY}).
 *
 * @author Zurker
 *
 * @see PlayerRightClickEvent
 * @see ItemUseHandler
 */
public class SlimefunItemUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final ItemStack item;
    private final PlayerRightClickEvent rightClickEvent;

    private boolean cancelled;

    public SlimefunItemUseEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack item, @Nonnull PlayerRightClickEvent rightClickEvent) {
        super(player);

        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(rightClickEvent, "The right click event must not be null");

        this.slimefunItem = slimefunItem;
        this.item = item;
        this.rightClickEvent = rightClickEvent;
    }

    /**
     * This returns the {@link SlimefunItem} that is about to be used.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the held {@link ItemStack} that is about to be used.
     *
     * @return The held {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the underlying {@link PlayerRightClickEvent}, giving access to
     * the clicked block, the hand and the original interact event.
     *
     * @return The underlying {@link PlayerRightClickEvent}
     */
    @Nonnull
    public PlayerRightClickEvent getRightClickEvent() {
        return rightClickEvent;
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

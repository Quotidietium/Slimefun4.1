package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} closes the inventory
 * view of a {@link PlayerBackpack} and the backpack's content has been marked dirty
 * for saving.
 * <p>
 * This event is not cancellable - the inventory is already closed. The corresponding
 * {@link PlayerBackpack} can be resolved asynchronously via
 * {@link PlayerProfile#getBackpack(ItemStack, java.util.function.Consumer)}.
 * This event is fired on the main thread.
 *
 * @author Zurker
 *
 * @see PlayerBackpackOpenEvent
 * @see PlayerBackpack
 */
public class PlayerBackpackCloseEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack item;

    public PlayerBackpackCloseEvent(@Nonnull Player player, @Nonnull ItemStack item) {
        super(player);

        this.item = item;
    }

    /**
     * This returns the backpack {@link ItemStack} whose view was closed.
     *
     * @return The backpack {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
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

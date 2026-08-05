package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.InfusedMagnet;

/**
 * This {@link PlayerEvent} is fired for every nearby {@link Item} an {@link InfusedMagnet}
 * is about to pull toward the {@link Player} while they are sneaking: the {@link Item} is
 * about to be teleported to the player's location and a pickup sound is about to play.
 * <p>
 * Cancelling this event skips this {@link Item}: it is left where it is and not teleported.
 * The magnet keeps pulling the remaining items within its radius during this tick.
 *
 * @author Zurker
 *
 * @see InfusedMagnet
 */
public class ItemMagnetPullEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final InfusedMagnet magnet;
    private final Item item;

    private boolean cancelled;

    public ItemMagnetPullEvent(@Nonnull Player player, @Nonnull InfusedMagnet magnet, @Nonnull Item item) {
        super(player);
        Validate.notNull(magnet, "The InfusedMagnet must not be null");
        Validate.notNull(item, "The Item must not be null");

        this.magnet = magnet;
        this.item = item;
    }

    /**
     * This returns the {@link InfusedMagnet} that is pulling the item.
     *
     * @return The {@link InfusedMagnet}
     */
    @Nonnull
    public InfusedMagnet getMagnet() {
        return magnet;
    }

    /**
     * This returns the dropped {@link Item} that is about to be pulled.
     *
     * @return The {@link Item}
     */
    @Nonnull
    public Item getItem() {
        return item;
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

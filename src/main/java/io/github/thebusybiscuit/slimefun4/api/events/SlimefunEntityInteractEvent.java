package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityInteractHandler;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks an {@link Entity}
 * with a {@link SlimefunItem} in hand, after the permission checks but before its
 * {@link EntityInteractHandler} is called.
 * <p>
 * Cancelling this event skips the {@link EntityInteractHandler} and cancels the underlying
 * {@link PlayerInteractEntityEvent}, mirroring the behavior of a failed permission check:
 * neither the item's effect nor any vanilla interaction applies.
 *
 * @author Zurker
 *
 * @see EntityInteractHandler
 */
public class SlimefunEntityInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final ItemStack item;
    private final PlayerInteractEntityEvent interactEvent;

    private boolean cancelled;

    public SlimefunEntityInteractEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack item, @Nonnull PlayerInteractEntityEvent interactEvent) {
        super(player);

        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(interactEvent, "The interact event must not be null");

        this.slimefunItem = slimefunItem;
        this.item = item;
        this.interactEvent = interactEvent;
    }

    /**
     * This returns the {@link SlimefunItem} that is about to be used on the {@link Entity}.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the held {@link ItemStack} that is about to be used on the {@link Entity}.
     *
     * @return The held {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the underlying {@link PlayerInteractEntityEvent}.
     *
     * @return The underlying {@link PlayerInteractEntityEvent}
     */
    @Nonnull
    public PlayerInteractEntityEvent getInteractEvent() {
        return interactEvent;
    }

    /**
     * This is a convenience method that returns the {@link Entity} that was clicked,
     * equivalent to {@link PlayerInteractEntityEvent#getRightClicked()}.
     *
     * @return The clicked {@link Entity}
     */
    @Nonnull
    public Entity getRightClicked() {
        return interactEvent.getRightClicked();
    }

    /**
     * This returns whether the interaction happened with the off hand, mirroring the
     * value the {@link EntityInteractHandler} receives.
     *
     * @return Whether the off hand was used
     */
    public boolean isOffHand() {
        return interactEvent.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.SlimefunFood;

/**
 * This {@link Event} is fired whenever a {@link Player} is about to consume a {@link SlimefunItem}
 * (food, potion, etc.).
 *
 * <p>
 * It is raised inside Slimefun's consume listener, right before the item's
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} is invoked,
 * which makes it the ideal cross-cutting hook for dietary effects:
 * </p>
 * <ul>
 * <li>{@link #setCancelled(boolean)} cancels the consumption entirely - the item is not eaten and
 * the {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} is
 * <b>not</b> called.</li>
 * <li>{@link #getPlayerItemConsumeEvent()} exposes the underlying {@link PlayerItemConsumeEvent},
 * so add-ons can call {@code e.setItem(...)} to alter the leftover item, etc.</li>
 * </ul>
 *
 * <p>
 * Cancellation is delegated to the underlying {@link PlayerItemConsumeEvent}, i.e. cancelling this
 * event also cancels the Bukkit one.
 * </p>
 *
 * @author Zurker
 *
 * @see SlimefunFood
 * @see io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler
 *
 */
public class SlimefunItemConsumeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final SlimefunItem slimefunItem;
    private final ItemStack item;
    private final PlayerItemConsumeEvent underlyingEvent;

    public SlimefunItemConsumeEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack item, @Nonnull PlayerItemConsumeEvent underlyingEvent) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(player, "The Player cannot be null");
        Validate.notNull(slimefunItem, "The SlimefunItem cannot be null");
        Validate.notNull(item, "The consumed ItemStack cannot be null");
        Validate.notNull(underlyingEvent, "The underlying PlayerItemConsumeEvent cannot be null");

        this.player = player;
        this.slimefunItem = slimefunItem;
        this.item = item;
        this.underlyingEvent = underlyingEvent;
    }

    /**
     * The {@link Player} consuming the item.
     *
     * @return The consuming {@link Player}
     */
    @Nonnull
    public Player getPlayer() {
        return player;
    }

    /**
     * The {@link SlimefunItem} being consumed.
     *
     * @return The consumed {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * Whether the consumed {@link SlimefunItem} is tagged as a {@link SlimefunFood}.
     *
     * @return {@code true} if the item implements {@link SlimefunFood}
     */
    public boolean isFood() {
        return slimefunItem instanceof SlimefunFood;
    }

    /**
     * The actual {@link ItemStack} being consumed.
     *
     * @return The consumed {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * The underlying Bukkit {@link PlayerItemConsumeEvent}.
     *
     * @return The wrapped {@link PlayerItemConsumeEvent}
     */
    @Nonnull
    public PlayerItemConsumeEvent getPlayerItemConsumeEvent() {
        return underlyingEvent;
    }

    @Override
    public boolean isCancelled() {
        return underlyingEvent.isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
        underlyingEvent.setCancelled(cancel);
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

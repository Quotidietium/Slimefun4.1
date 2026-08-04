package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} tries to heal an
 * {@link IronGolem} with a {@link SlimefunItem} that merely looks like an iron ingot
 * and Slimefun is about to block the heal: the interaction is about to be cancelled
 * and the player is about to be told that this is not possible.
 * <p>
 * Cancelling this event allows the heal: the interaction is left alone, no message is
 * sent and the golem can be healed with the {@link SlimefunItem} like with a vanilla
 * iron ingot.
 *
 * @author Zurker
 *
 * @see SlimefunItem
 */
public class IronGolemHealEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final IronGolem ironGolem;
    private final PlayerInteractEntityEvent interactEvent;

    private boolean cancelled;

    public IronGolemHealEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull IronGolem ironGolem, @Nonnull PlayerInteractEntityEvent interactEvent) {
        super(player);
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(ironGolem, "The IronGolem must not be null");
        Validate.notNull(interactEvent, "The interact event must not be null");

        this.slimefunItem = slimefunItem;
        this.ironGolem = ironGolem;
        this.interactEvent = interactEvent;
    }

    /**
     * This returns the {@link SlimefunItem} the player tried to heal with.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link IronGolem} that would have been healed.
     *
     * @return The {@link IronGolem}
     */
    @Nonnull
    public IronGolem getIronGolem() {
        return ironGolem;
    }

    /**
     * This returns the original {@link PlayerInteractEntityEvent} for this interaction.
     *
     * @return The {@link PlayerInteractEntityEvent}
     */
    @Nonnull
    public PlayerInteractEntityEvent getInteractEvent() {
        return interactEvent;
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

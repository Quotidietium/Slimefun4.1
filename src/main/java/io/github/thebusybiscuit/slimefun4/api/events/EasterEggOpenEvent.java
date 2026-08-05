package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.seasonal.EasterEgg;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks an {@link EasterEgg}:
 * the egg is about to be consumed, fireworks launched and the rolled gift spawned.
 * <p>
 * Cancelling this event skips the opening entirely: no egg is consumed, no fireworks are
 * launched and no gift is spawned. Addons may also replace the rolled gift via
 * {@link #setGift(ItemStack)}.
 *
 * @author Zurker
 *
 * @see EasterEgg
 */
public class EasterEggOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EasterEgg easterEgg;

    private ItemStack gift;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public EasterEggOpenEvent(Player player, EasterEgg easterEgg, ItemStack gift) {
        super(player);
        Validate.notNull(easterEgg, "The EasterEgg must not be null");
        Validate.notNull(gift, "The gift ItemStack must not be null");

        this.easterEgg = easterEgg;
        this.gift = gift;
    }

    /**
     * This returns the {@link EasterEgg} that is being opened.
     *
     * @return The {@link EasterEgg}
     */
    @Nonnull
    public EasterEgg getEasterEgg() {
        return easterEgg;
    }

    /**
     * This returns the rolled gift {@link ItemStack} that is about to be spawned.
     *
     * @return The rolled gift
     */
    @Nonnull
    public ItemStack getGift() {
        return gift;
    }

    /**
     * This sets the gift {@link ItemStack} that will be spawned.
     *
     * @param gift
     *            The gift to spawn
     */
    public void setGift(@Nonnull ItemStack gift) {
        Validate.notNull(gift, "The gift ItemStack must not be null");

        this.gift = gift;
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

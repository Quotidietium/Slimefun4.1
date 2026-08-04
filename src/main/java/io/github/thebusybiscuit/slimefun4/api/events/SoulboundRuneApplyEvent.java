package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.runes.SoulboundRune;

/**
 * This {@link PlayerEvent} is fired whenever a {@link SoulboundRune} has found a
 * compatible dropped {@link Item} and is about to start its ritual: the {@link ItemStack}
 * will become {@link Soulbound} after a short delay.
 * <p>
 * Cancelling this event aborts the ritual: both the rune and the target {@link Item}
 * remain on the ground, untouched.
 *
 * @author Zurker
 *
 * @see SoulboundRune
 * @see Soulbound
 */
public class SoulboundRuneApplyEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Item rune;
    private final Item item;
    private final ItemStack itemStack;

    private boolean cancelled;

    public SoulboundRuneApplyEvent(@Nonnull Player player, @Nonnull Item rune, @Nonnull Item item, @Nonnull ItemStack itemStack) {
        super(player);

        Validate.notNull(rune, "The rune must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(itemStack, "The ItemStack must not be null");

        this.rune = rune;
        this.item = item;
        this.itemStack = itemStack;
    }

    /**
     * This returns the dropped {@link SoulboundRune} {@link Item} entity.
     *
     * @return The rune {@link Item} entity
     */
    @Nonnull
    public Item getRune() {
        return rune;
    }

    /**
     * This returns the dropped {@link Item} entity that is about to become
     * {@link Soulbound}.
     *
     * @return The target {@link Item} entity
     */
    @Nonnull
    public Item getItem() {
        return item;
    }

    /**
     * This returns the {@link ItemStack} that is about to become {@link Soulbound}.
     *
     * @return The target {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return itemStack;
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

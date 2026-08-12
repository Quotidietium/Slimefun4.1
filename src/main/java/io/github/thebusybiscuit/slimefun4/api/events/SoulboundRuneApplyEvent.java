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
 * <p>
 * Addons may also redirect the ritual to a different dropped {@link Item} via
 * {@link #setTarget(Item)}, e.g. to prefer a more valuable candidate over the first
 * one the rune found. The replacement target is not re-checked against the rune's
 * compatibility rules; if it no longer qualifies when the ritual completes (picked
 * up, stacked or otherwise invalid), the ritual simply fails like it would for the
 * original target.
 *
 * @author Zurker
 *
 * @see SoulboundRune
 * @see Soulbound
 */
public class SoulboundRuneApplyEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Item rune;

    private Item item;
    private ItemStack itemStack;
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
     * @see #setTarget(Item)
     */
    @Nonnull
    public Item getItem() {
        return item;
    }

    /**
     * This redirects the ritual to a different dropped {@link Item}: that item's
     * {@link ItemStack} will become {@link Soulbound} instead of the one the
     * {@link SoulboundRune} found. {@link #getItemStack()} follows the new target.
     *
     * @param item
     *            The new target {@link Item} entity, must not be null and must
     *            carry an {@link ItemStack}
     */
    public void setTarget(@Nonnull Item item) {
        Validate.notNull(item, "The target item must not be null");
        Validate.notNull(item.getItemStack(), "The target's ItemStack must not be null");

        this.item = item;
        this.itemStack = item.getItemStack();
    }

    /**
     * This returns the {@link ItemStack} that is about to become {@link Soulbound},
     * which is the stack of the current target {@link Item}.
     *
     * @return The target {@link ItemStack}
     * @see #setTarget(Item)
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

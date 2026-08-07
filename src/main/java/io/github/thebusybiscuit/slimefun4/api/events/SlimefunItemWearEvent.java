package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.DamageableItem;

/**
 * This {@link PlayerEvent} is fired whenever a {@link DamageableItem} is about to take one
 * point of durability wear, after the Unbreaking enchantment has been evaluated and before
 * the damage is applied.
 * <p>
 * Cancelling this event vetoes the wear: the {@link ItemStack} keeps its current durability
 * and is not consumed. This is the only veto point before a Slimefun item breaks from wear.
 * <p>
 * When {@link #willBreak()} is true, the item is at its last point of durability and would
 * break right after this event without a veto. The event does not fire when the item is not
 * damageable, when it has already run out of durability checks, or when the Unbreaking
 * enchantment saved it. It is fired synchronously, since item wear happens on the main thread.
 *
 * @author Zurker
 *
 * @see DamageableItem
 * @see SlimefunItemDamageEvent
 */
public class SlimefunItemWearEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final ItemStack item;
    private final boolean willBreak;

    private boolean cancelled;

    public SlimefunItemWearEvent(@Nonnull Player player, @Nullable SlimefunItem slimefunItem, @Nonnull ItemStack item, boolean willBreak) {
        super(player);
        Validate.notNull(item, "The ItemStack must not be null");

        this.slimefunItem = slimefunItem;
        this.item = item;
        this.willBreak = willBreak;
    }

    /**
     * This returns the {@link SlimefunItem} that is wearing down, if it could be resolved.
     *
     * @return The {@link SlimefunItem}, or null if the stack is not a registered Slimefun item
     */
    @Nullable
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the live {@link ItemStack} that is about to take wear - treat it as
     * read-only context.
     *
     * @return The {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns whether the item is at its last point of durability: without a veto the
     * item breaks right after this event.
     *
     * @return Whether the item is about to break
     */
    public boolean willBreak() {
        return willBreak;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;

/**
 * This {@link PlayerEvent} is fired whenever stored {@link Soulbound} items are
 * returned to a {@link Player} - on respawn, or when they disconnect after dying
 * without respawning.
 * <p>
 * This event is not cancellable - the items have already been returned.
 *
 * @author Zurker
 *
 * @see SoulboundItemsKeepEvent
 * @see Soulbound
 */
public class SoulboundItemsReturnEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Map<Integer, ItemStack> items;

    public SoulboundItemsReturnEvent(@Nonnull Player player, @Nonnull Map<Integer, ItemStack> items) {
        super(player);

        Validate.notNull(items, "The items must not be null");
        this.items = Collections.unmodifiableMap(new HashMap<>(items));
    }

    /**
     * This returns the items that were returned, keyed by their inventory slot.
     * The pseudo-slot {@link Integer#MIN_VALUE} denotes the item the {@link Player}
     * held on their cursor when they died.
     *
     * @return An unmodifiable {@link Map} of the returned items by slot
     */
    @Nonnull
    public Map<Integer, ItemStack> getItems() {
        return items;
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

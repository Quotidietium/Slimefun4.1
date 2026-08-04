package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} in creative mode uses the
 * pick-block control (middle click) on a placed {@link SlimefunItem} block: the item is
 * about to be handed to the player - either by switching to the hotbar slot that already
 * holds it or by setting it as the cursor item.
 * <p>
 * Cancelling this event skips both: no slot is switched, no item is given and the vanilla
 * pick-block behavior applies to the underlying block instead.
 *
 * @author Zurker
 *
 * @see SlimefunItem
 */
public class SlimefunItemPickBlockEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block block;
    private final InventoryCreativeEvent inventoryEvent;

    private boolean cancelled;

    public SlimefunItemPickBlockEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull Block block, @Nonnull InventoryCreativeEvent inventoryEvent) {
        super(player);
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(inventoryEvent, "The inventory event must not be null");

        this.slimefunItem = slimefunItem;
        this.block = block;
        this.inventoryEvent = inventoryEvent;
    }

    /**
     * This returns the {@link SlimefunItem} that is about to be picked.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the placed {@link Block} the player is picking from.
     *
     * @return The {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the original {@link InventoryCreativeEvent} for this pick.
     *
     * @return The {@link InventoryCreativeEvent}
     */
    @Nonnull
    public InventoryCreativeEvent getInventoryEvent() {
        return inventoryEvent;
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

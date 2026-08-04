package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Item;
import org.bukkit.entity.Piglin;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.PiglinBarterDrop;

/**
 * This {@link EntityEvent} is fired whenever a {@link Piglin}'s barter drop passed the
 * chance check of a {@link PiglinBarterDrop} item and is about to be replaced with that
 * {@link SlimefunItem}'s recipe output.
 * <p>
 * Cancelling this event skips the replacement: the {@link Piglin} keeps its vanilla drop
 * and no further {@link PiglinBarterDrop} items are tried for this barter.
 *
 * @author Zurker
 *
 * @see PiglinBarterDrop
 */
public class PiglinBarterDropEvent extends EntityEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Item itemDrop;
    private final SlimefunItem slimefunItem;
    private final int chance;

    private boolean cancelled;

    public PiglinBarterDropEvent(@Nonnull Piglin piglin, @Nonnull Item itemDrop, @Nonnull SlimefunItem slimefunItem, int chance) {
        super(piglin);

        Validate.notNull(itemDrop, "The Item drop must not be null");
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");

        this.itemDrop = itemDrop;
        this.slimefunItem = slimefunItem;
        this.chance = chance;
    }

    /**
     * This returns the {@link Piglin} that is bartering.
     *
     * @return The {@link Piglin}
     */
    @Nonnull
    public Piglin getPiglin() {
        return (Piglin) getEntity();
    }

    /**
     * This returns the {@link Item} drop whose {@link ItemStack} is about to be replaced.
     * This is the very same {@link Item} the underlying {@link EntityDropItemEvent}
     * carries.
     *
     * @return The {@link Item} drop
     */
    @Nonnull
    public Item getItemDrop() {
        return itemDrop;
    }

    /**
     * This returns the {@link SlimefunItem} whose recipe output the drop is about to
     * be replaced with. The dropped {@link ItemStack} is
     * {@link SlimefunItem#getRecipeOutput()}.
     *
     * @return The {@link SlimefunItem} that won the barter roll
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the bartering loot chance of the winning {@link SlimefunItem},
     * as returned by {@link PiglinBarterDrop#getBarteringLootChance()}.
     *
     * @return The loot chance (1-99)
     */
    public int getChance() {
        return chance;
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

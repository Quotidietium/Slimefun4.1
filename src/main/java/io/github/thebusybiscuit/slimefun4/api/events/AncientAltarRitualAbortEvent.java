package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;

/**
 * This {@link PlayerEvent} is fired whenever a running {@link AncientAltar} ritual is
 * aborted, e.g. because an item was removed from an {@link AncientPedestal} before the
 * ritual could consume it.
 * <p>
 * This event is not cancellable: the ritual has already been aborted and every consumed
 * item has been returned to the world.
 *
 * @author Zurker
 *
 * @see AncientAltar
 * @see AncientAltarRitualStartEvent
 */
public class AncientAltarRitualAbortEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Block altar;
    private final List<Block> pedestals;
    private final List<ItemStack> returnedItems;

    public AncientAltarRitualAbortEvent(@Nonnull Player player, @Nonnull Block altar, @Nonnull List<Block> pedestals, @Nonnull List<ItemStack> returnedItems) {
        super(player);

        Validate.notNull(altar, "The altar must not be null");
        Validate.notNull(pedestals, "The pedestals must not be null");
        Validate.notNull(returnedItems, "The returned items must not be null");

        this.altar = altar;
        this.pedestals = Collections.unmodifiableList(new ArrayList<>(pedestals));
        this.returnedItems = Collections.unmodifiableList(new ArrayList<>(returnedItems));
    }

    /**
     * This returns the {@link AncientAltar} {@link Block} whose ritual was aborted.
     *
     * @return The altar {@link Block}
     */
    @Nonnull
    public Block getAltar() {
        return altar;
    }

    /**
     * This returns the {@link AncientPedestal} {@link Block Blocks} of the aborted ritual.
     *
     * @return An unmodifiable {@link List} of the pedestal {@link Block Blocks}
     */
    @Nonnull
    public List<Block> getPedestals() {
        return pedestals;
    }

    /**
     * This returns the items that had already been consumed by the ritual and were
     * dropped back onto the altar, starting with the catalyst.
     *
     * @return An unmodifiable {@link List} of the returned {@link ItemStack ItemStacks}
     */
    @Nonnull
    public List<ItemStack> getReturnedItems() {
        return returnedItems;
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

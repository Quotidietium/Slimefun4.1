package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link PlayerEvent} is fired whenever an {@code AncientAltarTask} is about to
 * consume a single ingredient sitting on an {@code AncientPedestal} during an ongoing
 * ritual, right before the item entity is removed from the world.
 * <p>
 * The event fires once per pedestal, in ritual order, as the animation visits each
 * pedestal (every 4 stages). Cancelling it spares that one ingredient: the item stays on
 * its pedestal and is not consumed, but the ritual continues with the remaining pedestals
 * and still produces its output at the end. This is the only per-ingredient veto point -
 * {@link AncientAltarRitualStartEvent} fires once for the whole batch (before any consume),
 * and {@link AncientAltarCraftEvent} only fires at the very end for the output.
 * <p>
 * The event is fired synchronously from the ritual task.
 *
 * @author Zurker
 *
 * @see AncientAltarRitualStartEvent
 * @see AncientAltarCraftEvent
 * @see AncientAltarRitualAbortEvent
 */
public class AncientAltarItemConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block altar;
    private final Block pedestal;
    private final ItemStack item;

    private boolean cancelled;

    public AncientAltarItemConsumeEvent(@Nonnull Player player, @Nonnull Block altar, @Nonnull Block pedestal, @Nonnull ItemStack item) {
        super(player);
        Validate.notNull(altar, "The altar Block must not be null");
        Validate.notNull(pedestal, "The pedestal Block must not be null");
        Validate.notNull(item, "The ItemStack must not be null");

        this.altar = altar;
        this.pedestal = pedestal;
        this.item = item;
    }

    /**
     * This returns the altar {@link Block} the ritual is running on.
     *
     * @return The altar {@link Block}
     */
    @Nonnull
    public Block getAltar() {
        return altar;
    }

    /**
     * This returns the pedestal {@link Block} whose ingredient is being consumed.
     *
     * @return The pedestal {@link Block}
     */
    @Nonnull
    public Block getPedestal() {
        return pedestal;
    }

    /**
     * This returns the ingredient that is about to be consumed.
     *
     * @return The consumed {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.Cooler;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Cooler} has fed its owner with a juice
 * (after the {@link CoolerFeedPlayerEvent}) and is about to restore the {@link Player}'s
 * saturation.
 * <p>
 * The saturation amount is modifiable via {@link #setSaturation(float)} - an addon may scale
 * it per juice or per player. Cancelling this event skips the saturation restore only: the
 * juice's potion effects, the consume sound and the consumption itself remain unaffected.
 *
 * @author Zurker
 *
 * @see Cooler
 * @see CoolerFeedPlayerEvent
 */
public class CoolerSaturationEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Cooler cooler;
    private final ItemStack coolerItem;
    private final ItemStack consumedItem;
    private float saturation;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public CoolerSaturationEvent(Player player, Cooler cooler, ItemStack coolerItem, ItemStack consumedItem, float saturation) {
        super(player);
        Validate.notNull(cooler, "The Cooler must not be null");
        Validate.notNull(coolerItem, "The Cooler item must not be null");
        Validate.notNull(consumedItem, "The consumed item must not be null");
        Validate.isTrue(saturation >= 0, "The saturation must not be negative");

        this.cooler = cooler;
        this.coolerItem = coolerItem;
        this.consumedItem = consumedItem;
        this.saturation = saturation;
    }

    /**
     * This returns the {@link Cooler} that fed the {@link Player}.
     *
     * @return The {@link Cooler}
     */
    @Nonnull
    public Cooler getCooler() {
        return cooler;
    }

    /**
     * This returns the {@link Cooler} {@link ItemStack} found in the {@link Player}'s inventory.
     *
     * @return The {@link Cooler} {@link ItemStack}
     */
    @Nonnull
    public ItemStack getCoolerItem() {
        return coolerItem;
    }

    /**
     * This returns the juice {@link ItemStack} that is being consumed.
     *
     * @return The consumed juice {@link ItemStack}
     */
    @Nonnull
    public ItemStack getConsumedItem() {
        return consumedItem;
    }

    /**
     * This returns the saturation that is about to be restored.
     *
     * @return The saturation amount
     */
    public float getSaturation() {
        return saturation;
    }

    /**
     * This sets the saturation that will be restored.
     *
     * @param saturation
     *            The new saturation amount, must not be negative
     */
    public void setSaturation(float saturation) {
        Validate.isTrue(saturation >= 0, "The saturation must not be negative");
        this.saturation = saturation;
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

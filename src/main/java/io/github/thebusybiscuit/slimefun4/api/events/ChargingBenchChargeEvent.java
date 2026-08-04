package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ChargingBench;

/**
 * This {@link Event} is fired whenever a {@link ChargingBench} is about to charge
 * a {@link Rechargeable} {@link ItemStack}.
 * <p>
 * Cancelling this event skips the charging for this tick: the item is not charged
 * and the bench's stored energy is not consumed. The amount of charge added can
 * be adjusted via {@link #setCharge(float)}.
 *
 * @author Zurker
 *
 * @see ChargingBench
 * @see Rechargeable
 */
public class ChargingBenchChargeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block bench;
    private final ItemStack item;
    private final Rechargeable rechargeable;
    private float charge;

    private boolean cancelled;

    public ChargingBenchChargeEvent(@Nonnull Block bench, @Nonnull ItemStack item, @Nonnull Rechargeable rechargeable, float charge) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(bench, "The bench must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(rechargeable, "The rechargeable must not be null");

        this.bench = bench;
        this.item = item;
        this.rechargeable = rechargeable;
        this.charge = charge;
    }

    /**
     * This returns the {@link Block} of the {@link ChargingBench}.
     *
     * @return The {@link ChargingBench} {@link Block}
     */
    @Nonnull
    public Block getBench() {
        return bench;
    }

    /**
     * This returns the {@link ItemStack} that is about to be charged.
     *
     * @return The {@link ItemStack} being charged
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the {@link Rechargeable} handling the charge.
     *
     * @return The {@link Rechargeable} of the item
     */
    @Nonnull
    public Rechargeable getRechargeable() {
        return rechargeable;
    }

    /**
     * This returns the amount of charge that will be added to the item.
     *
     * @return The charge to add
     */
    public float getCharge() {
        return charge;
    }

    /**
     * This sets the amount of charge that will be added to the item.
     *
     * @param charge
     *            The new charge, must be above zero
     */
    public void setCharge(float charge) {
        Validate.isTrue(charge > 0, "Charge must be above zero!");
        this.charge = charge;
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

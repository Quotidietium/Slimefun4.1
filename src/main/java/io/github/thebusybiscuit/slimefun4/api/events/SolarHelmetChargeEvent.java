package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.SolarHelmet;

/**
 * This {@link PlayerEvent} is fired whenever a {@link SolarHelmet} recharges one of the
 * {@link Player}'s {@link Rechargeable} items (armor or held items), right before the charge is
 * added.
 * <p>
 * The event is fired once per charged item. Addons may adjust the amount of charge added via
 * {@link #setCharge(float)} or cancel the charging for a specific item. This complements
 * {@link ChargingBenchChargeEvent}, which only covers items charged inside a Charging Bench.
 *
 * @author Zurker
 *
 * @see SolarHelmet
 * @see Rechargeable
 * @see ChargingBenchChargeEvent
 */
public class SolarHelmetChargeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SolarHelmet helmet;
    private final ItemStack item;
    private final Rechargeable rechargeable;
    private float charge;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public SolarHelmetChargeEvent(Player player, SolarHelmet helmet, ItemStack item, Rechargeable rechargeable, float charge) {
        super(player, !Bukkit.isPrimaryThread());
        Validate.notNull(helmet, "The SolarHelmet must not be null");
        Validate.notNull(item, "The ItemStack must not be null");
        Validate.notNull(rechargeable, "The Rechargeable must not be null");
        Validate.isTrue(charge > 0, "Charge must be above zero!");

        this.helmet = helmet;
        this.item = item;
        this.rechargeable = rechargeable;
        this.charge = charge;
    }

    /**
     * This returns the {@link SolarHelmet} that is charging the item.
     *
     * @return The {@link SolarHelmet}
     */
    @Nonnull
    public SolarHelmet getHelmet() {
        return helmet;
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

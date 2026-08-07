package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;

/**
 * This {@link Event} is fired whenever a {@link ProgrammableAndroid} has run out of fuel and
 * is about to consume a fuel item from its inventory to refill: the fuel item matched a
 * registered {@link MachineFuel} type and is about to be consumed.
 * <p>
 * The amount of fuel ticks granted is modifiable via {@link #setFuelTicks(int)} - an addon
 * may scale the fuel economy per android or per fuel item. Cancelling this event skips the
 * consumption entirely: the fuel item stays in the android's inventory and the android
 * remains out of fuel.
 *
 * @author Zurker
 *
 * @see ProgrammableAndroid
 * @see MachineFuel
 */
public class AndroidFuelConsumeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ProgrammableAndroid android;
    private final Block block;
    private final ItemStack fuelItem;
    private final MachineFuel machineFuel;
    private int fuelTicks;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public AndroidFuelConsumeEvent(ProgrammableAndroid android, Block block, ItemStack fuelItem, MachineFuel machineFuel, int fuelTicks) {
        Validate.notNull(android, "The ProgrammableAndroid must not be null");
        Validate.notNull(block, "The android Block must not be null");
        Validate.notNull(fuelItem, "The fuel item must not be null");
        Validate.notNull(machineFuel, "The MachineFuel must not be null");
        Validate.isTrue(fuelTicks > 0, "The fuel ticks must be positive");

        this.android = android;
        this.block = block;
        this.fuelItem = fuelItem;
        this.machineFuel = machineFuel;
        this.fuelTicks = fuelTicks;
    }

    /**
     * This returns the {@link ProgrammableAndroid} that is consuming fuel.
     *
     * @return The {@link ProgrammableAndroid}
     */
    @Nonnull
    public ProgrammableAndroid getAndroid() {
        return android;
    }

    /**
     * This returns the {@link Block} of the {@link ProgrammableAndroid}.
     *
     * @return The android {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the fuel {@link ItemStack} that is about to be consumed.
     *
     * @return The fuel {@link ItemStack}
     */
    @Nonnull
    public ItemStack getFuelItem() {
        return fuelItem;
    }

    /**
     * This returns the {@link MachineFuel} type the fuel item matched.
     *
     * @return The matched {@link MachineFuel}
     */
    @Nonnull
    public MachineFuel getMachineFuel() {
        return machineFuel;
    }

    /**
     * This returns how many fuel ticks the consumption will grant.
     *
     * @return The fuel ticks granted
     */
    public int getFuelTicks() {
        return fuelTicks;
    }

    /**
     * This sets how many fuel ticks the consumption will grant.
     *
     * @param fuelTicks
     *            The fuel ticks granted, must be positive
     */
    public void setFuelTicks(int fuelTicks) {
        Validate.isTrue(fuelTicks > 0, "The fuel ticks must be positive");
        this.fuelTicks = fuelTicks;
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

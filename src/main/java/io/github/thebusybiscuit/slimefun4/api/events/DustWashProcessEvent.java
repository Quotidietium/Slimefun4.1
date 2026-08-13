package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricDustWasher;

/**
 * This {@link Event} is fired whenever an {@link ElectricDustWasher} has computed a
 * wash result for a deterministic input (pulverized ore or sand) and is about to
 * consume the input and start the operation.
 * <p>
 * Cancelling this event vetoes the wash: the input stays in its slot and no operation
 * is started. The output can be replaced via {@link #setResult(ItemStack)} before it
 * is baked into the operation; the replacement is not re-checked against the output
 * slots.
 * <p>
 * The event does not fire when the input is neither pulverized ore nor sand, when the
 * output is jammed or when nothing would be consumed: the machine already idles in
 * those cases. Note that the {@link ElectricDustWasher} generates its recipes
 * dynamically, so {@link MachineRecipeStartEvent} never fires for it and this event
 * is the only veto point before the input is consumed. The sifted-ore path, whose
 * output is randomized per wash, is not covered by this event.
 *
 * @author Zurker
 *
 * @see AutoBrewEvent
 * @see AutoAnvilRepairEvent
 * @see ElectricDustWasher
 */
public class DustWashProcessEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ElectricDustWasher machine;
    private final Location location;
    private final ItemStack input;
    private ItemStack output;

    private boolean cancelled;

    public DustWashProcessEvent(@Nonnull ElectricDustWasher machine, @Nonnull Location location, @Nonnull ItemStack input, @Nonnull ItemStack output) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(machine, "The ElectricDustWasher must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(input, "The input must not be null");
        Validate.notNull(output, "The output must not be null");

        this.machine = machine;
        this.location = location;
        this.input = input;
        this.output = output;
    }

    /**
     * This returns the {@link ElectricDustWasher} that is about to wash.
     *
     * @return The {@link ElectricDustWasher}
     */
    @Nonnull
    public ElectricDustWasher getMachine() {
        return machine;
    }

    /**
     * This returns the {@link Location} of the {@link ElectricDustWasher}.
     *
     * @return The {@link Location} of the {@link ElectricDustWasher}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the input being washed, a live stack from the input slots - treat
     * it as read-only context.
     *
     * @return The input {@link ItemStack}
     */
    @Nonnull
    public ItemStack getInput() {
        return input;
    }

    /**
     * This returns the output that will be produced.
     *
     * @return The output {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return output;
    }

    /**
     * This sets the item that will be produced. The replacement is baked into the
     * operation without being re-checked against the output slots.
     *
     * @param output
     *            The replacement output, must not be null
     */
    public void setResult(@Nonnull ItemStack output) {
        Validate.notNull(output, "The output must not be null");
        this.output = output;
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

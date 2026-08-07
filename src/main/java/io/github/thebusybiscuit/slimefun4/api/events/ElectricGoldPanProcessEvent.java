package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.ElectricGoldPan;

/**
 * This {@link Event} is fired whenever an {@link ElectricGoldPan} has rolled a random
 * output for an input (gravel for the gold pan, soul sand or soul soil for the nether
 * gold pan) and is about to consume the input and start the operation.
 * <p>
 * Because the output is randomized per wash, this event is the only chance to observe
 * or fix a specific result: cancelling it vetoes the wash (the input stays and no
 * operation is started), and {@link #setResult(ItemStack)} replaces the rolled output
 * before it is baked into the operation. The replacement is not re-checked against the
 * output slots.
 * <p>
 * The event does not fire when the input is neither gravel nor a valid nether input,
 * when the rolled output is air, when the output is jammed or when nothing would be
 * consumed: the machine already idles in those cases. Note that the
 * {@link ElectricGoldPan} generates its recipes dynamically, so
 * {@link MachineRecipeStartEvent} never fires for it and this event is the only veto
 * point before the input is consumed.
 *
 * @author Zurker
 *
 * @see GoldPanUseEvent
 * @see DustWashProcessEvent
 * @see ElectricGoldPan
 */
public class ElectricGoldPanProcessEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ElectricGoldPan machine;
    private final Location location;
    private final ItemStack input;
    private ItemStack output;

    private boolean cancelled;

    public ElectricGoldPanProcessEvent(@Nonnull ElectricGoldPan machine, @Nonnull Location location, @Nonnull ItemStack input, @Nonnull ItemStack output) {
        Validate.notNull(machine, "The ElectricGoldPan must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(input, "The input must not be null");
        Validate.notNull(output, "The output must not be null");

        this.machine = machine;
        this.location = location;
        this.input = input;
        this.output = output;
    }

    /**
     * This returns the {@link ElectricGoldPan} that is about to process.
     *
     * @return The {@link ElectricGoldPan}
     */
    @Nonnull
    public ElectricGoldPan getMachine() {
        return machine;
    }

    /**
     * This returns the {@link Location} of the {@link ElectricGoldPan}.
     *
     * @return The {@link Location} of the {@link ElectricGoldPan}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the input being processed, a live stack from the input slots - treat
     * it as read-only context.
     *
     * @return The input {@link ItemStack}
     */
    @Nonnull
    public ItemStack getInput() {
        return input;
    }

    /**
     * This returns the randomly rolled output that will be produced.
     *
     * @return The rolled output {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return output;
    }

    /**
     * This sets the item that will be produced, overriding the rolled output. The
     * replacement is baked into the operation without being re-checked against the
     * output slots.
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

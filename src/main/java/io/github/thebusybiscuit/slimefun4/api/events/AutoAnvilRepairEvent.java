package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.AutoAnvil;

/**
 * This {@link Event} is fired whenever an {@link AutoAnvil} has computed a repair
 * and is about to consume the duct tape and the damaged item and start the
 * operation.
 * <p>
 * Cancelling this event vetoes the repair: both inputs stay in their input slots
 * and no operation is started. The repaired item can be replaced via
 * {@link #setResult(ItemStack)} before it is baked into the operation; the
 * replacement is not re-checked against the output slots.
 * <p>
 * The event does not fire when there is no damaged item, when the duct tape is
 * missing or when the output is jammed: the machine already idles in those cases
 * and nothing would be consumed. Note that the {@link AutoAnvil} generates its
 * recipes dynamically, so {@link MachineRecipeStartEvent} never fires for it and
 * this event is the only veto point before the inputs are consumed.
 *
 * @author Zurker
 *
 * @see AutoBrewEvent
 * @see BookBindEvent
 * @see AutoAnvil
 */
public class AutoAnvilRepairEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AutoAnvil anvil;
    private final Location location;
    private final ItemStack ductTape;
    private final ItemStack item;

    private ItemStack result;
    private boolean cancelled;

    public AutoAnvilRepairEvent(@Nonnull AutoAnvil anvil, @Nonnull Location location, @Nonnull ItemStack ductTape, @Nonnull ItemStack item, @Nonnull ItemStack result) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(anvil, "The AutoAnvil must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(ductTape, "The duct tape must not be null");
        Validate.notNull(item, "The item to repair must not be null");
        Validate.notNull(result, "The result must not be null");

        this.anvil = anvil;
        this.location = location;
        this.ductTape = ductTape;
        this.item = item;
        this.result = result;
    }

    /**
     * This returns the {@link AutoAnvil} that is about to repair.
     *
     * @return The {@link AutoAnvil}
     */
    @Nonnull
    public AutoAnvil getAnvil() {
        return anvil;
    }

    /**
     * This returns the {@link Location} of the {@link AutoAnvil}.
     *
     * @return The {@link Location} of the {@link AutoAnvil}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the duct tape being consumed, a live stack from the input
     * slots - treat it as read-only context.
     *
     * @return The duct tape
     */
    @Nonnull
    public ItemStack getDuctTape() {
        return ductTape;
    }

    /**
     * This returns the item being repaired, a live stack from the input slots -
     * treat it as read-only context.
     *
     * @return The damaged item
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the repaired item that will be produced.
     *
     * @return The repaired item
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This sets the item that will be produced. The replacement is baked into the
     * operation without being re-checked against the output slots.
     *
     * @param result
     *            The replacement item, must not be null
     */
    public void setResult(@Nonnull ItemStack result) {
        Validate.notNull(result, "The result must not be null");
        this.result = result;
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

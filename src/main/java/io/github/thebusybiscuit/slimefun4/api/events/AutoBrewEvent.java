package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.AutoBrewer;

/**
 * This {@link Event} is fired whenever an {@link AutoBrewer} has computed a brew
 * and is about to consume the potion and the ingredient and start the operation.
 * <p>
 * Cancelling this event vetoes the brew: both inputs stay in their input slots and
 * no operation is started. The resulting potion can be replaced via
 * {@link #setResult(ItemStack)} before it is baked into the operation; the
 * replacement is not re-checked against the output slots.
 * <p>
 * The event does not fire for an unknown recipe, a named ingredient or a jammed
 * output: the machine already idles in those cases and nothing would be consumed.
 * Note that the {@link AutoBrewer} generates its recipes dynamically, so
 * {@link MachineRecipeStartEvent} never fires for it and this event is the only
 * veto point before the inputs are consumed.
 *
 * @author Zurker
 *
 * @see BookBindEvent
 * @see AutoBrewer
 */
public class AutoBrewEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AutoBrewer brewer;
    private final Location location;
    private final ItemStack potion;
    private final ItemStack ingredient;

    private ItemStack result;
    private boolean cancelled;

    public AutoBrewEvent(@Nonnull AutoBrewer brewer, @Nonnull Location location, @Nonnull ItemStack potion, @Nonnull ItemStack ingredient, @Nonnull ItemStack result) {
        Validate.notNull(brewer, "The AutoBrewer must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(potion, "The potion must not be null");
        Validate.notNull(ingredient, "The ingredient must not be null");
        Validate.notNull(result, "The result must not be null");

        this.brewer = brewer;
        this.location = location;
        this.potion = potion;
        this.ingredient = ingredient;
        this.result = result;
    }

    /**
     * This returns the {@link AutoBrewer} that is about to brew.
     *
     * @return The {@link AutoBrewer}
     */
    @Nonnull
    public AutoBrewer getBrewer() {
        return brewer;
    }

    /**
     * This returns the {@link Location} of the {@link AutoBrewer}.
     *
     * @return The {@link Location} of the {@link AutoBrewer}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the potion being brewed, a live stack from the input slots -
     * treat it as read-only context.
     *
     * @return The input potion
     */
    @Nonnull
    public ItemStack getPotion() {
        return potion;
    }

    /**
     * This returns the ingredient being brewed with, a live stack from the input
     * slots - treat it as read-only context.
     *
     * @return The input ingredient
     */
    @Nonnull
    public ItemStack getIngredient() {
        return ingredient;
    }

    /**
     * This returns the potion that will be produced.
     *
     * @return The resulting potion
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This sets the potion that will be produced. The replacement is baked into
     * the operation without being re-checked against the output slots.
     *
     * @param result
     *            The replacement potion, must not be null
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

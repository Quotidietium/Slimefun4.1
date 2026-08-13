package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.BookBinder;

/**
 * This {@link Event} is fired whenever a {@link BookBinder} has combined two
 * enchanted books and is about to consume them and start the operation.
 * <p>
 * Cancelling this event vetoes the bind: both books stay in their input slots
 * and no operation is started. The resulting book can be replaced via
 * {@link #setResult(ItemStack)} before it is baked into the operation; the
 * replacement is not re-checked against the output slots.
 * <p>
 * The event does not fire when the combination would not change anything
 * (the result equals one of the inputs) or when the output slots cannot hold
 * the book: the machine already idles in those cases and no books would be
 * consumed. Note that the {@link BookBinder} generates its recipes dynamically,
 * so {@link MachineRecipeStartEvent} never fires for it and this event is the
 * only veto point before the books are consumed.
 *
 * @author Zurker
 *
 * @see AutoEnchantEvent
 * @see AsyncAutoEnchanterProcessEvent
 * @see BookBinder
 */
public class BookBindEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final BookBinder binder;
    private final Location location;
    private final ItemStack targetBook;
    private final ItemStack sourceBook;

    private ItemStack result;
    private boolean cancelled;

    public BookBindEvent(@Nonnull BookBinder binder, @Nonnull Location location, @Nonnull ItemStack targetBook, @Nonnull ItemStack sourceBook, @Nonnull ItemStack result) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(binder, "The BookBinder must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(targetBook, "The target book must not be null");
        Validate.notNull(sourceBook, "The source book must not be null");
        Validate.notNull(result, "The result must not be null");

        this.binder = binder;
        this.location = location;
        this.targetBook = targetBook;
        this.sourceBook = sourceBook;
        this.result = result;
    }

    /**
     * This returns the {@link BookBinder} that is about to bind.
     *
     * @return The {@link BookBinder}
     */
    @Nonnull
    public BookBinder getBinder() {
        return binder;
    }

    /**
     * This returns the {@link Location} of the {@link BookBinder}.
     *
     * @return The {@link Location} of the {@link BookBinder}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the target enchanted book, a live stack from the input
     * slots - treat it as read-only context.
     *
     * @return The target book
     */
    @Nonnull
    public ItemStack getTargetBook() {
        return targetBook;
    }

    /**
     * This returns the source enchanted book, a live stack from the input
     * slots - treat it as read-only context.
     *
     * @return The source book
     */
    @Nonnull
    public ItemStack getSourceBook() {
        return sourceBook;
    }

    /**
     * This returns the enchanted book that will be produced.
     *
     * @return The resulting book
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This sets the book that will be produced. The replacement is baked into
     * the operation without being re-checked against the output slots.
     *
     * @param result
     *            The replacement book, must not be null
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.AutoDisenchanter;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.AutoEnchanter;

import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * An {@link Event} that is called whenever an {@link AutoDisenchanter} is about to
 * disenchant an {@link ItemStack}, transferring its enchantments onto a book.
 * <p>
 * This is the process-level counterpart of the legacy {@link AutoDisenchantEvent},
 * mirroring the {@link AutoEnchanter}'s {@link AsyncAutoEnchanterProcessEvent}: it
 * carries the machine context (the {@link BlockMenu}) alongside both inputs and is
 * only fired once a plain book is present, right before the enchantments are
 * scanned. Cancelling this event vetoes the disenchant: nothing is consumed and no
 * operation is started.
 * <p>
 * Note that the {@link AutoDisenchanter} generates its recipes dynamically, so
 * {@link MachineRecipeStartEvent} never fires for it and this event (together with
 * the legacy {@link AutoDisenchantEvent}) is the only veto point before the inputs
 * are consumed. The event is asynchronous because machine tickers run on an
 * asynchronous thread.
 *
 * @author Zurker
 *
 * @see AutoDisenchantEvent
 * @see AsyncAutoEnchanterProcessEvent
 * @see AutoDisenchanter
 */
public class AsyncAutoDisenchanterProcessEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack item;
    private final ItemStack book;
    private final BlockMenu menu;

    private boolean cancelled;

    public AsyncAutoDisenchanterProcessEvent(@Nonnull ItemStack item, @Nonnull ItemStack book, @Nonnull BlockMenu menu) {
        super(true);

        Validate.notNull(item, "The item to disenchant cannot be null!");
        Validate.notNull(book, "The book to absorb the enchantments cannot be null!");
        Validate.notNull(menu, "The menu of auto-disenchanter cannot be null!");

        this.item = item;
        this.book = book;
        this.menu = menu;
    }

    /**
     * This returns the {@link ItemStack} that is being disenchanted.
     *
     * @return The {@link ItemStack} that is being disenchanted
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the plain book {@link ItemStack} that will absorb the
     * enchantments of the disenchanted item.
     *
     * @return The book {@link ItemStack} being consumed
     */
    @Nonnull
    public ItemStack getBook() {
        return book;
    }

    /**
     * This returns the {@link AutoDisenchanter}'s {@link BlockMenu}.
     *
     * @return The {@link BlockMenu} of the {@link AutoDisenchanter} that is disenchanting the item
     */
    @Nonnull
    public BlockMenu getMenu() {
        return menu;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}

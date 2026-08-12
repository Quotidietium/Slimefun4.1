package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.AutoDisenchanter;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting.AutoEnchanter;

import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * This {@link Event} is fired whenever an {@link AutoEnchanter} or an
 * {@link AutoDisenchanter} has selected the {@link Enchantment Enchantments} it is
 * about to transfer, right before the inputs are consumed and the operation starts.
 * <p>
 * The selection is the live {@link Map} the machine will transfer: removing entries
 * excludes those enchantments from the transfer, lowering a level downgrades what is
 * applied. The machine's own checks (enchantment applicability, level limits, the
 * AutoEnchanter's override-existing-levels setting) have already been applied, so the
 * map holds exactly what would be transferred without listeners. The processing time
 * and the max-enchants check are both evaluated on the final selection. If the map is
 * emptied, the machine treats it as "no valid enchantments" and idles.
 * <p>
 * Cancelling this event vetoes the whole operation: nothing is consumed and no
 * operation is started. This complements the process-level events
 * ({@link AsyncAutoEnchanterProcessEvent} / {@link AsyncAutoDisenchanterProcessEvent}),
 * which fire earlier and cannot see the selected enchantments.
 * <p>
 * For an {@link AutoEnchanter} the {@link #getItem() item} is the target and the
 * {@link #getBook() book} is the enchanted book being read; for an
 * {@link AutoDisenchanter} the item is the one being stripped and the book is the
 * plain book absorbing the enchantments. Note that for the disenchanting direction,
 * adding enchantments the item does not actually have is not supported (the removal
 * would fail and be logged). The event is asynchronous because machine tickers run on
 * an asynchronous thread.
 *
 * @author Zurker
 *
 * @see AsyncAutoEnchanterProcessEvent
 * @see AsyncAutoDisenchanterProcessEvent
 * @see AutoEnchanter
 * @see AutoDisenchanter
 */
public class AsyncAutoEnchantmentSelectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack item;
    private final ItemStack book;
    private final BlockMenu menu;
    private final Map<Enchantment, Integer> enchantments;

    private boolean cancelled;

    public AsyncAutoEnchantmentSelectEvent(@Nonnull ItemStack item, @Nonnull ItemStack book, @Nonnull BlockMenu menu, @Nonnull Map<Enchantment, Integer> enchantments) {
        super(true);

        Validate.notNull(item, "The item cannot be null!");
        Validate.notNull(book, "The book cannot be null!");
        Validate.notNull(menu, "The menu cannot be null!");
        Validate.notNull(enchantments, "The enchantment selection cannot be null!");

        this.item = item;
        this.book = book;
        this.menu = menu;
        this.enchantments = enchantments;
    }

    /**
     * This returns the {@link ItemStack} being processed: the target of an
     * {@link AutoEnchanter}, or the item being stripped by an
     * {@link AutoDisenchanter}. It is a live stack from the input slots - treat it
     * as read-only context.
     *
     * @return The item being processed
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the book {@link ItemStack} involved: the enchanted book an
     * {@link AutoEnchanter} reads from, or the plain book an
     * {@link AutoDisenchanter} writes to. It is a live stack from the input slots -
     * treat it as read-only context.
     *
     * @return The book {@link ItemStack}
     */
    @Nonnull
    public ItemStack getBook() {
        return book;
    }

    /**
     * This returns the machine's {@link BlockMenu}.
     *
     * @return The {@link BlockMenu} of the machine
     */
    @Nonnull
    public BlockMenu getMenu() {
        return menu;
    }

    /**
     * This returns the live selection of {@link Enchantment Enchantments} the machine
     * is about to transfer. The map is mutable on purpose: removing entries excludes
     * them from the transfer, changing a level adjusts what is applied. The
     * processing time and the max-enchants check are evaluated on the final content.
     *
     * @return The mutable enchantment selection
     */
    @Nonnull
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
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

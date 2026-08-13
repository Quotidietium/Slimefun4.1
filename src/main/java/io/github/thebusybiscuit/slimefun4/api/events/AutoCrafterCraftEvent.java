package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters.AbstractAutoCrafter;
import io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters.AbstractRecipe;

/**
 * This {@link Event} is fired whenever an {@link AbstractAutoCrafter} is about to perform
 * an automatic crafting operation during its tick, after the recipe and energy checks but
 * before any ingredients are consumed.
 * <p>
 * Cancelling this event skips this crafting attempt: no ingredients are consumed, no
 * energy is removed and the result is not produced. The {@link AbstractAutoCrafter} will
 * try again on its next tick.
 * <p>
 * Note that this event fires for every crafting <strong>attempt</strong>, regardless of
 * whether the required ingredients are actually present in the target {@link Inventory}.
 * Use {@link AutoCrafterCraftCompleteEvent} if you only care about successful crafts.
 * Since {@link AbstractAutoCrafter AutoCrafters} tick constantly, this event is only
 * allocated and fired when at least one listener is registered.
 *
 * @author Zurker
 *
 * @see AutoCrafterCraftCompleteEvent
 * @see AbstractAutoCrafter
 */
public class AutoCrafterCraftEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AbstractAutoCrafter crafter;
    private final Block block;
    private final Inventory inventory;
    private final AbstractRecipe recipe;
    private ItemStack result;

    private boolean cancelled;

    public AutoCrafterCraftEvent(@Nonnull AbstractAutoCrafter crafter, @Nonnull Block block, @Nonnull Inventory inventory, @Nonnull AbstractRecipe recipe) {

        // The AutoCrafter ticks synchronously (isSynchronized = true), so this fires on the main
        // thread. The adaptive declaration reports the actual context regardless.
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(crafter, "The AutoCrafter must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(inventory, "The Inventory must not be null");
        Validate.notNull(recipe, "The Recipe must not be null");

        this.crafter = crafter;
        this.block = block;
        this.inventory = inventory;
        this.recipe = recipe;
        this.result = recipe.getResult();
    }

    /**
     * This returns the {@link AbstractAutoCrafter} that is about to craft.
     *
     * @return The {@link AbstractAutoCrafter}
     */
    @Nonnull
    public AbstractAutoCrafter getCrafter() {
        return crafter;
    }

    /**
     * This returns the {@link Block} of the {@link AbstractAutoCrafter}.
     *
     * @return The crafter {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the target {@link Inventory} the ingredients are taken from
     * and the result is added to. This is the inventory of the container below
     * the {@link AbstractAutoCrafter}.
     *
     * @return The target {@link Inventory}
     */
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * This returns the {@link AbstractRecipe} that is about to be crafted.
     *
     * @return The {@link AbstractRecipe}
     */
    @Nonnull
    public AbstractRecipe getRecipe() {
        return recipe;
    }

    /**
     * This returns the {@link ItemStack} that will be produced by this craft,
     * initialized from {@link AbstractRecipe#getResult()} but overridable via
     * {@link #setResult(ItemStack)}.
     *
     * @return The result {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return result;
    }

    /**
     * This sets the {@link ItemStack} that will be produced by this craft,
     * overriding the recipe's default result. The replacement is added to the
     * target inventory without being re-checked for fit.
     *
     * @param result
     *            The replacement result, must not be null
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

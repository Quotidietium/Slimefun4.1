package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters.AbstractAutoCrafter;
import io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters.AbstractRecipe;

/**
 * This {@link Event} is fired whenever an {@link AbstractAutoCrafter} has successfully
 * completed an automatic crafting operation during its tick: the ingredients were
 * consumed and the result was added to the target {@link Inventory}.
 * It fires before the energy is removed and the particle effect is played.
 * <p>
 * This {@link Event} is not cancellable, the crafting operation has already happened.
 * Use {@link AutoCrafterCraftEvent} to prevent a crafting attempt.
 * Since {@link AbstractAutoCrafter AutoCrafters} tick constantly, this event is only
 * allocated and fired when at least one listener is registered.
 *
 * @author Zurker
 *
 * @see AutoCrafterCraftEvent
 * @see AbstractAutoCrafter
 */
public class AutoCrafterCraftCompleteEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final AbstractAutoCrafter crafter;
    private final Block block;
    private final Inventory inventory;
    private final AbstractRecipe recipe;

    public AutoCrafterCraftCompleteEvent(@Nonnull AbstractAutoCrafter crafter, @Nonnull Block block, @Nonnull Inventory inventory, @Nonnull AbstractRecipe recipe) {

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
    }

    /**
     * This returns the {@link AbstractAutoCrafter} that has crafted.
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
     * This returns the target {@link Inventory} the ingredients were taken from
     * and the result was added to. This is the inventory of the container below
     * the {@link AbstractAutoCrafter}.
     *
     * @return The target {@link Inventory}
     */
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * This returns the {@link AbstractRecipe} that was crafted.
     *
     * @return The {@link AbstractRecipe}
     */
    @Nonnull
    public AbstractRecipe getRecipe() {
        return recipe;
    }

    /**
     * This is a convenience method that returns the {@link ItemStack} that was crafted,
     * equivalent to {@link AbstractRecipe#getResult()}. This returns the original
     * {@link ItemStack} of the {@link AbstractRecipe}, so make sure to
     * {@link ItemStack#clone()} it before modifying.
     *
     * @return The crafted {@link ItemStack}
     */
    @Nonnull
    public ItemStack getResult() {
        return recipe.getResult();
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

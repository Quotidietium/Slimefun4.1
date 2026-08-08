package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.AutoBreeder;

/**
 * This {@link Event} is fired whenever an {@link AutoBreeder} is about to feed an
 * {@link Animals} entity: the energy has been validated, the organic food located and
 * the animal is about to enter love mode.
 * <p>
 * Cancelling this event skips this breeding operation entirely: the energy is kept,
 * the food is not consumed and the animal is left alone. The breeder retries on its
 * next tick.
 * <p>
 * The love mode duration (default 600 ticks = 30 seconds) can be modified via
 * {@link #setLoveModeTicks(int)} before the animal enters love mode, allowing addons
 * to extend or shorten the breeding window.
 *
 * @author Zurker
 *
 * @see AutoBreeder
 */
public class AutoBreedEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AutoBreeder breeder;
    private final Block block;
    private final Animals animal;
    private final ItemStack food;
    private int loveModeTicks = 600;

    private boolean cancelled;

    public AutoBreedEvent(@Nonnull AutoBreeder breeder, @Nonnull Block block, @Nonnull Animals animal, @Nonnull ItemStack food) {
        Validate.notNull(breeder, "The AutoBreeder must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(animal, "The Animals entity must not be null");
        Validate.notNull(food, "The food must not be null");

        this.breeder = breeder;
        this.block = block;
        this.animal = animal;
        this.food = food;
    }

    /**
     * This returns the {@link AutoBreeder} that is breeding.
     *
     * @return The {@link AutoBreeder}
     */
    @Nonnull
    public AutoBreeder getBreeder() {
        return breeder;
    }

    /**
     * This returns the {@link Block} of the {@link AutoBreeder}.
     *
     * @return The breeder {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link Animals} entity that is about to enter love mode.
     *
     * @return The {@link Animals} entity being fed
     */
    @Nonnull
    public Animals getAnimal() {
        return animal;
    }

    /**
     * This returns the organic food {@link ItemStack} the animal is about to be fed
     * from the breeder's input slots.
     *
     * @return The food being consumed
     */
    @Nonnull
    public ItemStack getFood() {
        return food;
    }

    /**
     * This returns the love mode duration (in ticks) that will be applied to the
     * animal. Default is 600 ticks (30 seconds).
     *
     * @return The love mode duration in ticks
     */
    public int getLoveModeTicks() {
        return loveModeTicks;
    }

    /**
     * This sets the love mode duration that will be applied to the animal. A value
     * of 0 effectively prevents breeding while still consuming the food.
     *
     * @param ticks
     *            The new love mode duration, must not be negative
     */
    public void setLoveModeTicks(int ticks) {
        Validate.isTrue(ticks >= 0, "The love mode duration must not be negative");
        this.loveModeTicks = ticks;
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

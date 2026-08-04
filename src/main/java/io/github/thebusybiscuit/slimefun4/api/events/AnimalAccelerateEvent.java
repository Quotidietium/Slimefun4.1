package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators.AnimalGrowthAccelerator;

/**
 * This {@link Event} is fired whenever an {@link AnimalGrowthAccelerator} has found a
 * baby animal within its radius and is about to accelerate its growth: the energy and
 * the organic food are about to be consumed and the animal's age is about to be
 * increased.
 * <p>
 * Cancelling this event skips this animal: no energy or food is consumed, the animal's
 * age is left alone and the accelerator continues scanning the other animals within
 * its radius during this tick.
 *
 * @author Zurker
 *
 * @see AnimalGrowthAccelerator
 * @see CropAccelerateEvent
 * @see TreeAccelerateEvent
 */
public class AnimalAccelerateEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AnimalGrowthAccelerator accelerator;
    private final Block block;
    private final Ageable animal;
    private final ItemStack food;

    private boolean cancelled;

    public AnimalAccelerateEvent(@Nonnull AnimalGrowthAccelerator accelerator, @Nonnull Block block, @Nonnull Ageable animal, @Nonnull ItemStack food) {
        Validate.notNull(accelerator, "The AnimalGrowthAccelerator must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(animal, "The animal must not be null");
        Validate.notNull(food, "The food must not be null");

        this.accelerator = accelerator;
        this.block = block;
        this.animal = animal;
        this.food = food;
    }

    /**
     * This returns the {@link AnimalGrowthAccelerator} that is about to accelerate.
     *
     * @return The {@link AnimalGrowthAccelerator}
     */
    @Nonnull
    public AnimalGrowthAccelerator getAccelerator() {
        return accelerator;
    }

    /**
     * This returns the {@link Block} of the {@link AnimalGrowthAccelerator}.
     *
     * @return The accelerator {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the baby animal that is about to be accelerated.
     *
     * @return The {@link Ageable} animal
     */
    @Nonnull
    public Ageable getAnimal() {
        return animal;
    }

    /**
     * This returns the organic food {@link ItemStack} that is about to be consumed.
     *
     * @return The food {@link ItemStack}
     */
    @Nonnull
    public ItemStack getFood() {
        return food;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;

/**
 * This {@link Event} is fired whenever an {@link AContainer} machine has matched a
 * {@link MachineRecipe} against its inputs and is about to consume them and start the
 * operation.
 * <p>
 * Cancelling this event vetoes the recipe: the inputs stay untouched, no operation is
 * started and the machine idles for this tick (it will re-scan on the next one). This
 * complements {@link AsyncMachineOperationStartEvent}, which only fires after the
 * inputs were already consumed.
 * <p>
 * The event does not fire when a recipe matched but the output slots cannot hold the
 * results: the machine already idles in that case and no inputs would be consumed.
 *
 * @author Zurker
 *
 * @see AsyncMachineOperationStartEvent
 * @see GeneratorFuelBurnEvent
 */
public class MachineRecipeStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AContainer machine;
    private final Location location;
    private final MachineRecipe recipe;

    private boolean cancelled;

    public MachineRecipeStartEvent(@Nonnull AContainer machine, @Nonnull Location location, @Nonnull MachineRecipe recipe) {
        Validate.notNull(machine, "The AContainer must not be null");
        Validate.notNull(location, "The Location must not be null");
        Validate.notNull(recipe, "The MachineRecipe must not be null");

        this.machine = machine;
        this.location = location;
        this.recipe = recipe;
    }

    /**
     * This returns the {@link AContainer} that is about to start the recipe.
     *
     * @return The {@link AContainer}
     */
    @Nonnull
    public AContainer getMachine() {
        return machine;
    }

    /**
     * This returns the {@link Location} of the {@link AContainer}.
     *
     * @return The {@link Location} of the {@link AContainer}
     */
    @Nonnull
    public Location getLocation() {
        return location;
    }

    /**
     * This returns the {@link MachineRecipe} that is about to be started.
     *
     * @return The {@link MachineRecipe}
     */
    @Nonnull
    public MachineRecipe getRecipe() {
        return recipe;
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

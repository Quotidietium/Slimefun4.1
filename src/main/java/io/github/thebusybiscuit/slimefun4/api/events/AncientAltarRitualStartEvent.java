package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} activates an {@link AncientAltar}
 * with a valid recipe and the ritual is about to start: the catalyst is about to be consumed
 * and the ritual animation is about to begin.
 * <p>
 * Cancelling this event prevents the ritual: the catalyst is not consumed, the items on the
 * {@link AncientPedestal Pedestals} stay in place and the altar is released for further use.
 *
 * @author Zurker
 *
 * @see AncientAltar
 * @see AncientAltarCraftEvent
 */
public class AncientAltarRitualStartEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block altar;
    private final List<Block> pedestals;
    private final ItemStack catalyst;
    private final List<ItemStack> input;
    private final ItemStack output;

    private boolean cancelled;

    public AncientAltarRitualStartEvent(@Nonnull Player player, @Nonnull Block altar, @Nonnull List<Block> pedestals, @Nonnull ItemStack catalyst, @Nonnull List<ItemStack> input, @Nonnull ItemStack output) {
        super(player);

        Validate.notNull(altar, "The altar must not be null");
        Validate.notNull(pedestals, "The pedestals must not be null");
        Validate.notNull(catalyst, "The catalyst must not be null");
        Validate.notNull(input, "The input must not be null");
        Validate.notNull(output, "The output must not be null");

        this.altar = altar;
        this.pedestals = Collections.unmodifiableList(new ArrayList<>(pedestals));
        this.catalyst = catalyst;
        this.input = Collections.unmodifiableList(new ArrayList<>(input));
        this.output = output;
    }

    /**
     * This returns the {@link AncientAltar} {@link Block} that was activated.
     *
     * @return The altar {@link Block}
     */
    @Nonnull
    public Block getAltar() {
        return altar;
    }

    /**
     * This returns the eight {@link AncientPedestal} {@link Block Blocks} surrounding
     * the altar.
     *
     * @return An unmodifiable {@link List} of the pedestal {@link Block Blocks}
     */
    @Nonnull
    public List<Block> getPedestals() {
        return pedestals;
    }

    /**
     * This returns the catalyst the {@link Player} activated the altar with.
     *
     * @return The catalyst {@link ItemStack}
     */
    @Nonnull
    public ItemStack getCatalyst() {
        return catalyst;
    }

    /**
     * This returns the items resting on the {@link AncientPedestal Pedestals}, in the
     * order the pedestals were scanned.
     *
     * @return An unmodifiable {@link List} of the input {@link ItemStack ItemStacks}
     */
    @Nonnull
    public List<ItemStack> getInput() {
        return input;
    }

    /**
     * This returns the result of the matched recipe.
     *
     * @return The output {@link ItemStack}
     */
    @Nonnull
    public ItemStack getOutput() {
        return output;
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

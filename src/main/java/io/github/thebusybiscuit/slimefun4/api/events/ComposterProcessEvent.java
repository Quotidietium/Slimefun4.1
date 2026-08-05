package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.Composter;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link Composter}
 * with a valid input and the composter is about to process it: the input is about to be
 * consumed and an output {@link ItemStack} is about to be produced.
 * <p>
 * The output {@link ItemStack} is modifiable via {@link #setOutput(ItemStack)} - an addon may
 * replace the produced item. Cancelling this event skips the processing entirely: the input is
 * left in the player's hand and nothing is produced.
 *
 * @author Zurker
 *
 * @see Composter
 */
public class ComposterProcessEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Composter composter;
    private final Block block;
    private final ItemStack input;
    private ItemStack output;

    private boolean cancelled;

    public ComposterProcessEvent(@Nonnull Player player, @Nonnull Composter composter, @Nonnull Block block, @Nonnull ItemStack input, @Nonnull ItemStack output) {
        super(player);
        Validate.notNull(composter, "The Composter must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(input, "The input must not be null");
        Validate.notNull(output, "The output must not be null");

        this.composter = composter;
        this.block = block;
        this.input = input;
        this.output = output;
    }

    /**
     * This returns the {@link Composter} that is processing.
     *
     * @return The {@link Composter}
     */
    @Nonnull
    public Composter getComposter() {
        return composter;
    }

    /**
     * This returns the {@link Block} of the {@link Composter}.
     *
     * @return The {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the input {@link ItemStack} that is about to be consumed.
     *
     * @return The input {@link ItemStack}
     */
    @Nonnull
    public ItemStack getInput() {
        return input;
    }

    /**
     * This returns the output {@link ItemStack} that is about to be produced. Use
     * {@link #setOutput(ItemStack)} to replace it.
     *
     * @return The output {@link ItemStack}
     */
    @Nonnull
    public ItemStack getOutput() {
        return output;
    }

    /**
     * This replaces the output {@link ItemStack} that will be produced.
     *
     * @param output
     *            The new output, not {@code null}
     */
    public void setOutput(@Nullable ItemStack output) {
        Validate.notNull(output, "The output must not be null");
        this.output = output;
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

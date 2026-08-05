package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.Crucible;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link Crucible}
 * with a valid input and the crucible is about to generate a liquid above it: the input is
 * about to be consumed and either water or lava is about to be placed.
 * <p>
 * Whether water or lava is generated is modifiable via {@link #setWater(boolean)} - an addon
 * may override the derived type. Cancelling this event skips the generation entirely: the
 * input is left in the player's hand and no liquid is placed.
 *
 * @author Zurker
 *
 * @see Crucible
 */
public class CrucibleLiquidGenerateEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Crucible crucible;
    private final Block block;
    private final ItemStack input;
    private boolean water;

    private boolean cancelled;

    public CrucibleLiquidGenerateEvent(@Nonnull Player player, @Nonnull Crucible crucible, @Nonnull Block block, @Nonnull ItemStack input, boolean water) {
        super(player);
        Validate.notNull(crucible, "The Crucible must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(input, "The input must not be null");

        this.crucible = crucible;
        this.block = block;
        this.input = input;
        this.water = water;
    }

    /**
     * This returns the {@link Crucible} that is generating the liquid.
     *
     * @return The {@link Crucible}
     */
    @Nonnull
    public Crucible getCrucible() {
        return crucible;
    }

    /**
     * This returns the {@link Block} above the crucible where the liquid is placed.
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
     * This returns whether water ({@code true}) or lava ({@code false}) is about to be
     * generated. Use {@link #setWater(boolean)} to override it.
     *
     * @return {@code true} for water, {@code false} for lava
     */
    public boolean isWater() {
        return water;
    }

    /**
     * This overrides the liquid that will be generated.
     *
     * @param water
     *            {@code true} for water, {@code false} for lava
     */
    public void setWater(boolean water) {
        this.water = water;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.IgnitionChamber;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.Smeltery;

/**
 * This {@link PlayerEvent} is fired whenever an {@link IgnitionChamber} is about to
 * consume one point of durability from a Flint and Steel to (re-)ignite a
 * {@link Smeltery}.
 * <p>
 * Cancelling this event vetoes the ignition: the Flint and Steel keeps its current
 * durability and the {@link Smeltery} is not ignited for this attempt. Note that the
 * "no flint and steel" message is not sent for a vetoed attempt, since a Flint and
 * Steel was actually present.
 *
 * @author Zurker
 *
 * @see IgnitionChamber
 * @see Smeltery
 */
public class IgnitionChamberUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block smelteryBlock;
    private final Block chamber;
    private final ItemStack flintAndSteel;

    private boolean cancelled;

    public IgnitionChamberUseEvent(@Nonnull Player player, @Nonnull Block smelteryBlock, @Nonnull Block chamber, @Nonnull ItemStack flintAndSteel) {
        super(player);
        Validate.notNull(smelteryBlock, "The Smeltery block must not be null");
        Validate.notNull(chamber, "The IgnitionChamber block must not be null");
        Validate.notNull(flintAndSteel, "The Flint and Steel must not be null");

        this.smelteryBlock = smelteryBlock;
        this.chamber = chamber;
        this.flintAndSteel = flintAndSteel;
    }

    /**
     * This returns the {@link Smeltery} {@link Block} that is being ignited.
     *
     * @return The {@link Smeltery} {@link Block}
     */
    @Nonnull
    public Block getSmelteryBlock() {
        return smelteryBlock;
    }

    /**
     * This returns the {@link Block} of the {@link IgnitionChamber} that provides
     * the Flint and Steel.
     *
     * @return The {@link IgnitionChamber} {@link Block}
     */
    @Nonnull
    public Block getChamber() {
        return chamber;
    }

    /**
     * This returns the Flint and Steel that is about to lose one point of durability.
     * This is the live {@link ItemStack} inside the {@link IgnitionChamber}'s
     * inventory.
     *
     * @return The Flint and Steel
     */
    @Nonnull
    public ItemStack getFlintAndSteel() {
        return flintAndSteel;
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

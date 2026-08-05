package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.SmeltersPickaxe;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} mines an ore with a
 * {@link SmeltersPickaxe} and one of the resulting drops has a furnace recipe: the drop
 * is about to be transformed into its smelted counterpart.
 * <p>
 * Cancelling this event skips the smelting transformation for this drop; the drop keeps
 * its raw form (the fortune-based amount adjustment still applies, as it does for drops
 * without a furnace recipe).
 *
 * @author Zurker
 *
 * @see SmeltersPickaxe
 */
public class SmeltersPickaxeSmeltEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SmeltersPickaxe pickaxe;
    private final Block block;
    private final ItemStack drop;
    private final ItemStack output;

    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public SmeltersPickaxeSmeltEvent(Player player, SmeltersPickaxe pickaxe, Block block, ItemStack drop, ItemStack output) {
        super(player);
        Validate.notNull(pickaxe, "The SmeltersPickaxe must not be null");
        Validate.notNull(block, "The mined Block must not be null");
        Validate.notNull(drop, "The drop being smelted must not be null");
        Validate.notNull(output, "The smelted output must not be null");

        this.pickaxe = pickaxe;
        this.block = block;
        this.drop = drop;
        this.output = output;
    }

    /**
     * This returns the {@link SmeltersPickaxe} that mined the ore.
     *
     * @return The {@link SmeltersPickaxe}
     */
    @Nonnull
    public SmeltersPickaxe getPickaxe() {
        return pickaxe;
    }

    /**
     * This returns the ore {@link Block} that was mined.
     *
     * @return The mined {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the live drop {@link ItemStack} that is about to be smelted.
     * At event time it still has its raw (unsmelted) type.
     *
     * @return The drop being smelted
     */
    @Nonnull
    public ItemStack getDrop() {
        return drop;
    }

    /**
     * This returns the furnace result {@link ItemStack} whose type the drop is about
     * to take on.
     *
     * @return The smelted output
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

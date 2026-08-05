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

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GoldPan;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a valid block
 * (e.g. gravel) with a {@link GoldPan} and the pan is about to sift the block: the block
 * is about to be consumed and a random output {@link ItemStack} is about to be spawned.
 * <p>
 * The output {@link ItemStack} is modifiable via {@link #setOutput(ItemStack)} - an addon
 * may replace the randomly rolled drop with a custom item. Cancelling this event skips the
 * sifting entirely: the block is left untouched and nothing is spawned.
 *
 * @author Zurker
 *
 * @see GoldPan
 */
public class GoldPanUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final GoldPan goldPan;
    private final Block block;
    private ItemStack output;

    private boolean cancelled;

    public GoldPanUseEvent(@Nonnull Player player, @Nonnull GoldPan goldPan, @Nonnull Block block, @Nonnull ItemStack output) {
        super(player);
        Validate.notNull(goldPan, "The GoldPan must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(output, "The output must not be null");

        this.goldPan = goldPan;
        this.block = block;
        this.output = output;
    }

    /**
     * This returns the {@link GoldPan} that was used.
     *
     * @return The {@link GoldPan}
     */
    @Nonnull
    public GoldPan getGoldPan() {
        return goldPan;
    }

    /**
     * This returns the {@link Block} that is being sifted.
     *
     * @return The {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the randomly rolled output {@link ItemStack} that is about to be spawned.
     * Use {@link #setOutput(ItemStack)} to replace it.
     *
     * @return The output {@link ItemStack}
     */
    @Nonnull
    public ItemStack getOutput() {
        return output;
    }

    /**
     * This replaces the output {@link ItemStack} that will be spawned.
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

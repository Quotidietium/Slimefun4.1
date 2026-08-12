package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.MinerAndroid;

/**
 * This {@link Event} is fired before a {@link MinerAndroid} mines a {@link Block}.
 * If this {@link Event} is cancelled, the {@link Block} will not be mined.
 * <p>
 * The drops the block would yield (computed as if broken with a diamond pickaxe) are
 * exposed via {@link #getDrops()}: addons may remove entries (e.g. to void specific
 * drops) or add new ones (e.g. a custom loot table) before they are pushed into the
 * android's inventory. Events constructed via the legacy two-argument constructor carry
 * an empty drop list; modifying that list has no effect.
 *
 * @author poma123
 *
 */
public class AndroidMineEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block block;
    private final AndroidInstance android;
    private final List<ItemStack> drops;
    private boolean cancelled;

    /**
     * @param block
     *            The mined {@link Block}
     * @param android
     *            The {@link AndroidInstance} that triggered this {@link Event}
     */
    @ParametersAreNonnullByDefault
    public AndroidMineEvent(Block block, AndroidInstance android) {
        this(block, android, Collections.emptyList());
    }

    /**
     * @param block
     *            The mined {@link Block}
     * @param android
     *            The {@link AndroidInstance} that triggered this {@link Event}
     * @param drops
     *            The {@link ItemStack ItemStacks} the block would drop
     */
    @ParametersAreNonnullByDefault
    public AndroidMineEvent(Block block, AndroidInstance android, Collection<ItemStack> drops) {
        Validate.notNull(block, "The mined Block must not be null");
        Validate.notNull(android, "The AndroidInstance must not be null");
        Validate.notNull(drops, "The drops must not be null");

        this.block = block;
        this.android = android;
        this.drops = new ArrayList<>(drops);
    }

    /**
     * This method returns the mined {@link Block}
     *
     * @return the mined {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This method returns the {@link AndroidInstance} who
     * triggered this {@link Event}
     *
     * @return the involved {@link AndroidInstance}
     */
    @Nonnull
    public AndroidInstance getAndroid() {
        return android;
    }

    /**
     * This method returns the mutable {@link List} of {@link ItemStack ItemStacks} the
     * block is about to drop into the android's inventory. Removing or adding entries
     * changes what the android collects; clearing the list breaks the block without
     * any drops.
     *
     * @return the drops of the mined {@link Block}
     */
    @Nonnull
    public List<ItemStack> getDrops() {
        return drops;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
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
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
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.WoodcutterAndroid;

/**
 * This {@link Event} is fired before a {@link WoodcutterAndroid} breaks a log {@link Block}
 * while chopping a tree: the log was found in the vein the android is working on and the
 * android's owner was verified to have permission to break it. The event fires once per log,
 * mirroring how an {@link AndroidMineEvent} fires once per mined block.
 * <p>
 * If this {@link Event} is cancelled, the log is not broken: no drop is pushed into the
 * android's inventory and a bottom log is not replanted. The android keeps working on the
 * same tree and retries on its next tick.
 * <p>
 * The drops the log would yield (one log of its own type) are exposed via {@link #getDrops()}:
 * addons may remove entries (e.g. to void the drop) or add new ones (e.g. to let the android
 * collect charcoal instead) before they are pushed into the android's inventory. Events
 * constructed via the legacy two-argument constructor carry an empty drop list; modifying
 * that list has no effect.
 *
 * @author Zurker
 *
 * @see WoodcutterAndroid
 * @see AndroidMineEvent
 */
public class AndroidChopTreeEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block block;
    private final AndroidInstance android;
    private final List<ItemStack> drops;
    private boolean cancelled;

    /**
     * @param block
     *            The log {@link Block} about to be chopped
     * @param android
     *            The {@link AndroidInstance} that triggered this {@link Event}
     */
    @ParametersAreNonnullByDefault
    public AndroidChopTreeEvent(Block block, AndroidInstance android) {
        this(block, android, Collections.emptyList());
    }

    /**
     * @param block
     *            The log {@link Block} about to be chopped
     * @param android
     *            The {@link AndroidInstance} that triggered this {@link Event}
     * @param drops
     *            The {@link ItemStack ItemStacks} the log would drop
     */
    @ParametersAreNonnullByDefault
    public AndroidChopTreeEvent(Block block, AndroidInstance android, Collection<ItemStack> drops) {
        Validate.notNull(block, "The log Block must not be null");
        Validate.notNull(android, "The AndroidInstance must not be null");
        Validate.notNull(drops, "The drops must not be null");

        this.block = block;
        this.android = android;
        this.drops = new ArrayList<>(drops);
    }

    /**
     * This method returns the log {@link Block} about to be chopped.
     *
     * @return The log {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This method returns the {@link AndroidInstance} who triggered this {@link Event}.
     *
     * @return The involved {@link AndroidInstance}
     */
    @Nonnull
    public AndroidInstance getAndroid() {
        return android;
    }

    /**
     * This method returns the mutable {@link List} of {@link ItemStack ItemStacks} the
     * log is about to drop into the android's inventory. Removing or adding entries
     * changes what the android collects; clearing the list breaks the log without
     * any drops.
     *
     * @return the drops of the chopped log
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

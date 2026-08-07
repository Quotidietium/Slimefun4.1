package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.MultiTool;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} sneak-right-clicks with a
 * {@link MultiTool} to switch its mode: the next enabled mode has been selected and is
 * about to be applied (message, persistent data and lore update).
 * <p>
 * The target mode index is modifiable via {@link #setNextIndex(int)} - an addon may
 * redirect the switch to a different mode. Cancelling this event skips the switch
 * entirely: the {@link MultiTool} keeps its current mode and no message is sent.
 *
 * @author Zurker
 *
 * @see MultiTool
 */
public class MultiToolModeSwitchEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final MultiTool multiTool;
    private final ItemStack item;
    private final int previousIndex;
    private final int modeCount;
    private int nextIndex;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public MultiToolModeSwitchEvent(Player player, MultiTool multiTool, ItemStack item, int previousIndex, int nextIndex, int modeCount) {
        super(player);
        Validate.notNull(multiTool, "The MultiTool must not be null");
        Validate.notNull(item, "The MultiTool item must not be null");
        Validate.isTrue(modeCount > 0, "The mode count must be positive");
        Validate.isTrue(previousIndex >= 0 && previousIndex < modeCount, "The previous index must be within the mode range");
        Validate.isTrue(nextIndex >= 0 && nextIndex < modeCount, "The next index must be within the mode range");

        this.multiTool = multiTool;
        this.item = item;
        this.previousIndex = previousIndex;
        this.nextIndex = nextIndex;
        this.modeCount = modeCount;
    }

    /**
     * This returns the {@link MultiTool} that is being switched.
     *
     * @return The {@link MultiTool}
     */
    @Nonnull
    public MultiTool getMultiTool() {
        return multiTool;
    }

    /**
     * This returns the {@link MultiTool} {@link ItemStack} held by the {@link Player}.
     *
     * @return The {@link MultiTool} {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the index of the mode the {@link MultiTool} is switching away from.
     *
     * @return The current mode index
     */
    public int getPreviousIndex() {
        return previousIndex;
    }

    /**
     * This returns the index of the mode the {@link MultiTool} is about to switch to.
     * Use {@link #setNextIndex(int)} to redirect the switch.
     *
     * @return The target mode index
     */
    public int getNextIndex() {
        return nextIndex;
    }

    /**
     * This redirects the switch to a different mode index.
     *
     * @param nextIndex
     *            The new target mode index, must be within {@code [0, getModeCount())}
     */
    public void setNextIndex(int nextIndex) {
        Validate.isTrue(nextIndex >= 0 && nextIndex < modeCount, "The next index must be within the mode range");
        this.nextIndex = nextIndex;
    }

    /**
     * This returns how many modes the {@link MultiTool} has. Valid mode indices are
     * within {@code [0, getModeCount())}.
     *
     * @return The number of modes
     */
    public int getModeCount() {
        return modeCount;
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

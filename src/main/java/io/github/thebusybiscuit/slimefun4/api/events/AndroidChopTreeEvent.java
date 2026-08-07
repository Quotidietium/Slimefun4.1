package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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
    private boolean cancelled;

    /**
     * @param block
     *            The log {@link Block} about to be chopped
     * @param android
     *            The {@link AndroidInstance} that triggered this {@link Event}
     */
    @ParametersAreNonnullByDefault
    public AndroidChopTreeEvent(Block block, AndroidInstance android) {
        Validate.notNull(block, "The log Block must not be null");
        Validate.notNull(android, "The AndroidInstance must not be null");

        this.block = block;
        this.android = android;
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

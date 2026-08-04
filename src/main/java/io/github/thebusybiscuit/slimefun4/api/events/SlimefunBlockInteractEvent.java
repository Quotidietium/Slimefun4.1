package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link Block}
 * that belongs to a {@link SlimefunItem} ({@link Action#RIGHT_CLICK_BLOCK}), after the
 * permission checks but before any {@link BlockUseHandler} is called or any inventory
 * is opened.
 * <p>
 * This event fires for every {@link SlimefunItem} block, including machines without a
 * {@link BlockUseHandler} whose inventory would open instead.
 * <p>
 * Cancelling this event prevents the interaction entirely: no {@link BlockUseHandler}
 * is called, no inventory is opened and the underlying {@link PlayerInteractEvent}
 * is cancelled as well.
 *
 * @author Zurker
 *
 * @see PlayerRightClickEvent
 * @see BlockUseHandler
 */
public class SlimefunBlockInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final Block clickedBlock;
    private final PlayerRightClickEvent rightClickEvent;

    private boolean cancelled;

    public SlimefunBlockInteractEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull Block clickedBlock, @Nonnull PlayerRightClickEvent rightClickEvent) {
        super(player);

        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(clickedBlock, "The clicked block must not be null");
        Validate.notNull(rightClickEvent, "The right click event must not be null");

        this.slimefunItem = slimefunItem;
        this.clickedBlock = clickedBlock;
        this.rightClickEvent = rightClickEvent;
    }

    /**
     * This returns the {@link SlimefunItem} the clicked {@link Block} belongs to.
     *
     * @return The {@link SlimefunItem} of the clicked {@link Block}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the clicked {@link Block}.
     *
     * @return The clicked {@link Block}
     */
    @Nonnull
    public Block getClickedBlock() {
        return clickedBlock;
    }

    /**
     * This returns the underlying {@link PlayerRightClickEvent}, giving access to
     * the held item, the clicked face and the original {@link PlayerInteractEvent}.
     *
     * @return The underlying {@link PlayerRightClickEvent}
     */
    @Nonnull
    public PlayerRightClickEvent getRightClickEvent() {
        return rightClickEvent;
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

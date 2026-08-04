package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} breaks a block with a
 * {@link SlimefunItem} as its tool, after the permission checks but before its
 * {@link ToolUseHandler} is called.
 * <p>
 * Cancelling this event skips the {@link ToolUseHandler}: the block breaks plainly,
 * without any of the tool's effects. The underlying {@link BlockBreakEvent} is not
 * affected.
 *
 * @author Zurker
 *
 * @see ToolUseHandler
 */
public class SlimefunToolUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunItem slimefunItem;
    private final ItemStack tool;
    private final BlockBreakEvent breakEvent;
    private final int fortune;
    private final List<ItemStack> drops;

    private boolean cancelled;

    public SlimefunToolUseEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack tool, @Nonnull BlockBreakEvent breakEvent, int fortune, @Nonnull List<ItemStack> drops) {
        super(player);

        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(tool, "The tool must not be null");
        Validate.notNull(breakEvent, "The break event must not be null");
        Validate.notNull(drops, "The drops must not be null");

        this.slimefunItem = slimefunItem;
        this.tool = tool;
        this.breakEvent = breakEvent;
        this.fortune = fortune;
        this.drops = drops;
    }

    /**
     * This returns the {@link SlimefunItem} used as the tool.
     *
     * @return The {@link SlimefunItem} tool
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the held {@link ItemStack} used as the tool.
     *
     * @return The tool {@link ItemStack}
     */
    @Nonnull
    public ItemStack getTool() {
        return tool;
    }

    /**
     * This returns the underlying {@link BlockBreakEvent}.
     *
     * @return The underlying {@link BlockBreakEvent}
     */
    @Nonnull
    public BlockBreakEvent getBreakEvent() {
        return breakEvent;
    }

    /**
     * This returns the amount of bonus drops to be expected from the fortune
     * {@link Enchantment} of the tool, mirroring the value the {@link ToolUseHandler}
     * receives. It is 1 for tools without fortune.
     *
     * @return The amount of bonus drops
     */
    public int getFortune() {
        return fortune;
    }

    /**
     * This returns the live {@link List} of drops for this break. It is the very same
     * {@link List} the {@link ToolUseHandler} receives, so any modification applies
     * to the drops of this break.
     *
     * @return The mutable {@link List} of drops
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

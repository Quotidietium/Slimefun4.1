package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} changes the mode of their
 * Slimefun guide in the guide settings, e.g. from survival to cheat mode.
 * <p>
 * It only fires on an actual change; clicking the option without the permission for
 * another mode resolves to the current mode and fires nothing. Cancelling this event
 * vetoes the change: the guide keeps its current mode.
 *
 * @author Zurker
 *
 * @see SlimefunGuideMode
 * @see SlimefunGuideOpenEvent
 */
public class GuideModeChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ItemStack guide;
    private final SlimefunGuideMode previousMode;
    private final SlimefunGuideMode newMode;

    private boolean cancelled;

    public GuideModeChangeEvent(@Nonnull Player player, @Nonnull ItemStack guide, @Nonnull SlimefunGuideMode previousMode, @Nonnull SlimefunGuideMode newMode) {
        super(player);
        Validate.notNull(guide, "The guide item must not be null");
        Validate.notNull(previousMode, "The previous mode must not be null");
        Validate.notNull(newMode, "The new mode must not be null");

        this.guide = guide;
        this.previousMode = previousMode;
        this.newMode = newMode;
    }

    /**
     * This returns the guide {@link ItemStack} whose mode is being changed.
     * This is the live item: its meta is rewritten when the change goes through.
     *
     * @return The guide {@link ItemStack}
     */
    @Nonnull
    public ItemStack getGuide() {
        return guide;
    }

    /**
     * This returns the {@link SlimefunGuideMode} the guide is currently in.
     *
     * @return The previous {@link SlimefunGuideMode}
     */
    @Nonnull
    public SlimefunGuideMode getPreviousMode() {
        return previousMode;
    }

    /**
     * This returns the {@link SlimefunGuideMode} the guide is about to switch to.
     *
     * @return The new {@link SlimefunGuideMode}
     */
    @Nonnull
    public SlimefunGuideMode getNewMode() {
        return newMode;
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

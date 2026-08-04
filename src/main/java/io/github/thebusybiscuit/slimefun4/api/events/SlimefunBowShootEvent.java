package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SlimefunBow;

/**
 * This event is fired whenever a {@link Player} shoots a {@link SlimefunBow}.
 *
 * <p>
 * It wraps Bukkit's {@link EntityShootBowEvent} and additionally exposes the resolved
 * {@link SlimefunBow} and the fired {@link Arrow}. Cancelling it prevents the shot from being
 * <b>tracked</b> by Slimefun - meaning the {@link io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler}
 * will <b>not</b> be invoked when the arrow later hits something. The arrow itself still flies
 * (the underlying {@link EntityShootBowEvent} is left untouched), so vanilla bow behaviour is
 * never disturbed.
 * </p>
 *
 * @author Zurker
 *
 * @see SlimefunBow
 * @see io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler
 *
 */
public class SlimefunBowShootEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SlimefunBow bow;
    private final ItemStack bowItem;
    private final Arrow arrow;
    private final EntityShootBowEvent underlyingEvent;
    private boolean cancelled;

    public SlimefunBowShootEvent(@Nonnull Player player, @Nonnull SlimefunBow bow, @Nonnull ItemStack bowItem, @Nonnull Arrow arrow, @Nonnull EntityShootBowEvent underlyingEvent) {
        super(player);

        Validate.notNull(bow, "The SlimefunBow cannot be null");
        Validate.notNull(bowItem, "The bow ItemStack cannot be null");
        Validate.notNull(arrow, "The Arrow cannot be null");
        Validate.notNull(underlyingEvent, "The underlying EntityShootBowEvent cannot be null");

        this.bow = bow;
        this.bowItem = bowItem;
        this.arrow = arrow;
        this.underlyingEvent = underlyingEvent;
    }

    /**
     * The {@link SlimefunBow} that was fired.
     *
     * @return The {@link SlimefunBow}
     */
    @Nonnull
    public SlimefunBow getBow() {
        return bow;
    }

    /**
     * The {@link ItemStack} of the bow that was fired.
     *
     * @return The bow {@link ItemStack}
     */
    @Nonnull
    public ItemStack getBowItem() {
        return bowItem;
    }

    /**
     * The {@link Arrow} projectile that was shot.
     *
     * @return The fired {@link Arrow}
     */
    @Nonnull
    public Arrow getArrow() {
        return arrow;
    }

    /**
     * The underlying Bukkit {@link EntityShootBowEvent}.
     *
     * @return The wrapped {@link EntityShootBowEvent}
     */
    @Nonnull
    public EntityShootBowEvent getEntityShootBowEvent() {
        return underlyingEvent;
    }

    /**
     * Whether this shot will be tracked by Slimefun.
     *
     * @return {@code true} if the shot is suppressed (not tracked)
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * If cancelled, the shot is no longer tracked by Slimefun and the
     * {@link io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler} will not fire on
     * hit. The arrow still flies normally.
     *
     * @param cancel
     *            {@code true} to stop tracking this shot
     */
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

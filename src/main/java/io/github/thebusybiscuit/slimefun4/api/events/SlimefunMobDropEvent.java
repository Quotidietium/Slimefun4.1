package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.attributes.RandomMobDrop;

/**
 * This {@link Event} is fired whenever a custom Slimefun mob drop is about to be
 * added to an {@link EntityDeathEvent}'s drops - after the killer's permission
 * check and any {@link RandomMobDrop} chance roll have already passed.
 * <p>
 * Cancelling this event prevents this particular drop from being added.
 *
 * @author Zurker
 *
 * @see SlimefunEntityKillEvent
 * @see RandomMobDrop
 */
public class SlimefunMobDropEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player killer;
    private final LivingEntity entity;
    private final ItemStack drop;
    private final EntityDeathEvent underlyingEvent;

    private boolean cancelled;

    public SlimefunMobDropEvent(@Nonnull Player killer, @Nonnull LivingEntity entity, @Nonnull ItemStack drop, @Nonnull EntityDeathEvent underlyingEvent) {
        Validate.notNull(killer, "The killer must not be null");
        Validate.notNull(entity, "The killed entity must not be null");
        Validate.notNull(drop, "The drop must not be null");
        Validate.notNull(underlyingEvent, "The underlying EntityDeathEvent must not be null");

        this.killer = killer;
        this.entity = entity;
        this.drop = drop;
        this.underlyingEvent = underlyingEvent;
    }

    /**
     * This returns the {@link Player} who killed the entity.
     *
     * @return The killing {@link Player}
     */
    @Nonnull
    public Player getKiller() {
        return killer;
    }

    /**
     * This returns the {@link LivingEntity} that was killed.
     *
     * @return The killed {@link LivingEntity}
     */
    @Nonnull
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * This returns the custom {@link ItemStack} that is about to be dropped.
     *
     * @return The custom drop {@link ItemStack}
     */
    @Nonnull
    public ItemStack getDrop() {
        return drop;
    }

    /**
     * This returns the underlying {@link EntityDeathEvent}, giving access to
     * the full drop list and the dropped experience.
     *
     * @return The underlying {@link EntityDeathEvent}
     */
    @Nonnull
    public EntityDeathEvent getEntityDeathEvent() {
        return underlyingEvent;
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

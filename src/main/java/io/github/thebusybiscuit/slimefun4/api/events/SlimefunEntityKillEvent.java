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

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityKillHandler;

/**
 * This {@link Event} is fired whenever a {@link Player} kills a {@link LivingEntity}
 * while holding a {@link SlimefunItem}, right before the item's
 * {@link EntityKillHandler} is invoked.
 * <p>
 * Cancelling this event prevents the {@link EntityKillHandler} from running
 * (e.g. the Sword of Beheading would not drop a head). The underlying
 * {@link EntityDeathEvent} itself is not affected.
 *
 * @author Zurker
 *
 * @see SlimefunItemDamageEvent
 * @see EntityKillHandler
 */
public class SlimefunEntityKillEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player killer;
    private final LivingEntity entity;
    private final SlimefunItem slimefunItem;
    private final ItemStack item;
    private final EntityDeathEvent underlyingEvent;

    private boolean cancelled;

    public SlimefunEntityKillEvent(@Nonnull Player killer, @Nonnull LivingEntity entity, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack item, @Nonnull EntityDeathEvent underlyingEvent) {
        Validate.notNull(killer, "The killer must not be null");
        Validate.notNull(entity, "The killed entity must not be null");
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(underlyingEvent, "The underlying EntityDeathEvent must not be null");

        this.killer = killer;
        this.entity = entity;
        this.slimefunItem = slimefunItem;
        this.item = item;
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
     * This returns the {@link SlimefunItem} the entity was killed with.
     *
     * @return The {@link SlimefunItem} in the killer's hand
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the {@link ItemStack} the entity was killed with.
     *
     * @return The {@link ItemStack} in the killer's hand
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the underlying {@link EntityDeathEvent}, giving access to
     * the drops and the dropped experience.
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

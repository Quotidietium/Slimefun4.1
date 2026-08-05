package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SwordOfBeheading;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} kills a mob or player with a
 * {@link SwordOfBeheading} and the beheading roll succeeded: the head {@link ItemStack} is
 * about to be added to the drops.
 * <p>
 * Cancelling this event skips the head drop entirely. Addons may also replace the dropped
 * head via {@link #setHead(ItemStack)}.
 *
 * @author Zurker
 *
 * @see SwordOfBeheading
 */
public class SwordOfBeheadingDropEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final SwordOfBeheading sword;
    private final Entity entity;

    private ItemStack head;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public SwordOfBeheadingDropEvent(Player player, SwordOfBeheading sword, Entity entity, ItemStack head) {
        super(player);
        Validate.notNull(sword, "The SwordOfBeheading must not be null");
        Validate.notNull(entity, "The beheaded Entity must not be null");
        Validate.notNull(head, "The head ItemStack must not be null");

        this.sword = sword;
        this.entity = entity;
        this.head = head;
    }

    /**
     * This returns the {@link SwordOfBeheading} that beheaded the entity.
     *
     * @return The {@link SwordOfBeheading}
     */
    @Nonnull
    public SwordOfBeheading getSword() {
        return sword;
    }

    /**
     * This returns the {@link Entity} that was beheaded.
     *
     * @return The beheaded {@link Entity}
     */
    @Nonnull
    public Entity getEntity() {
        return entity;
    }

    /**
     * This returns the head {@link ItemStack} that is about to be dropped.
     *
     * @return The head to drop
     */
    @Nonnull
    public ItemStack getHead() {
        return head;
    }

    /**
     * This sets the head {@link ItemStack} that will be dropped.
     *
     * @param head
     *            The head to drop
     */
    public void setHead(@Nonnull ItemStack head) {
        Validate.notNull(head, "The head ItemStack must not be null");

        this.head = head;
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

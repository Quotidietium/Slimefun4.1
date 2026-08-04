package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.KnowledgeFlask;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} fills a {@link KnowledgeFlask}
 * with one of their experience levels, before the level is deducted.
 * <p>
 * Cancelling this event aborts the filling: the {@link Player} keeps their level, no filled
 * flask is produced and the empty flask is not consumed.
 *
 * @author Zurker
 *
 * @see KnowledgeFlask
 */
public class KnowledgeFlaskFillEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final KnowledgeFlask flask;
    private final ItemStack item;

    private boolean cancelled;

    public KnowledgeFlaskFillEvent(@Nonnull Player player, @Nonnull KnowledgeFlask flask, @Nonnull ItemStack item) {
        super(player);

        Validate.notNull(flask, "The KnowledgeFlask must not be null");
        Validate.notNull(item, "The flask item must not be null");

        this.flask = flask;
        this.item = item;
    }

    /**
     * This returns the {@link KnowledgeFlask} that is being filled.
     *
     * @return The {@link KnowledgeFlask}
     */
    @Nonnull
    public KnowledgeFlask getFlask() {
        return flask;
    }

    /**
     * This returns the held {@link ItemStack} of the {@link KnowledgeFlask} that is
     * about to be consumed.
     *
     * @return The held flask {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
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

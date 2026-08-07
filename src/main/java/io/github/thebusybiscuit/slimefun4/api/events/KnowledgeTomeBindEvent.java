package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.KnowledgeTome;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks an unbound
 * {@link KnowledgeTome}: the {@link Player}'s name and uuid are about to be written into
 * the tome, binding it to them.
 * <p>
 * Cancelling this event skips the binding entirely: the tome stays unbound and no sound
 * is played.
 *
 * @author Zurker
 *
 * @see KnowledgeTome
 * @see KnowledgeTomeShareEvent
 */
public class KnowledgeTomeBindEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final KnowledgeTome tome;
    private final ItemStack tomeItem;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public KnowledgeTomeBindEvent(Player player, KnowledgeTome tome, ItemStack tomeItem) {
        super(player);
        Validate.notNull(tome, "The KnowledgeTome must not be null");
        Validate.notNull(tomeItem, "The tome item must not be null");

        this.tome = tome;
        this.tomeItem = tomeItem;
    }

    /**
     * This returns the {@link KnowledgeTome} that is being bound.
     *
     * @return The {@link KnowledgeTome}
     */
    @Nonnull
    public KnowledgeTome getTome() {
        return tome;
    }

    /**
     * This returns the unbound {@link KnowledgeTome} {@link ItemStack} held by the {@link Player}.
     *
     * @return The tome {@link ItemStack}
     */
    @Nonnull
    public ItemStack getTomeItem() {
        return tomeItem;
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

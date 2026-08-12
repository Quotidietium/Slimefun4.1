package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.UUID;

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
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link KnowledgeTome}
 * that is bound to another player: the owner's researches are about to be copied to the
 * {@link Player} and the tome is about to be consumed.
 * <p>
 * Cancelling this event vetoes the sharing entirely: no researches are copied and the tome
 * is not consumed.
 * <p>
 * Addons may also redirect the share source via {@link #setOwner(UUID)}, e.g. to make a
 * special tome that always shares a template account's researches instead of the bound
 * owner's. The bound owner written in the tome's lore is never modified.
 *
 * @author Zurker
 *
 * @see KnowledgeTome
 */
public class KnowledgeTomeShareEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final KnowledgeTome tome;
    private final ItemStack tomeItem;

    private UUID owner;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public KnowledgeTomeShareEvent(Player player, KnowledgeTome tome, ItemStack tomeItem, UUID owner) {
        super(player);
        Validate.notNull(tome, "The KnowledgeTome must not be null");
        Validate.notNull(tomeItem, "The tome item must not be null");
        Validate.notNull(owner, "The owner UUID must not be null");

        this.tome = tome;
        this.tomeItem = tomeItem;
        this.owner = owner;
    }

    /**
     * This returns the {@link KnowledgeTome} that is being shared.
     *
     * @return The {@link KnowledgeTome}
     */
    @Nonnull
    public KnowledgeTome getTome() {
        return tome;
    }

    /**
     * This returns the bound {@link KnowledgeTome} {@link ItemStack} held by the {@link Player}.
     *
     * @return The tome {@link ItemStack}
     */
    @Nonnull
    public ItemStack getTomeItem() {
        return tomeItem;
    }

    /**
     * This returns the {@link UUID} of the {@link Player} the {@link KnowledgeTome} is bound to,
     * i.e. the owner whose researches are about to be copied.
     *
     * @return The owner's {@link UUID}
     */
    @Nonnull
    public UUID getOwner() {
        return owner;
    }

    /**
     * This sets the {@link UUID} whose researches will be copied to the {@link Player},
     * overriding the owner the tome is bound to. The tome's lore is never modified;
     * the redirect applies only to this single share.
     *
     * @param owner
     *            The {@link UUID} of the new research source, must not be null
     */
    public void setOwner(@Nonnull UUID owner) {
        Validate.notNull(owner, "The owner UUID must not be null");

        this.owner = owner;
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

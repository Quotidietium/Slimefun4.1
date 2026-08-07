package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.talismans.Talisman;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Talisman} has activated (after the
 * {@link TalismanActivateEvent} and after consumption) and its {@link PotionEffect}s are
 * about to be applied to the {@link Player}.
 * <p>
 * The list of effects is modifiable: an addon may add, remove or replace effects via
 * {@link #getEffects()} or {@link #setEffects(List)}. Cancelling this event skips the
 * potion application only - consumption, the triggering event's cancellation and the
 * activation message remain unaffected.
 *
 * @author Zurker
 *
 * @see Talisman
 * @see TalismanActivateEvent
 */
public class TalismanEffectApplyEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Talisman talisman;
    private final ItemStack talismanItem;
    private List<PotionEffect> effects;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public TalismanEffectApplyEvent(Player player, Talisman talisman, ItemStack talismanItem, List<PotionEffect> effects) {
        super(player);
        Validate.notNull(talisman, "The Talisman must not be null");
        Validate.notNull(talismanItem, "The talisman item must not be null");
        Validate.notNull(effects, "The effects list must not be null");

        this.talisman = talisman;
        this.talismanItem = talismanItem;
        this.effects = effects;
    }

    /**
     * This returns the {@link Talisman} that has activated.
     *
     * @return The {@link Talisman}
     */
    @Nonnull
    public Talisman getTalisman() {
        return talisman;
    }

    /**
     * This returns the {@link ItemStack} of the {@link Talisman} that was found in the
     * {@link Player}'s inventory or ender chest.
     *
     * @return The talisman {@link ItemStack}
     */
    @Nonnull
    public ItemStack getTalismanItem() {
        return talismanItem;
    }

    /**
     * This returns the live list of {@link PotionEffect}s that will be applied to the
     * {@link Player}. The list is modifiable - changes apply directly.
     *
     * @return The modifiable list of {@link PotionEffect}s
     */
    @Nonnull
    public List<PotionEffect> getEffects() {
        return effects;
    }

    /**
     * This replaces the {@link PotionEffect}s that will be applied to the {@link Player}.
     *
     * @param effects
     *            The new list of {@link PotionEffect}s, not {@code null}
     */
    public void setEffects(@Nonnull List<PotionEffect> effects) {
        Validate.notNull(effects, "The effects list must not be null");
        this.effects = effects;
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

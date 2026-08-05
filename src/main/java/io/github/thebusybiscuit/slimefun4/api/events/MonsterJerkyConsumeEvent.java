package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.food.MonsterJerky;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} consumes a {@link MonsterJerky}:
 * one tick later, any hunger effect is removed and the jerky's {@link PotionEffect}
 * (saturation by default) is applied.
 * <p>
 * Cancelling this event skips the whole consumption effect: no hunger removal and no
 * {@link PotionEffect}. Addons may also replace the applied effect via
 * {@link #setEffect(PotionEffect)}.
 *
 * @author Zurker
 *
 * @see MonsterJerky
 * @see DietCookieConsumeEvent
 */
public class MonsterJerkyConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final MonsterJerky jerky;

    private PotionEffect effect;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public MonsterJerkyConsumeEvent(Player player, MonsterJerky jerky, PotionEffect effect) {
        super(player);
        Validate.notNull(jerky, "The MonsterJerky must not be null");
        Validate.notNull(effect, "The PotionEffect must not be null");

        this.jerky = jerky;
        this.effect = effect;
    }

    /**
     * This returns the {@link MonsterJerky} that was consumed.
     *
     * @return The {@link MonsterJerky}
     */
    @Nonnull
    public MonsterJerky getJerky() {
        return jerky;
    }

    /**
     * This returns the {@link PotionEffect} that is about to be applied to the {@link Player}.
     *
     * @return The {@link PotionEffect} to apply
     */
    @Nonnull
    public PotionEffect getEffect() {
        return effect;
    }

    /**
     * This sets the {@link PotionEffect} that will be applied to the {@link Player}.
     *
     * @param effect
     *            The {@link PotionEffect} to apply
     */
    public void setEffect(@Nonnull PotionEffect effect) {
        Validate.notNull(effect, "The PotionEffect must not be null");

        this.effect = effect;
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

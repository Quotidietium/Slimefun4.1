package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.github.thebusybiscuit.slimefun4.implementation.items.food.Juice;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} drinks a {@link Juice}:
 * the juice's first saturation or absorption {@link PotionEffect} (if any) is about to be
 * applied and the empty glass bottle removed.
 * <p>
 * Cancelling this event skips the whole drink effect: no {@link PotionEffect} is applied and
 * no glass bottle is removed. Addons may replace the applied effect via
 * {@link #setEffect(PotionEffect)}, or pass {@code null} to apply no effect at all.
 * <p>
 * For juices without a saturation or absorption effect, {@link #getEffect()} returns
 * {@code null}; addons may still supply one via {@link #setEffect(PotionEffect)}.
 *
 * @author Zurker
 *
 * @see Juice
 * @see MonsterJerkyConsumeEvent
 */
public class JuiceDrinkEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Juice juice;
    private final ItemStack item;

    private PotionEffect effect;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public JuiceDrinkEvent(Player player, Juice juice, ItemStack item, @Nullable PotionEffect effect) {
        super(player);
        Validate.notNull(juice, "The Juice must not be null");
        Validate.notNull(item, "The consumed ItemStack must not be null");

        this.juice = juice;
        this.item = item;
        this.effect = effect;
    }

    /**
     * This returns the {@link Juice} that was drunk.
     *
     * @return The {@link Juice}
     */
    @Nonnull
    public Juice getJuice() {
        return juice;
    }

    /**
     * This returns the consumed {@link ItemStack}.
     *
     * @return The consumed {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the {@link PotionEffect} that is about to be applied to the
     * {@link Player}, i.e. the juice's first {@link PotionEffectType#SATURATION} or
     * {@link PotionEffectType#ABSORPTION} effect, or {@code null} if the juice has none.
     *
     * @return The {@link PotionEffect} to apply, or {@code null} for none
     */
    @Nullable
    public PotionEffect getEffect() {
        return effect;
    }

    /**
     * This sets the {@link PotionEffect} that will be applied to the {@link Player}.
     *
     * @param effect
     *            The {@link PotionEffect} to apply, or {@code null} to apply none
     */
    public void setEffect(@Nullable PotionEffect effect) {
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;

/**
 * This {@link Event} is fired on the main thread whenever a {@link SlimefunArmorPiece}
 * is about to apply its {@link PotionEffect PotionEffects} to the wearing {@link Player}.
 * It only fires for armor pieces that actually have effects configured.
 * <p>
 * Cancelling this event skips the application of this armor piece's effects
 * for this tick. The effects are re-evaluated on the next armor tick.
 * <p>
 * The effects can be replaced via {@link #setEffects(PotionEffect[])} before they
 * are applied, allowing addons to boost, reduce, add, or remove individual effects.
 *
 * @author Zurker
 *
 * @see SlimefunArmorPiece
 */
public class SlimefunArmorEffectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final SlimefunArmorPiece armorItem;
    private final ItemStack item;
    private final PotionEffect[] originalEffects;
    private PotionEffect[] effects;

    private boolean cancelled;

    public SlimefunArmorEffectEvent(@Nonnull Player player, @Nonnull SlimefunArmorPiece armorItem, @Nonnull ItemStack item, @Nonnull PotionEffect[] effects) {

        // Fired from the async ticker thread (machine/network tick) - report the actual context
        super(!Bukkit.isPrimaryThread());
        Validate.notNull(player, "The player must not be null");
        Validate.notNull(armorItem, "The armor item must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(effects, "The effects must not be null");

        this.player = player;
        this.armorItem = armorItem;
        this.item = item;
        this.originalEffects = effects;
        this.effects = effects;
    }

    /**
     * This returns the {@link Player} wearing the armor.
     *
     * @return The wearing {@link Player}
     */
    @Nonnull
    public Player getPlayer() {
        return player;
    }

    /**
     * This returns the {@link SlimefunArmorPiece} whose effects are about to be applied.
     *
     * @return The {@link SlimefunArmorPiece}
     */
    @Nonnull
    public SlimefunArmorPiece getArmorItem() {
        return armorItem;
    }

    /**
     * This returns the actual armor {@link ItemStack} worn by the {@link Player}.
     *
     * @return The worn {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the {@link PotionEffect PotionEffects} that are about to be applied.
     * If {@link #setEffects(PotionEffect[])} was called, returns the replacement.
     *
     * @return The effects to apply
     */
    @Nonnull
    public PotionEffect[] getEffects() {
        return effects;
    }

    /**
     * This returns the original {@link PotionEffect PotionEffects} from the armor piece
     * definition, before any listener modification.
     *
     * @return The original effects
     */
    @Nonnull
    public PotionEffect[] getOriginalEffects() {
        return originalEffects;
    }

    /**
     * This sets the {@link PotionEffect PotionEffects} that will be applied, overriding
     * the armor piece's default effects. An empty array is equivalent to cancelling
     * the event (no effects applied this tick).
     *
     * @param effects
     *            The replacement effects, must not be null
     */
    public void setEffects(@Nonnull PotionEffect[] effects) {
        Validate.notNull(effects, "The effects must not be null");
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

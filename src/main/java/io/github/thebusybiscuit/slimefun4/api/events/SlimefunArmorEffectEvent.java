package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
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
    private final PotionEffect[] effects;

    private boolean cancelled;

    public SlimefunArmorEffectEvent(@Nonnull Player player, @Nonnull SlimefunArmorPiece armorItem, @Nonnull ItemStack item, @Nonnull PotionEffect[] effects) {
        Validate.notNull(player, "The player must not be null");
        Validate.notNull(armorItem, "The armor item must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(effects, "The effects must not be null");

        this.player = player;
        this.armorItem = armorItem;
        this.item = item;
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
     *
     * @return The effects to apply
     */
    @Nonnull
    public PotionEffect[] getEffects() {
        return effects;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.RainbowArmorPiece;

/**
 * This {@link PlayerEvent} is fired whenever a worn {@link RainbowArmorPiece} is about
 * to cycle to the next {@link Color} of its color sequence.
 * <p>
 * The event fires once per worn armor piece per armor tick. The target {@link Color}
 * is mutable: {@link #setNewColor(Color)} lets addons override the color the piece
 * changes into. Cancelling this event skips the color change for this tick, so the
 * armor keeps its current {@link Color}.
 * <p>
 * Note that the color sequence itself is global to the armor task and advances
 * independently of this event, so a cancelled piece simply rejoins the sequence at
 * whatever color comes next.
 *
 * @author Zurker
 *
 * @see RainbowArmorPiece
 * @see RainbowBlockCycleEvent
 */
public class RainbowArmorCycleEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final RainbowArmorPiece armorPiece;
    private final ItemStack itemStack;
    private final Color previousColor;

    private Color newColor;
    private boolean cancelled;

    public RainbowArmorCycleEvent(@Nonnull Player player, @Nonnull RainbowArmorPiece armorPiece, @Nonnull ItemStack itemStack, @Nonnull Color previousColor, @Nonnull Color newColor) {
        super(player);
        Validate.notNull(armorPiece, "The RainbowArmorPiece must not be null");
        Validate.notNull(itemStack, "The ItemStack must not be null");
        Validate.notNull(previousColor, "The previous Color must not be null");
        Validate.notNull(newColor, "The new Color must not be null");

        this.armorPiece = armorPiece;
        this.itemStack = itemStack;
        this.previousColor = previousColor;
        this.newColor = newColor;
    }

    /**
     * This returns the {@link RainbowArmorPiece} that is about to change its color.
     *
     * @return The {@link RainbowArmorPiece}
     */
    @Nonnull
    public RainbowArmorPiece getArmorPiece() {
        return armorPiece;
    }

    /**
     * This returns the live armor {@link ItemStack} the {@link Player} is wearing.
     *
     * @return The worn {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * This returns the {@link Color} the armor piece currently has.
     *
     * @return The current {@link Color}
     */
    @Nonnull
    public Color getPreviousColor() {
        return previousColor;
    }

    /**
     * This returns the {@link Color} the armor piece is about to change into.
     *
     * @return The next {@link Color}
     */
    @Nonnull
    public Color getNewColor() {
        return newColor;
    }

    /**
     * Overrides the {@link Color} the armor piece is about to change into.
     *
     * @param newColor
     *            The {@link Color} to change into instead
     */
    public void setNewColor(@Nonnull Color newColor) {
        Validate.notNull(newColor, "The new Color must not be null");
        this.newColor = newColor;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.misc.StrangeNetherGoo;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a {@link Sheep}
 * with {@link StrangeNetherGoo}: the goo is about to be consumed and the sheep tainted
 * (poisoned, dyed purple and renamed).
 * <p>
 * Cancelling this event skips the taint entirely: no goo is consumed, the sheep stays
 * untouched and the underlying interaction is not consumed either (so shearing and other
 * vanilla interactions keep working).
 * <p>
 * Addons may also customize the taint's appearance via {@link #setTaintColor(DyeColor)}
 * and {@link #setTaintName(String)}, e.g. to mark sheep tainted by a shrine with a
 * different color. The poison effect is applied regardless, as is the consumed goo.
 *
 * @author Zurker
 *
 * @see StrangeNetherGoo
 */
public class StrangeNetherGooTaintEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final StrangeNetherGoo goo;
    private final Sheep sheep;

    private DyeColor taintColor = DyeColor.PURPLE;
    private String taintName = ChatColor.DARK_PURPLE + "Tainted Sheep";
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public StrangeNetherGooTaintEvent(Player player, StrangeNetherGoo goo, Sheep sheep) {
        super(player);
        Validate.notNull(goo, "The StrangeNetherGoo must not be null");
        Validate.notNull(sheep, "The Sheep must not be null");

        this.goo = goo;
        this.sheep = sheep;
    }

    /**
     * This returns the {@link StrangeNetherGoo} that is being used.
     *
     * @return The {@link StrangeNetherGoo}
     */
    @Nonnull
    public StrangeNetherGoo getGoo() {
        return goo;
    }

    /**
     * This returns the {@link Sheep} that is about to be tainted.
     *
     * @return The {@link Sheep}
     */
    @Nonnull
    public Sheep getSheep() {
        return sheep;
    }

    /**
     * This returns the {@link DyeColor} the {@link Sheep} will be dyed with. It defaults
     * to {@link DyeColor#PURPLE}.
     *
     * @return The taint {@link DyeColor}
     * @see #setTaintColor(DyeColor)
     */
    @Nonnull
    public DyeColor getTaintColor() {
        return taintColor;
    }

    /**
     * This sets the {@link DyeColor} the {@link Sheep} will be dyed with, replacing the
     * default {@link DyeColor#PURPLE}.
     *
     * @param taintColor
     *            The taint {@link DyeColor}, must not be null
     */
    public void setTaintColor(@Nonnull DyeColor taintColor) {
        Validate.notNull(taintColor, "The taint color must not be null");

        this.taintColor = taintColor;
    }

    /**
     * This returns the custom name the {@link Sheep} will receive. It defaults to the
     * classic dark purple "Tainted Sheep".
     *
     * @return The taint name
     * @see #setTaintName(String)
     */
    @Nonnull
    public String getTaintName() {
        return taintName;
    }

    /**
     * This sets the custom name the {@link Sheep} will receive, replacing the classic
     * dark purple "Tainted Sheep". The name is applied as-is, including any colors.
     *
     * @param taintName
     *            The taint name, must not be null
     */
    public void setTaintName(@Nonnull String taintName) {
        Validate.notNull(taintName, "The taint name must not be null");

        this.taintName = taintName;
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

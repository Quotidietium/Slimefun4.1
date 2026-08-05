package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.food.MeatJerky;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} consumes a {@link MeatJerky}:
 * the jerky's saturation is about to be added to the {@link Player}'s saturation level.
 * <p>
 * Cancelling this event skips the saturation gain. Addons may also adjust the granted
 * saturation via {@link #setSaturation(int)}.
 *
 * @author Zurker
 *
 * @see MeatJerky
 * @see MonsterJerkyConsumeEvent
 */
public class MeatJerkyConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final MeatJerky jerky;

    private int saturation;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public MeatJerkyConsumeEvent(Player player, MeatJerky jerky, int saturation) {
        super(player);
        Validate.notNull(jerky, "The MeatJerky must not be null");
        Validate.isTrue(saturation >= 0, "The saturation must not be negative");

        this.jerky = jerky;
        this.saturation = saturation;
    }

    /**
     * This returns the {@link MeatJerky} that was consumed.
     *
     * @return The {@link MeatJerky}
     */
    @Nonnull
    public MeatJerky getJerky() {
        return jerky;
    }

    /**
     * This returns the saturation that is about to be added to the {@link Player}'s
     * saturation level.
     *
     * @return The saturation to add
     */
    public int getSaturation() {
        return saturation;
    }

    /**
     * This sets the saturation that will be added to the {@link Player}'s saturation level.
     *
     * @param saturation
     *            The saturation to add
     */
    public void setSaturation(int saturation) {
        Validate.isTrue(saturation >= 0, "The saturation must not be negative");

        this.saturation = saturation;
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

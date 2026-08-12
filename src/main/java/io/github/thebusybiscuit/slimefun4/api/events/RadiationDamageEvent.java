package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.core.attributes.RadiationSymptom;

/**
 * The {@link RadiationDamageEvent} is called when a player takes radiation damage.
 * <p>
 * The exposure level the {@link RadiationSymptom RadiationSymptoms} are checked against
 * can be modified via {@link #setExposure(int)}: an addon may scale the effective level
 * down (e.g. for protective gear) or up (e.g. for a hardcore mode). This only affects
 * which symptoms are applied this tick; the {@link Player}'s stored exposure level is
 * left untouched.
 * <p>
 * Cancelling this event skips the symptoms entirely.
 *
 * @author HoosierTransfer
 */
public class RadiationDamageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private int exposure;
    private boolean cancelled;

    /**
     * This constructs a new {@link RadiationDamageEvent}.
     *
     * @param player The {@link Player} who took radiation damage
     * @param exposure The amount of radiation exposure
     */
    public RadiationDamageEvent(@Nonnull Player player, int exposure) {
        Validate.notNull(player, "The Player must not be null");
        Validate.isTrue(exposure >= 0, "The exposure must not be negative");

        this.player = player;
        this.exposure = exposure;
    }

    /**
     * This returns the {@link Player} who took radiation damage.
     *
     * @return The {@link Player} who took radiation damage
     */
    public @Nonnull Player getPlayer() {
        return player;
    }

    /**
     * This returns the amount of radiation exposure the
     * {@link RadiationSymptom RadiationSymptoms} are checked against.
     *
     * @return The amount of radiation exposure
     */
    public int getExposure() {
        return exposure;
    }

    /**
     * This sets the effective exposure level the {@link RadiationSymptom RadiationSymptoms}
     * are checked against this tick. The {@link Player}'s stored exposure level is not
     * changed by this.
     *
     * @param exposure
     *            The effective exposure level, must be at least 0
     */
    public void setExposure(int exposure) {
        Validate.isTrue(exposure >= 0, "The exposure must not be negative");

        this.exposure = exposure;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static @Nonnull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @Nonnull HandlerList getHandlers() {
        return getHandlerList();
    }
}

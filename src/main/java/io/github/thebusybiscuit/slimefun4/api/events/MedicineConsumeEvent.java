package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.medical.Medicine;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} consumes {@link Medicine}:
 * the medicine is about to extinguish the player, clear negative potion effects and
 * radiation exposure, and heal them.
 * <p>
 * Cancelling this event skips the curing entirely: no fire is extinguished, no effects or
 * radiation are cleared and no healing happens. This mirrors {@link VitaminsCureEvent} and
 * {@link BandageHealEvent}, which previously left {@link Medicine} as the only medical supply
 * without a corresponding event.
 *
 * @author Zurker
 *
 * @see Medicine
 * @see VitaminsCureEvent
 * @see BandageHealEvent
 */
public class MedicineConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Medicine medicine;
    private boolean cancelled;

    public MedicineConsumeEvent(@Nonnull Player player, @Nonnull Medicine medicine) {
        super(player);
        Validate.notNull(medicine, "The Medicine must not be null");

        this.medicine = medicine;
    }

    /**
     * This returns the {@link Medicine} that is being consumed.
     *
     * @return The {@link Medicine}
     */
    @Nonnull
    public Medicine getMedicine() {
        return medicine;
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

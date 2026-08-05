package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.api.events.ParachuteSlowFallEvent;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.Parachute;

/**
 * The {@link ParachuteTask} adds the entire functionality of the {@link Parachute}.
 * It continously sets the velocity of the {@link Player} to make them fall slowly.
 * Perhaps it can be changed to use the slow falling effect at some point.
 *
 * @author TheBusyBiscuit
 *
 * @see Parachute
 *
 */
public class ParachuteTask extends AbstractPlayerTask {

    /**
     * The {@link Parachute} this task was started for, if known. Used to populate
     * {@link ParachuteSlowFallEvent}; {@code null} for the legacy constructor.
     */
    @Nullable
    private final Parachute parachute;

    public ParachuteTask(@Nonnull Player p) {
        this(p, null);
    }

    /**
     * @param p
     *            The {@link Player} wearing the parachute
     * @param parachute
     *            The {@link Parachute} this task was started for
     */
    public ParachuteTask(@Nonnull Player p, @Nullable Parachute parachute) {
        super(p);
        this.parachute = parachute;
    }

    @Override
    protected void executeTask() {
        /*
         * Fire a ParachuteSlowFallEvent before applying the slow-fall velocity.
         * Cancellation skips this tick: no velocity is applied, but the task keeps
         * running. Gated on registered listeners to keep this per-tick path
         * allocation-free by default.
         */
        if (parachute != null && ParachuteSlowFallEvent.getHandlerList().getRegisteredListeners().length > 0) {
            ParachuteSlowFallEvent event = new ParachuteSlowFallEvent(p, parachute);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }
        }

        Vector vector = new Vector(0, 1, 0);
        vector.multiply(-0.1);
        p.setVelocity(vector);
        p.setFallDistance(0F);

        if (!p.isSneaking()) {
            cancel();
        }
    }

}

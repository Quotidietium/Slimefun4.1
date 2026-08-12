package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.api.events.JetpackThrustEvent;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.Jetpack;

public class JetpackTask extends AbstractPlayerTask {

    private static final float COST = 0.08F;

    private final Jetpack jetpack;

    public JetpackTask(@Nonnull Player p, @Nonnull Jetpack jetpack) {
        super(p);
        this.jetpack = jetpack;
    }

    @Override
    protected void executeTask() {
        if (p.getInventory().getChestplate() == null || p.getInventory().getChestplate().getType() == Material.AIR) {
            return;
        }

        /*
         * Fire a JetpackThrustEvent before consuming charge. Cancellation skips
         * this thrust: no charge is consumed and no velocity is applied, but the
         * task keeps running. Gated on registered listeners to keep this per-tick
         * path allocation-free by default.
         */
        float cost = COST;

        if (JetpackThrustEvent.getHandlerList().getRegisteredListeners().length > 0) {
            JetpackThrustEvent event = new JetpackThrustEvent(p, jetpack, cost);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            // An addon may have adjusted the charge cost of this thrust
            cost = event.getCost();
        }

        if (jetpack.removeItemCharge(p.getInventory().getChestplate(), cost)) {
            SoundEffect.JETPACK_THRUST_SOUND.playAt(p.getLocation(), SoundCategory.PLAYERS);
            p.getWorld().playEffect(p.getLocation(), Effect.SMOKE, 1, 1);
            p.setFallDistance(0F);
            Vector vector = new Vector(0, 1, 0);
            vector.multiply(jetpack.getThrust());
            vector.add(p.getEyeLocation().getDirection().multiply(0.2F));

            p.setVelocity(vector);
        } else {
            cancel();
        }
    }
}

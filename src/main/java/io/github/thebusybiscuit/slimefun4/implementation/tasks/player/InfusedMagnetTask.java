package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun4.api.events.ItemMagnetPullEvent;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.InfusedMagnet;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * This {@link AbstractPlayerTask} is run when a {@link Player} carries an {@link InfusedMagnet}.
 * It manages the automatic pickup of nearby items.
 *
 * @author TheBusyBiscuit
 *
 * @see InfusedMagnet
 *
 */
public class InfusedMagnetTask extends AbstractPlayerTask {

    /**
     * The radius in which an {@link Item} is picked up.
     */
    private final double radius;

    /**
     * The {@link InfusedMagnet} this task was started for, if known. Used to populate
     * {@link ItemMagnetPullEvent}; {@code null} for the legacy constructor.
     */
    @Nullable
    private final InfusedMagnet magnet;

    /**
     * This creates a new {@link InfusedMagnetTask} for the given {@link Player} with the given
     * pickup radius.
     *
     * @param p
     *            The {@link Player} who items should be teleported to
     * @param radius
     *            The radius in which items should be picked up
     */
    public InfusedMagnetTask(@Nonnull Player p, double radius) {
        this(p, null, radius);
    }

    /**
     * This creates a new {@link InfusedMagnetTask} for the given {@link Player} and
     * {@link InfusedMagnet} with the given pickup radius.
     *
     * @param p
     *            The {@link Player} who items should be teleported to
     * @param magnet
     *            The {@link InfusedMagnet} this task was started for
     * @param radius
     *            The radius in which items should be picked up
     */
    public InfusedMagnetTask(@Nonnull Player p, @Nullable InfusedMagnet magnet, double radius) {
        super(p);

        this.radius = radius;
        this.magnet = magnet;
    }

    @Override
    protected void executeTask() {
        boolean playSound = false;
        boolean listenersPresent = ItemMagnetPullEvent.getHandlerList().getRegisteredListeners().length > 0;

        for (Entity entity : p.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Item item && !SlimefunUtils.hasNoPickupFlag(item) && item.getPickupDelay() <= 0 && p.getLocation().distanceSquared(item.getLocation()) > 0.3) {
                Location destination;

                if (listenersPresent) {
                    destination = pullDestination(item);

                    if (destination == null) {
                        continue;
                    }
                } else {
                    destination = p.getLocation();
                }

                item.teleport(destination);
                playSound = true;
            }
        }

        // Only play a sound if an Item was found
        if (playSound) {
            SoundEffect.INFUSED_MAGNET_TELEPORT_SOUND.playFor(p);
        }
    }

    /**
     * Fires an {@link ItemMagnetPullEvent} for the given item and returns the destination
     * the item will be teleported to, or {@code null} when a listener cancelled the pull.
     * Only called when at least one listener is registered.
     */
    @Nullable
    private Location pullDestination(@Nonnull Item item) {
        ItemMagnetPullEvent event = new ItemMagnetPullEvent(p, magnet, item);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled() ? null : event.getDestination();
    }

    @Override
    protected boolean isValid() {
        return super.isValid() && p.getGameMode() != GameMode.SPECTATOR;
    }
}

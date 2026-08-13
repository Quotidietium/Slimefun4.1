package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.MagicEyeOfEnder;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a
 * {@link MagicEyeOfEnder} while wearing the full Ender Armor set: an {@link EnderPearl}
 * is about to be launched.
 * <p>
 * Cancelling this event skips the launch entirely; no {@link EnderPearl} is spawned.
 * <p>
 * Addons may also override the launch velocity via {@link #setVelocity(Vector)}, e.g. to
 * throw the {@link EnderPearl} harder or to lob it upwards. By default the velocity is
 * {@code null} and the launch uses the vanilla default (the {@link Player}'s eye
 * direction), exactly as before. Setting a velocity redirects the launch; setting it
 * back to {@code null} restores the vanilla default.
 *
 * @author Zurker
 *
 * @see MagicEyeOfEnder
 */
public class MagicEyeOfEnderLaunchEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final MagicEyeOfEnder magicEye;

    private Vector velocity;
    private boolean cancelled;

    public MagicEyeOfEnderLaunchEvent(@Nonnull Player player, @Nonnull MagicEyeOfEnder magicEye) {
        super(player);
        Validate.notNull(magicEye, "The MagicEyeOfEnder must not be null");

        this.magicEye = magicEye;
    }

    /**
     * This returns the {@link MagicEyeOfEnder} that is launching the {@link EnderPearl}.
     *
     * @return The {@link MagicEyeOfEnder}
     */
    @Nonnull
    public MagicEyeOfEnder getMagicEye() {
        return magicEye;
    }

    /**
     * This returns the velocity the {@link EnderPearl} will be launched with, or
     * {@code null} when the vanilla default launch (the {@link Player}'s eye direction)
     * is used.
     *
     * @return The launch velocity, or {@code null} for the vanilla default
     * @see #setVelocity(Vector)
     */
    @Nullable
    public Vector getVelocity() {
        return velocity;
    }

    /**
     * This overrides the velocity the {@link EnderPearl} is launched with, redirecting
     * the launch. Passing {@code null} restores the vanilla default launch.
     *
     * @param velocity
     *            The launch velocity, or {@code null} for the vanilla default
     */
    public void setVelocity(@Nullable Vector velocity) {
        if (velocity != null) {
            Validate.isTrue(Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ()), "The vector must have finite components, received: " + velocity);
        }

        this.velocity = velocity;
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

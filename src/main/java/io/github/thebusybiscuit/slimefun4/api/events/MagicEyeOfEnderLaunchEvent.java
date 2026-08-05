package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.MagicEyeOfEnder;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a
 * {@link MagicEyeOfEnder} while wearing the full Ender Armor set: an {@link EnderPearl}
 * is about to be launched.
 * <p>
 * Cancelling this event skips the launch entirely; no {@link EnderPearl} is spawned.
 *
 * @author Zurker
 *
 * @see MagicEyeOfEnder
 */
public class MagicEyeOfEnderLaunchEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final MagicEyeOfEnder magicEye;

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

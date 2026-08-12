package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.PickaxeOfTheSeeker;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a
 * {@link PickaxeOfTheSeeker} and a matching ore was found in range: the {@link Player}
 * is about to be rotated to face the located ore {@link Block}.
 * <p>
 * Cancelling this event skips the rotation only; the {@link PickaxeOfTheSeeker} is still
 * considered used and takes durability damage. This event is not fired when no ore is found.
 * <p>
 * Addons may also redirect the locate to a different ore {@link Block} via
 * {@link #setOre(Block)}, e.g. to make the pickaxe prefer a rarer ore over the closest one.
 *
 * @author Zurker
 *
 * @see PickaxeOfTheSeeker
 */
public class PickaxeOfTheSeekerLocateEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PickaxeOfTheSeeker pickaxe;

    private Block ore;
    private boolean cancelled;

    public PickaxeOfTheSeekerLocateEvent(@Nonnull Player player, @Nonnull PickaxeOfTheSeeker pickaxe, @Nonnull Block ore) {
        super(player);
        Validate.notNull(pickaxe, "The PickaxeOfTheSeeker must not be null");
        Validate.notNull(ore, "The located ore Block must not be null");

        this.pickaxe = pickaxe;
        this.ore = ore;
    }

    /**
     * This returns the {@link PickaxeOfTheSeeker} that located the ore.
     *
     * @return The {@link PickaxeOfTheSeeker}
     */
    @Nonnull
    public PickaxeOfTheSeeker getPickaxe() {
        return pickaxe;
    }

    /**
     * This returns the ore {@link Block} the {@link Player} is about to face.
     *
     * @return The located ore {@link Block}
     */
    @Nonnull
    public Block getOre() {
        return ore;
    }

    /**
     * This sets the ore {@link Block} the {@link Player} will face, e.g. to prefer a
     * rarer ore over the closest one the {@link PickaxeOfTheSeeker} located.
     *
     * @param ore
     *            The ore {@link Block} to face instead
     */
    public void setOre(@Nonnull Block ore) {
        Validate.notNull(ore, "The ore Block must not be null");

        this.ore = ore;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * This {@link PlayerEvent} is fired whenever a {@link io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.Smeltery}
 * has finished crafting and its fire is about to be consumed (the fire block removed),
 * after the {@link io.github.thebusybiscuit.slimefun4.implementation.items.blocks.IgnitionChamber}
 * has failed to renew it.
 * <p>
 * Cancelling this event keeps the fire burning: the fire block is not removed and the
 * Smeltery can continue operating without re-ignition. The event does not fire when the
 * IgnitionChamber successfully renews the fire (that path fires
 * {@link IgnitionChamberUseEvent}), nor when the fire-breaking chance does not trigger.
 * <p>
 * The event is fired synchronously from the crafting thread.
 *
 * @author Zurker
 *
 * @see IgnitionChamberUseEvent
 * @see MultiBlockCraftEvent
 */
public class SmelteryFireConsumeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Block smeltery;
    private final Block fire;

    private boolean cancelled;

    public SmelteryFireConsumeEvent(@Nonnull Player player, @Nonnull Block smeltery, @Nonnull Block fire) {
        super(player);
        Validate.notNull(smeltery, "The smeltery Block must not be null");
        Validate.notNull(fire, "The fire Block must not be null");

        this.smeltery = smeltery;
        this.fire = fire;
    }

    /**
     * This returns the {@link Block} of the Smeltery.
     *
     * @return The Smeltery {@link Block}
     */
    @Nonnull
    public Block getSmeltery() {
        return smeltery;
    }

    /**
     * This returns the fire {@link Block} that is about to be consumed.
     *
     * @return The fire {@link Block}
     */
    @Nonnull
    public Block getFire() {
        return fire;
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

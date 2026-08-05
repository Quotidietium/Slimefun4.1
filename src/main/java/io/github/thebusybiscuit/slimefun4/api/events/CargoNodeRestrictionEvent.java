package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoNode;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} tries to place a
 * {@link CargoNode} on the top or bottom face of a block (or against a non-air block)
 * and Slimefun is about to cancel the placement: cargo nodes may only be placed on the
 * sides of a block.
 * <p>
 * Cancelling this event vetoes the restriction: the placement is allowed to proceed and
 * no warning message is sent to the {@link Player}.
 *
 * @author Zurker
 *
 * @see CargoNode
 */
public class CargoNodeRestrictionEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final CargoNode cargoNode;
    private final Block block;
    private final Block blockAgainst;
    private final BlockPlaceEvent placeEvent;

    private boolean cancelled;

    public CargoNodeRestrictionEvent(@Nonnull Player player, @Nonnull CargoNode cargoNode, @Nonnull Block block, @Nonnull Block blockAgainst, @Nonnull BlockPlaceEvent placeEvent) {
        super(player);
        Validate.notNull(cargoNode, "The CargoNode must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(blockAgainst, "The blockAgainst must not be null");
        Validate.notNull(placeEvent, "The place event must not be null");

        this.cargoNode = cargoNode;
        this.block = block;
        this.blockAgainst = blockAgainst;
        this.placeEvent = placeEvent;
    }

    /**
     * This returns the {@link CargoNode} that is about to be placed.
     *
     * @return The {@link CargoNode}
     */
    @Nonnull
    public CargoNode getCargoNode() {
        return cargoNode;
    }

    /**
     * This returns the {@link Block} where the {@link CargoNode} is being placed.
     *
     * @return The placed {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link Block} the {@link CargoNode} is being placed against.
     *
     * @return The {@link Block} the node is placed against
     */
    @Nonnull
    public Block getBlockAgainst() {
        return blockAgainst;
    }

    /**
     * This returns the original {@link BlockPlaceEvent} for this placement.
     *
     * @return The {@link BlockPlaceEvent}
     */
    @Nonnull
    public BlockPlaceEvent getPlaceEvent() {
        return placeEvent;
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

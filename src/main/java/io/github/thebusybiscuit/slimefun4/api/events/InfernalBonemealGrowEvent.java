package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.InfernalBonemeal;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a not fully grown
 * Nether Wart with {@link InfernalBonemeal}: the Nether Wart is about to be grown to its
 * full age and the bone meal consumed.
 * <p>
 * Cancelling this event skips the growth entirely: the Nether Wart keeps its age and no
 * bone meal is consumed.
 *
 * @author Zurker
 *
 * @see InfernalBonemeal
 */
public class InfernalBonemealGrowEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final InfernalBonemeal bonemeal;
    private final Block block;

    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public InfernalBonemealGrowEvent(Player player, InfernalBonemeal bonemeal, Block block) {
        super(player);
        Validate.notNull(bonemeal, "The InfernalBonemeal must not be null");
        Validate.notNull(block, "The Nether Wart Block must not be null");

        this.bonemeal = bonemeal;
        this.block = block;
    }

    /**
     * This returns the {@link InfernalBonemeal} that is being used.
     *
     * @return The {@link InfernalBonemeal}
     */
    @Nonnull
    public InfernalBonemeal getBonemeal() {
        return bonemeal;
    }

    /**
     * This returns the Nether Wart {@link Block} that is about to be grown.
     *
     * @return The Nether Wart {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
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

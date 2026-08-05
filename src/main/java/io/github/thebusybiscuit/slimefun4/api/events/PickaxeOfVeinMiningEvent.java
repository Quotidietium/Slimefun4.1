package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.PickaxeOfVeinMining;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} mines an ore with a
 * {@link PickaxeOfVeinMining} and the connected ore vein is about to be mined as well.
 * <p>
 * Cancelling this event skips the vein mining entirely; the directly mined block is still
 * broken by vanilla. Addons may also remove entries from {@link #getBlocks()} to spare
 * individual ores.
 *
 * @author Zurker
 *
 * @see PickaxeOfVeinMining
 * @see LumberAxeTreeFellEvent
 */
public class PickaxeOfVeinMiningEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PickaxeOfVeinMining pickaxe;
    private final List<Block> blocks;

    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public PickaxeOfVeinMiningEvent(Player player, PickaxeOfVeinMining pickaxe, List<Block> blocks) {
        super(player);
        Validate.notNull(pickaxe, "The PickaxeOfVeinMining must not be null");
        Validate.notNull(blocks, "The vein blocks must not be null");

        this.pickaxe = pickaxe;
        this.blocks = blocks;
    }

    /**
     * This returns the {@link PickaxeOfVeinMining} that is mining the vein.
     *
     * @return The {@link PickaxeOfVeinMining}
     */
    @Nonnull
    public PickaxeOfVeinMining getPickaxe() {
        return pickaxe;
    }

    /**
     * This returns the live {@link List} of vein {@link Block Blocks} that will be broken,
     * including the directly mined block. Addons may remove entries to spare individual ores.
     *
     * @return The vein blocks about to be broken
     */
    @Nonnull
    public List<Block> getBlocks() {
        return blocks;
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

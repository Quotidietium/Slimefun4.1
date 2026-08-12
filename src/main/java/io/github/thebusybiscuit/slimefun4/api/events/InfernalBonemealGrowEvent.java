package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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
 * <p>
 * Addons may also adjust how far the Nether Wart grows via {@link #setTargetAge(int)},
 * e.g. to make the bonemeal grow it a single stage per use instead of to full maturity.
 *
 * @author Zurker
 *
 * @see InfernalBonemeal
 */
public class InfernalBonemealGrowEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final InfernalBonemeal bonemeal;
    private final Block block;
    private final int maximumAge;

    private int targetAge;
    private boolean cancelled;

    /**
     * This creates a new {@link InfernalBonemealGrowEvent}. The target age defaults to
     * the Nether Wart's maximum age, computed from the {@link Block}'s data.
     *
     * @param player
     *            The {@link Player} using the bonemeal
     * @param bonemeal
     *            The {@link InfernalBonemeal} being used
     * @param block
     *            The Nether Wart {@link Block} being grown
     */
    @ParametersAreNonnullByDefault
    public InfernalBonemealGrowEvent(Player player, InfernalBonemeal bonemeal, Block block) {
        this(player, bonemeal, block, maximumAgeOf(block));
    }

    /**
     * This creates a new {@link InfernalBonemealGrowEvent} with an explicit maximum age.
     * {@link InfernalBonemeal} itself uses this to hand in the maximum age of the block
     * data it already holds.
     *
     * @param player
     *            The {@link Player} using the bonemeal
     * @param bonemeal
     *            The {@link InfernalBonemeal} being used
     * @param block
     *            The Nether Wart {@link Block} being grown
     * @param maximumAge
     *            The Nether Wart's maximum age, must not be negative
     */
    @ParametersAreNonnullByDefault
    public InfernalBonemealGrowEvent(Player player, InfernalBonemeal bonemeal, Block block, int maximumAge) {
        super(player);
        Validate.notNull(bonemeal, "The InfernalBonemeal must not be null");
        Validate.notNull(block, "The Nether Wart Block must not be null");
        Validate.isTrue(maximumAge >= 0, "The maximum age must not be negative");

        this.bonemeal = bonemeal;
        this.block = block;
        this.maximumAge = maximumAge;
        this.targetAge = maximumAge;
    }

    /**
     * Computes the maximum age of the Nether Wart {@link Block}. Validates the
     * {@link Block} upfront so the delegating constructor keeps rejecting null blocks
     * with an {@link IllegalArgumentException}.
     */
    private static int maximumAgeOf(@Nonnull Block block) {
        Validate.notNull(block, "The Nether Wart Block must not be null");

        return ((Ageable) block.getBlockData()).getMaximumAge();
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

    /**
     * This returns the Nether Wart's maximum age.
     *
     * @return The maximum age
     */
    public int getMaximumAge() {
        return maximumAge;
    }

    /**
     * This returns the age the Nether Wart will be grown to. It defaults to the
     * {@link #getMaximumAge() maximum age}, i.e. full maturity.
     *
     * @return The target age
     * @see #setTargetAge(int)
     */
    public int getTargetAge() {
        return targetAge;
    }

    /**
     * This sets the age the Nether Wart will be grown to.
     *
     * @param targetAge
     *            The target age, between 0 and the {@link #getMaximumAge() maximum age}
     */
    public void setTargetAge(int targetAge) {
        Validate.isTrue(targetAge >= 0, "The target age must not be negative");
        Validate.isTrue(targetAge <= maximumAge, "The target age must not exceed the maximum age");

        this.targetAge = targetAge;
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

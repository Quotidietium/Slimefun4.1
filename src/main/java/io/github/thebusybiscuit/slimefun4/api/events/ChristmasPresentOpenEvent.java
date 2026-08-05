package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.seasonal.ChristmasPresent;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} places a {@link ChristmasPresent}
 * down on a {@link Block}: the present is about to be consumed, fireworks launched and the
 * rolled gift spawned on the clicked face.
 * <p>
 * Cancelling this event skips the opening entirely: no present is consumed, no fireworks are
 * launched and no gift is spawned. Addons may also replace the rolled gift via
 * {@link #setGift(ItemStack)}.
 *
 * @author Zurker
 *
 * @see ChristmasPresent
 * @see EasterEggOpenEvent
 */
public class ChristmasPresentOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ChristmasPresent present;
    private final Block clickedBlock;
    private final Block spawnBlock;

    private ItemStack gift;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public ChristmasPresentOpenEvent(Player player, ChristmasPresent present, Block clickedBlock, Block spawnBlock, ItemStack gift) {
        super(player);
        Validate.notNull(present, "The ChristmasPresent must not be null");
        Validate.notNull(clickedBlock, "The clicked Block must not be null");
        Validate.notNull(spawnBlock, "The spawn Block must not be null");
        Validate.notNull(gift, "The gift ItemStack must not be null");

        this.present = present;
        this.clickedBlock = clickedBlock;
        this.spawnBlock = spawnBlock;
        this.gift = gift;
    }

    /**
     * This returns the {@link ChristmasPresent} that is being opened.
     *
     * @return The {@link ChristmasPresent}
     */
    @Nonnull
    public ChristmasPresent getPresent() {
        return present;
    }

    /**
     * This returns the {@link Block} the {@link ChristmasPresent} was placed against.
     *
     * @return The clicked {@link Block}
     */
    @Nonnull
    public Block getClickedBlock() {
        return clickedBlock;
    }

    /**
     * This returns the {@link Block} at which the gift will be spawned, i.e. the
     * relative of the clicked {@link Block} on the clicked face.
     *
     * @return The spawn {@link Block}
     */
    @Nonnull
    public Block getSpawnBlock() {
        return spawnBlock;
    }

    /**
     * This returns the rolled gift {@link ItemStack} that is about to be spawned.
     *
     * @return The rolled gift
     */
    @Nonnull
    public ItemStack getGift() {
        return gift;
    }

    /**
     * This sets the gift {@link ItemStack} that will be spawned.
     *
     * @param gift
     *            The gift to spawn
     */
    public void setGift(@Nonnull ItemStack gift) {
        Validate.notNull(gift, "The gift ItemStack must not be null");

        this.gift = gift;
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

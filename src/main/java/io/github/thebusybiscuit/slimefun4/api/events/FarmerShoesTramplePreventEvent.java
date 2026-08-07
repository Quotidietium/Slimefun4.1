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

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.FarmerShoes;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} wearing {@link FarmerShoes}
 * steps on farmland: the shoes are about to prevent the trample, keeping the farmland
 * intact.
 * <p>
 * Cancelling this event vetoes the protection: the trample is not prevented and the
 * farmland is destroyed as it would be in vanilla.
 *
 * @author Zurker
 *
 * @see FarmerShoes
 * @see BeeStingProtectionEvent
 * @see SlimefunBootsFallEvent
 */
public class FarmerShoesTramplePreventEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final FarmerShoes boots;
    private final ItemStack bootsItem;
    private final Block farmland;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public FarmerShoesTramplePreventEvent(Player player, FarmerShoes boots, ItemStack bootsItem, Block farmland) {
        super(player);
        Validate.notNull(boots, "The FarmerShoes must not be null");
        Validate.notNull(bootsItem, "The boots item must not be null");
        Validate.notNull(farmland, "The farmland Block must not be null");

        this.boots = boots;
        this.bootsItem = bootsItem;
        this.farmland = farmland;
    }

    /**
     * This returns the {@link FarmerShoes} that are preventing the trample.
     *
     * @return The {@link FarmerShoes}
     */
    @Nonnull
    public FarmerShoes getBoots() {
        return boots;
    }

    /**
     * This returns the worn boots {@link ItemStack}.
     *
     * @return The boots {@link ItemStack}
     */
    @Nonnull
    public ItemStack getBootsItem() {
        return bootsItem;
    }

    /**
     * This returns the farmland {@link Block} being stepped on.
     *
     * @return The farmland {@link Block}
     */
    @Nonnull
    public Block getFarmland() {
        return farmland;
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

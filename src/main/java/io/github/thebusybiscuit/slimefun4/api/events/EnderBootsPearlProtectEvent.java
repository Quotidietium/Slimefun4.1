package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.armor.EnderBoots;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} wearing {@link EnderBoots}
 * takes damage from an {@link EnderPearl} impact: the boots are about to negate the
 * damage.
 * <p>
 * Cancelling this event vetoes the protection: the pearl damage applies as it would in
 * vanilla.
 *
 * @author Zurker
 *
 * @see EnderBoots
 * @see FarmerShoesTramplePreventEvent
 * @see BeeStingProtectionEvent
 */
public class EnderBootsPearlProtectEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EnderBoots boots;
    private final ItemStack bootsItem;
    private final EnderPearl pearl;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public EnderBootsPearlProtectEvent(Player player, EnderBoots boots, ItemStack bootsItem, EnderPearl pearl) {
        super(player);
        Validate.notNull(boots, "The EnderBoots must not be null");
        Validate.notNull(bootsItem, "The boots item must not be null");
        Validate.notNull(pearl, "The EnderPearl must not be null");

        this.boots = boots;
        this.bootsItem = bootsItem;
        this.pearl = pearl;
    }

    /**
     * This returns the {@link EnderBoots} that are negating the damage.
     *
     * @return The {@link EnderBoots}
     */
    @Nonnull
    public EnderBoots getBoots() {
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
     * This returns the {@link EnderPearl} whose impact damage is being negated.
     *
     * @return The {@link EnderPearl}
     */
    @Nonnull
    public EnderPearl getPearl() {
        return pearl;
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

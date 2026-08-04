package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.runes.VillagerRune;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} uses a {@link VillagerRune}
 * on a {@link Villager}, right before the {@link Villager Villager's} profession,
 * level and experience are reset.
 * <p>
 * Cancelling this event aborts the reset: the {@link Villager} keeps its profession
 * and the rune is not consumed.
 *
 * @author Zurker
 *
 * @see VillagerRune
 */
public class VillagerRuneResetEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Villager villager;
    private final ItemStack rune;

    private boolean cancelled;

    public VillagerRuneResetEvent(@Nonnull Player player, @Nonnull Villager villager, @Nonnull ItemStack rune) {
        super(player);

        Validate.notNull(villager, "The villager must not be null");
        Validate.notNull(rune, "The rune must not be null");

        this.villager = villager;
        this.rune = rune;
    }

    /**
     * This returns the {@link Villager} whose profession is about to be reset.
     *
     * @return The {@link Villager}
     */
    @Nonnull
    public Villager getVillager() {
        return villager;
    }

    /**
     * This returns the {@link VillagerRune} {@link ItemStack} held by the {@link Player}.
     *
     * @return The rune {@link ItemStack}
     */
    @Nonnull
    public ItemStack getRune() {
        return rune;
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

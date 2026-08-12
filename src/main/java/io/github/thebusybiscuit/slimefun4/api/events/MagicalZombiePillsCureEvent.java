package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.MagicalZombiePills;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} right-clicks a zombified
 * {@link Entity} (a zombie villager or a zombified piglin) with {@link MagicalZombiePills}:
 * the pill is about to be consumed and the entity cured.
 * <p>
 * Cancelling this event skips the cure entirely: no pill is consumed and the entity is
 * not converted.
 * <p>
 * For a {@link org.bukkit.entity.ZombieVillager} target the conversion time can be
 * adjusted via {@link #setConversionTime(int)} - it defaults to {@code 1} tick, the
 * historic near-instant cure; a higher value delays the conversion like a vanilla
 * cure would. The value is ignored for a {@link org.bukkit.entity.PigZombie} target,
 * which is always converted instantly.
 *
 * @author Zurker
 *
 * @see MagicalZombiePills
 */
public class MagicalZombiePillsCureEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final MagicalZombiePills pills;
    private final Entity entity;
    private final ItemStack item;

    private int conversionTime = 1;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public MagicalZombiePillsCureEvent(Player player, MagicalZombiePills pills, Entity entity, ItemStack item) {
        super(player);
        Validate.notNull(pills, "The MagicalZombiePills must not be null");
        Validate.notNull(entity, "The cured Entity must not be null");
        Validate.notNull(item, "The used ItemStack must not be null");

        this.pills = pills;
        this.entity = entity;
        this.item = item;
    }

    /**
     * This returns the {@link MagicalZombiePills} that are being used.
     *
     * @return The {@link MagicalZombiePills}
     */
    @Nonnull
    public MagicalZombiePills getPills() {
        return pills;
    }

    /**
     * This returns the zombified {@link Entity} that is about to be cured.
     *
     * @return The cured {@link Entity}
     */
    @Nonnull
    public Entity getEntity() {
        return entity;
    }

    /**
     * This returns the held {@link ItemStack} of pills that is about to be consumed.
     *
     * @return The used {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItem() {
        return item;
    }

    /**
     * This returns the conversion time (in ticks) a cured
     * {@link org.bukkit.entity.ZombieVillager} will take to turn back into a villager.
     * Defaults to {@code 1} tick, the historic near-instant cure. Ignored for a
     * {@link org.bukkit.entity.PigZombie} target.
     *
     * @return The conversion time in ticks
     */
    public int getConversionTime() {
        return conversionTime;
    }

    /**
     * This sets the conversion time (in ticks) a cured
     * {@link org.bukkit.entity.ZombieVillager} will take to turn back into a villager.
     *
     * @param conversionTime
     *            The conversion time in ticks, must be at least 1
     */
    public void setConversionTime(int conversionTime) {
        Validate.isTrue(conversionTime >= 1, "The conversion time must be at least 1");

        this.conversionTime = conversionTime;
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

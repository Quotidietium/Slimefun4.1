package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.SlimefunWeapon;

/**
 * This {@link Event} is fired whenever a {@link Player} is about to deal melee damage to an
 * {@link Entity} while holding a {@link SlimefunItem}.
 *
 * <p>
 * It is raised inside Slimefun's damage listener, right before the item's
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler} is invoked, which
 * makes it the ideal place to <b>modify or veto</b> the damage of a Slimefun weapon:
 * </p>
 * <ul>
 * <li>{@link #setDamage(double)} scales the raw damage (writes through to the underlying
 * {@link EntityDamageByEntityEvent}).</li>
 * <li>{@link #setCancelled(boolean)} cancels the hit entirely - the damage is prevented and the
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler} is <b>not</b> called.</li>
 * </ul>
 *
 * <p>
 * The event wraps the original {@link EntityDamageByEntityEvent} (available via
 * {@link #getEntityDamageEvent()}) so advanced add-ons can inspect damage modifiers directly.
 * Cancellation and damage are delegated to that underlying event, i.e. cancelling this event
 * also cancels the Bukkit one.
 * </p>
 *
 * <p>
 * Note: this only covers <b>melee</b> damage. Bow hits use
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler} and the launch is
 * covered by {@link SlimefunBowShootEvent}.
 * </p>
 *
 * @author Zurker
 *
 * @see SlimefunWeapon
 * @see io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler
 *
 */
public class SlimefunItemDamageEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player damager;
    private final Entity victim;
    private final SlimefunItem slimefunItem;
    private final ItemStack weapon;
    private final EntityDamageByEntityEvent underlyingEvent;

    public SlimefunItemDamageEvent(@Nonnull Player damager, @Nonnull Entity victim, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack weapon, @Nonnull EntityDamageByEntityEvent underlyingEvent) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(damager, "The damager cannot be null");
        Validate.notNull(victim, "The victim cannot be null");
        Validate.notNull(slimefunItem, "The SlimefunItem cannot be null");
        Validate.notNull(weapon, "The weapon cannot be null");
        Validate.notNull(underlyingEvent, "The underlying EntityDamageByEntityEvent cannot be null");

        this.damager = damager;
        this.victim = victim;
        this.slimefunItem = slimefunItem;
        this.weapon = weapon;
        this.underlyingEvent = underlyingEvent;
    }

    /**
     * The {@link Player} dealing the damage.
     *
     * @return The attacking {@link Player}
     */
    @Nonnull
    public Player getDamager() {
        return damager;
    }

    /**
     * The {@link Entity} receiving the damage.
     *
     * @return The damaged {@link Entity}
     */
    @Nonnull
    public Entity getVictim() {
        return victim;
    }

    /**
     * The {@link SlimefunItem} that was used as a weapon.
     *
     * @return The {@link SlimefunItem} held by the attacker
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * Whether the used {@link SlimefunItem} is tagged as a {@link SlimefunWeapon}.
     *
     * @return {@code true} if the item implements {@link SlimefunWeapon}
     */
    public boolean isWeapon() {
        return slimefunItem instanceof SlimefunWeapon;
    }

    /**
     * The actual {@link ItemStack} used to attack.
     *
     * @return The {@link ItemStack} in the attacker's main hand
     */
    @Nonnull
    public ItemStack getWeapon() {
        return weapon;
    }

    /**
     * The underlying Bukkit {@link EntityDamageByEntityEvent}.
     *
     * @return The wrapped {@link EntityDamageByEntityEvent}
     */
    @Nonnull
    public EntityDamageByEntityEvent getEntityDamageEvent() {
        return underlyingEvent;
    }

    /**
     * The (modifiable) amount of damage that will be dealt.
     *
     * @return The current damage value
     *
     * @see EntityDamageByEntityEvent#getDamage()
     */
    public double getDamage() {
        return underlyingEvent.getDamage();
    }

    /**
     * Sets the amount of damage that will be dealt. This writes through to the underlying
     * {@link EntityDamageByEntityEvent}.
     *
     * @param damage
     *            The new damage amount (must be {@code >= 0})
     */
    public void setDamage(double damage) {
        Validate.isTrue(damage >= 0, "Damage must be zero or greater!");
        underlyingEvent.setDamage(damage);
    }

    @Override
    public boolean isCancelled() {
        return underlyingEvent.isCancelled();
    }

    @Override
    public void setCancelled(boolean cancel) {
        underlyingEvent.setCancelled(cancel);
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

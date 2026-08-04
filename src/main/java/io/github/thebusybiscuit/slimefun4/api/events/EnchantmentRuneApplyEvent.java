package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.magical.runes.EnchantmentRune;

/**
 * This {@link PlayerEvent} is fired whenever an {@link EnchantmentRune} has found a
 * compatible dropped {@link Item} and is about to start its ritual: the chosen
 * {@link Enchantment} will be applied to the {@link ItemStack} after a short delay.
 * <p>
 * Cancelling this event aborts the ritual: both the rune and the target {@link Item}
 * remain on the ground, untouched. Listeners may also replace the randomly chosen
 * {@link Enchantment} or its level before it is applied.
 *
 * @author Zurker
 *
 * @see EnchantmentRune
 */
public class EnchantmentRuneApplyEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Item rune;
    private final Item item;
    private final ItemStack itemStack;

    private Enchantment enchantment;
    private int level;
    private boolean cancelled;

    public EnchantmentRuneApplyEvent(@Nonnull Player player, @Nonnull Item rune, @Nonnull Item item, @Nonnull ItemStack itemStack, @Nonnull Enchantment enchantment, int level) {
        super(player);

        Validate.notNull(rune, "The rune must not be null");
        Validate.notNull(item, "The item must not be null");
        Validate.notNull(itemStack, "The ItemStack must not be null");
        Validate.notNull(enchantment, "The enchantment must not be null");
        Validate.isTrue(level >= 1, "The level must be at least 1");

        this.rune = rune;
        this.item = item;
        this.itemStack = itemStack;
        this.enchantment = enchantment;
        this.level = level;
    }

    /**
     * This returns the dropped {@link EnchantmentRune} {@link Item} entity.
     *
     * @return The rune {@link Item} entity
     */
    @Nonnull
    public Item getRune() {
        return rune;
    }

    /**
     * This returns the dropped {@link Item} entity that is about to be enchanted.
     *
     * @return The target {@link Item} entity
     */
    @Nonnull
    public Item getItem() {
        return item;
    }

    /**
     * This returns the {@link ItemStack} that is about to be enchanted.
     *
     * @return The target {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * This returns the randomly chosen {@link Enchantment} that will be applied.
     *
     * @return The {@link Enchantment} to apply
     */
    @Nonnull
    public Enchantment getEnchantment() {
        return enchantment;
    }

    /**
     * This replaces the randomly chosen {@link Enchantment} with a different one.
     * The replacement must be applicable to the {@link ItemStack}, as required by
     * {@link ItemStack#addEnchantment(Enchantment, int)}.
     *
     * @param enchantment
     *            The {@link Enchantment} to apply instead
     */
    public void setEnchantment(@Nonnull Enchantment enchantment) {
        Validate.notNull(enchantment, "The enchantment must not be null");
        this.enchantment = enchantment;
    }

    /**
     * This returns the level of the {@link Enchantment} that will be applied.
     *
     * @return The enchantment level
     */
    public int getLevel() {
        return level;
    }

    /**
     * This replaces the randomly chosen enchantment level. The level must satisfy
     * the contract of {@link ItemStack#addEnchantment(Enchantment, int)} for the
     * current {@link Enchantment}.
     *
     * @param level
     *            The level to apply instead, at least 1
     */
    public void setLevel(int level) {
        Validate.isTrue(level >= 1, "The level must be at least 1");
        this.level = level;
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

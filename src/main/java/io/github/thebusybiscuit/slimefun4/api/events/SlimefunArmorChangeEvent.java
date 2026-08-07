package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.HashedArmorpiece;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.SlimefunArmorTask;

/**
 * This {@link PlayerEvent} is fired whenever the {@link SlimefunArmorTask} detects
 * that the content of one of a {@link Player}'s armor slots has changed and either
 * the old or the new content is a {@link SlimefunArmorPiece}: equipping, unequipping
 * or swapping a piece of Slimefun armor.
 * <p>
 * The change is detected by comparing the armor slot against the cached
 * {@link HashedArmorpiece} of the {@link PlayerProfile} (durability changes are
 * deliberately ignored by that comparison), so this event is a notification only:
 * the inventory already holds the new state and the event cannot be cancelled.
 * It is not fired when neither the old nor the new content is a
 * {@link SlimefunArmorPiece} (a plain vanilla armor swap), nor for the join-time
 * cache synchronization.
 * <p>
 * Note that the {@link SlimefunArmorTask} runs on an asynchronous thread, so this
 * event is asynchronous in production. The previous {@link ItemStack} is not
 * retained by the cache; only its {@link SlimefunArmorPiece} identity is available.
 * The per-tick potion effects of a worn piece are governed separately by the
 * {@link SlimefunArmorEffectEvent}.
 *
 * @author Zurker
 *
 * @see SlimefunArmorEffectEvent
 * @see SlimefunArmorTask
 * @see SlimefunArmorPiece
 * @see HashedArmorpiece
 */
public class SlimefunArmorChangeEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final int slot;
    private final SlimefunArmorPiece previousArmor;
    private final ItemStack newItem;
    private final SlimefunArmorPiece newArmor;

    public SlimefunArmorChangeEvent(@Nonnull Player player, int slot, @Nullable SlimefunArmorPiece previousArmor, @Nullable ItemStack newItem, @Nullable SlimefunArmorPiece newArmor) {
        super(player, !Bukkit.isPrimaryThread());

        Validate.notNull(player, "The Player must not be null");
        Validate.isTrue(slot >= 0 && slot < 4, "The armor slot must be between 0 and 3");
        Validate.isTrue(previousArmor != null || newArmor != null, "At least one side of the change must be a SlimefunArmorPiece");

        this.slot = slot;
        this.previousArmor = previousArmor;
        this.newItem = newItem;
        this.newArmor = newArmor;
    }

    /**
     * This returns the index of the armor slot that changed, as used by
     * {@code PlayerInventory#getArmorContents()} and {@link PlayerProfile#getArmor()}.
     *
     * @return The armor slot index, between 0 and 3
     */
    public int getSlot() {
        return slot;
    }

    /**
     * This returns the {@link SlimefunArmorPiece} that was previously worn in this
     * slot, or null if the previous content was empty or not Slimefun armor.
     * The previous {@link ItemStack} itself is not retained by the armor cache.
     *
     * @return The previously worn {@link SlimefunArmorPiece}, if any
     */
    @Nullable
    public SlimefunArmorPiece getPreviousArmor() {
        return previousArmor;
    }

    /**
     * This returns the {@link ItemStack} now occupying the slot, a live stack from
     * the {@link Player}'s inventory. It may be null or air when the slot was
     * emptied.
     *
     * @return The new content of the armor slot
     */
    @Nullable
    public ItemStack getNewItem() {
        return newItem;
    }

    /**
     * This returns the {@link SlimefunArmorPiece} that is now worn in this slot,
     * or null if the new content is empty or not Slimefun armor.
     *
     * @return The newly worn {@link SlimefunArmorPiece}, if any
     */
    @Nullable
    public SlimefunArmorPiece getNewArmor() {
        return newArmor;
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

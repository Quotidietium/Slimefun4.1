package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.FishermanAndroid;

/**
 * This {@link Event} is fired before a {@link FishermanAndroid} pushes its caught
 * fish into its inventory. If this {@link Event} is cancelled, the catch is lost
 * and nothing is inserted.
 * <p>
 * The caught item can be replaced via {@link #setDrop(ItemStack)}, allowing addons
 * to implement custom fishing loot tables.
 *
 * @author Zurker
 *
 * @see AndroidMineEvent
 * @see AndroidFarmEvent
 */
public class AndroidFishEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AndroidInstance android;
    private ItemStack drop;
    private boolean cancelled;

    /**
     * @param android
     *            The {@link AndroidInstance} that triggered this {@link Event}
     * @param drop
     *            The {@link ItemStack} the android caught
     */
    @ParametersAreNonnullByDefault
    public AndroidFishEvent(AndroidInstance android, ItemStack drop) {
        this.android = android;
        this.drop = drop;
    }

    /**
     * This method returns the {@link AndroidInstance} who
     * triggered this {@link Event}.
     *
     * @return the involved {@link AndroidInstance}
     */
    @Nonnull
    public AndroidInstance getAndroid() {
        return android;
    }

    /**
     * This method returns the {@link ItemStack} the android caught.
     *
     * @return the caught {@link ItemStack}
     */
    @Nonnull
    public ItemStack getDrop() {
        return drop;
    }

    /**
     * This method replaces the {@link ItemStack} the android caught.
     *
     * @param drop
     *            The new catch, must not be null
     */
    public void setDrop(@Nonnull ItemStack drop) {
        Validate.notNull(drop, "The drop must not be null");
        this.drop = drop;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
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

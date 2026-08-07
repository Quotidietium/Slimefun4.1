package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This {@link PlayerEvent} is fired whenever a {@link SlimefunItem} is about to be
 * blocked from being used in a vanilla workstation, such as an anvil, a brewing stand
 * or a crafting table.
 * <p>
 * The event fires in two situations: when the result is about to be denied (taking the
 * result out, clicking the item in, dragging it across or discoloring it in a cauldron)
 * and when the result preview is about to be hidden (crafting and smithing tables).
 * Cancelling this event allows the {@link SlimefunItem} to be used instead; addons that
 * want to fully allow an item should cancel it at both the preview and the result stage.
 * <p>
 * Fully automated paths without a {@link Player} context (hoppers, the vanilla crafter)
 * do not fire this event; the vanilla crafter fires {@link SlimefunItemCrafterPreventEvent}
 * instead.
 *
 * @author Zurker
 *
 * @see SlimefunItem
 * @see SlimefunItemCrafterPreventEvent
 */
public class SlimefunItemWorkstationEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The vanilla workstation a {@link SlimefunItem} is about to be blocked from.
     */
    public enum Workstation {

        ANVIL,
        BREWING_STAND,
        CARTOGRAPHY_TABLE,
        CAULDRON,
        CRAFTING_TABLE,
        GRINDSTONE,
        SMITHING_TABLE

    }

    private final SlimefunItem slimefunItem;
    private final ItemStack itemStack;
    private final Workstation workstation;

    private boolean cancelled;

    public SlimefunItemWorkstationEvent(@Nonnull Player player, @Nonnull SlimefunItem slimefunItem, @Nonnull ItemStack itemStack, @Nonnull Workstation workstation) {
        super(player);
        Validate.notNull(slimefunItem, "The SlimefunItem must not be null");
        Validate.notNull(itemStack, "The ItemStack must not be null");
        Validate.notNull(workstation, "The Workstation must not be null");

        this.slimefunItem = slimefunItem;
        this.itemStack = itemStack;
        this.workstation = workstation;
    }

    /**
     * This returns the {@link SlimefunItem} that is about to be blocked.
     *
     * @return The {@link SlimefunItem}
     */
    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return slimefunItem;
    }

    /**
     * This returns the actual {@link ItemStack} the {@link Player} tried to use.
     *
     * @return The used {@link ItemStack}
     */
    @Nonnull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * This returns the {@link Workstation} the {@link SlimefunItem} is about to be
     * blocked from.
     *
     * @return The {@link Workstation}
     */
    @Nonnull
    public Workstation getWorkstation() {
        return workstation;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} dies while carrying
 * {@link Soulbound} items, right before those items are stored away (and removed
 * from the death drops) for their return on respawn.
 * <p>
 * Cancelling this event disables the soulbound behavior for this death entirely:
 * every item, including the {@link Soulbound} ones, drops normally and nothing
 * is returned on respawn.
 * <p>
 * Addons may also spare individual items from being kept via
 * {@link #excludeWhen(Predicate)}: an excluded {@link Soulbound} item drops
 * normally with this death - it is neither stored nor returned on respawn.
 * Without any exclusion predicate every {@link Soulbound} item is kept, which
 * is the historic behavior.
 *
 * @author Zurker
 *
 * @see SoulboundItemsReturnEvent
 * @see Soulbound
 */
public class SoulboundItemsKeepEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerDeathEvent deathEvent;
    private final List<Predicate<ItemStack>> exclusions = new ArrayList<>();

    private boolean cancelled;

    public SoulboundItemsKeepEvent(@Nonnull Player player, @Nonnull PlayerDeathEvent deathEvent) {
        super(player);

        Validate.notNull(deathEvent, "The death event must not be null");
        this.deathEvent = deathEvent;
    }

    /**
     * This returns the underlying {@link PlayerDeathEvent}, giving access to
     * the drops this event's cancellation will leave untouched.
     *
     * @return The underlying {@link PlayerDeathEvent}
     */
    @Nonnull
    public PlayerDeathEvent getDeathEvent() {
        return deathEvent;
    }

    /**
     * This excludes any {@link Soulbound} {@link ItemStack} matching the given
     * {@link Predicate} from being kept: it drops normally with this death and
     * is not returned on respawn. Multiple predicates can be registered; an item
     * matching any of them is excluded.
     *
     * @param predicate
     *            The {@link Predicate} matching the items to let drop
     */
    public void excludeWhen(@Nonnull Predicate<ItemStack> predicate) {
        Validate.notNull(predicate, "The predicate must not be null");

        exclusions.add(predicate);
    }

    /**
     * This returns whether the given {@link ItemStack} was excluded from being
     * kept via {@link #excludeWhen(Predicate)}. Without any registered predicate
     * this always returns {@code false}, preserving the historic behavior.
     *
     * @param item
     *            The {@link ItemStack} to check
     *
     * @return Whether the item drops normally instead of being kept
     */
    public boolean isExcluded(@Nullable ItemStack item) {
        if (item == null || exclusions.isEmpty()) {
            return false;
        }

        for (Predicate<ItemStack> predicate : exclusions) {
            if (predicate.test(item)) {
                return true;
            }
        }

        return false;
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

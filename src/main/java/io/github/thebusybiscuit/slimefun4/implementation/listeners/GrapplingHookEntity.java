package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

final class GrapplingHookEntity {

    private final boolean dropItem;
    private final boolean wasConsumed;
    private final Arrow arrow;
    private final Entity leashTarget;

    private boolean handled = false;

    @ParametersAreNonnullByDefault
    GrapplingHookEntity(Player p, Arrow arrow, Entity leashTarget, boolean dropItem, boolean wasConsumed) {
        this.arrow = arrow;
        this.wasConsumed = wasConsumed;
        this.leashTarget = leashTarget;
        this.dropItem = p.getGameMode() != GameMode.CREATIVE && dropItem;
    }

    @Nonnull
    public Arrow getArrow() {
        return arrow;
    }

    /**
     * Atomically claims this grappling hook so its landing is processed exactly once.
     * <p>
     * A single hook arrow can raise several Bukkit events when it lands - for example an
     * arrow that strikes an entity typically fires both {@code EntityDamageByEntityEvent}
     * and {@code ProjectileHitEvent}, and one that clips a painting or item frame also
     * raises {@code HangingBreakByEntityEvent}. Without a guard, every one of those
     * handlers would re-enter the landing logic and re-drop the hook item (a duplication).
     * The current reliance on {@code Arrow#isValid()} turning false after {@link #remove()}
     * is timing-sensitive and Bukkit-version dependent, so this compare-and-set makes the
     * "land exactly once" guarantee explicit and robust under high-frequency use.
     *
     * @return {@code true} if this caller is the first to handle the landing, {@code false}
     *         for every subsequent caller
     */
    public boolean markHandled() {
        if (handled) {
            return false;
        }

        handled = true;
        return true;
    }

    public void drop(@Nonnull Location l) {
        // If a grappling hook was consumed, drop one grappling hook on the floor
        if (dropItem && wasConsumed) {
            Item item = l.getWorld().dropItem(l, SlimefunItems.GRAPPLING_HOOK.item());
            item.setPickupDelay(16);
        }
    }

    public void remove() {
        if (arrow.isValid()) {
            arrow.remove();
        }

        if (leashTarget.isValid()) {
            leashTarget.remove();
        }
    }

}
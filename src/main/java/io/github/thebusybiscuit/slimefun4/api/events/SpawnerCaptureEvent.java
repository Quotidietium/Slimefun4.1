package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.PickaxeOfContainment;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} breaks a spawner with a
 * {@link PickaxeOfContainment}: the spawner's {@link EntityType} has been read and the
 * resulting {@link ItemStack} (a broken or repaired spawner) is about to be spawned as a drop.
 * <p>
 * The drop {@link ItemStack} is modifiable via {@link #setDrop(ItemStack)} - an addon may
 * replace the captured spawner with a custom item. Cancelling this event skips the capture
 * entirely: nothing is spawned and vanilla drop behaviour (drops and experience) is preserved.
 *
 * @author Zurker
 *
 * @see PickaxeOfContainment
 * @see GoldPanUseEvent
 */
public class SpawnerCaptureEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final PickaxeOfContainment pickaxe;
    private final Block block;
    private final EntityType entityType;
    private ItemStack drop;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public SpawnerCaptureEvent(Player player, PickaxeOfContainment pickaxe, Block block, @Nullable EntityType entityType, ItemStack drop) {
        super(player);
        Validate.notNull(pickaxe, "The PickaxeOfContainment must not be null");
        Validate.notNull(block, "The spawner block must not be null");
        Validate.notNull(drop, "The drop must not be null");

        this.pickaxe = pickaxe;
        this.block = block;
        this.entityType = entityType;
        this.drop = drop;
    }

    /**
     * This returns the {@link PickaxeOfContainment} that was used.
     *
     * @return The {@link PickaxeOfContainment}
     */
    @Nonnull
    public PickaxeOfContainment getPickaxe() {
        return pickaxe;
    }

    /**
     * This returns the spawner {@link Block} that is being broken.
     *
     * @return The spawner {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link EntityType} the broken spawner was spawning, or {@code null} if
     * the block state could not be read as a creature spawner.
     *
     * @return The spawned {@link EntityType}, or null
     */
    @Nullable
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * This returns the {@link ItemStack} that is about to be spawned as the capture result.
     * Use {@link #setDrop(ItemStack)} to replace it.
     *
     * @return The drop {@link ItemStack}
     */
    @Nonnull
    public ItemStack getDrop() {
        return drop;
    }

    /**
     * This replaces the {@link ItemStack} that will be spawned as the capture result.
     *
     * @param drop
     *            The new drop, not {@code null}
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

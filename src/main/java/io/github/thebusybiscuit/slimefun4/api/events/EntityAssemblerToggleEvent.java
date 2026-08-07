package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.AbstractEntityAssembler;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} clicks the toggle
 * button of an {@link AbstractEntityAssembler} (e.g. the iron golem or wither
 * assembler) to enable or disable it.
 * <p>
 * Cancelling this event vetoes the toggle: the assembler keeps its current state
 * and the menu is not redrawn. Individual assemblies are governed by
 * {@link AsyncEntityAssembleEvent}, which fires only while the assembler is
 * enabled.
 *
 * @author Zurker
 *
 * @see AsyncEntityAssembleEvent
 * @see AbstractEntityAssembler
 */
public class EntityAssemblerToggleEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final AbstractEntityAssembler<?> assembler;
    private final Block block;
    private final boolean enabling;

    private boolean cancelled;

    public EntityAssemblerToggleEvent(@Nonnull Player player, @Nonnull AbstractEntityAssembler<?> assembler, @Nonnull Block block, boolean enabling) {
        super(player);

        Validate.notNull(assembler, "The AbstractEntityAssembler must not be null");
        Validate.notNull(block, "The Block must not be null");

        this.assembler = assembler;
        this.block = block;
        this.enabling = enabling;
    }

    /**
     * This returns the {@link AbstractEntityAssembler} being toggled.
     *
     * @return The {@link AbstractEntityAssembler}
     */
    @Nonnull
    public AbstractEntityAssembler<?> getAssembler() {
        return assembler;
    }

    /**
     * This returns the {@link Block} of the {@link AbstractEntityAssembler}.
     *
     * @return The assembler {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the state the {@link AbstractEntityAssembler} is about to
     * assume: true if it is about to be enabled, false if it is about to be
     * disabled.
     *
     * @return The new state
     */
    public boolean isEnabling() {
        return enabling;
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

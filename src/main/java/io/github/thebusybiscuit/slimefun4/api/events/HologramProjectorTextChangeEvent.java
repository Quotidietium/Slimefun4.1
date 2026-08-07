package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.HologramProjector;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} submits a new text for a
 * {@link HologramProjector} via chat input.
 * <p>
 * The new text is mutable: {@link #setNewText(String)} lets addons rewrite or censor it
 * before it is applied. Cancelling this event vetoes the change entirely: the hologram
 * keeps its previous text and the editor is not reopened.
 *
 * @author Zurker
 *
 * @see HologramProjector
 */
public class HologramProjectorTextChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final HologramProjector projector;
    private final Block block;
    private final String previousText;

    private String newText;
    private boolean cancelled;

    public HologramProjectorTextChangeEvent(@Nonnull Player player, @Nonnull HologramProjector projector, @Nonnull Block block, @Nonnull String previousText, @Nonnull String newText) {
        super(player);
        Validate.notNull(projector, "The HologramProjector must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(previousText, "The previous text must not be null");
        Validate.notNull(newText, "The new text must not be null");

        this.projector = projector;
        this.block = block;
        this.previousText = previousText;
        this.newText = newText;
    }

    /**
     * This returns the {@link HologramProjector} whose text is being changed.
     *
     * @return The {@link HologramProjector}
     */
    @Nonnull
    public HologramProjector getProjector() {
        return projector;
    }

    /**
     * This returns the {@link Block} of the {@link HologramProjector}.
     *
     * @return The {@link HologramProjector} {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the text the hologram currently shows (the raw, uncolored input as
     * it was submitted).
     *
     * @return The previous text
     */
    @Nonnull
    public String getPreviousText() {
        return previousText;
    }

    /**
     * This returns the text the hologram is about to show.
     *
     * @return The new text
     */
    @Nonnull
    public String getNewText() {
        return newText;
    }

    /**
     * Rewrites the text the hologram is about to show, e.g. to censor it.
     *
     * @param newText
     *            The replacement text
     */
    public void setNewText(@Nonnull String newText) {
        Validate.notNull(newText, "The new text must not be null");
        this.newText = newText;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} edits the script of a
 * {@link ProgrammableAndroid} through its script editor and the new script is about
 * to be stored: adding, deleting or duplicating an instruction, or downloading a
 * shared script into the android.
 * <p>
 * Cancelling this event vetoes the change: the stored script stays as it is and the
 * editor is not refreshed, as if the click had never happened.
 * <p>
 * Scripts are stored as dash-separated instruction tokens beginning with a 'START'
 * token and ending with a 'REPEAT' token. The event is fired before anything is
 * written, synchronously, since menu clicks happen on the main thread. Programmatic
 * changes through {@link ProgrammableAndroid#setScript} do not fire this event, only
 * player-driven edits do.
 *
 * @author Zurker
 *
 * @see AndroidMoveEvent
 * @see AndroidRotateEvent
 * @see ProgrammableAndroid
 */
public class AndroidScriptChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ProgrammableAndroid android;
    private final Block block;
    private final String oldScript;
    private final String newScript;

    private boolean cancelled;

    public AndroidScriptChangeEvent(@Nonnull Player player, @Nonnull ProgrammableAndroid android, @Nonnull Block block, @Nonnull String oldScript, @Nonnull String newScript) {
        super(player);
        Validate.notNull(android, "The android must not be null");
        Validate.notNull(block, "The Block must not be null");
        Validate.notNull(oldScript, "The old script must not be null");
        Validate.notNull(newScript, "The new script must not be null");

        this.android = android;
        this.block = block;
        this.oldScript = oldScript;
        this.newScript = newScript;
    }

    /**
     * This returns the {@link ProgrammableAndroid} whose script is being changed.
     *
     * @return The {@link ProgrammableAndroid}
     */
    @Nonnull
    public ProgrammableAndroid getAndroid() {
        return android;
    }

    /**
     * This returns the {@link Block} of the {@link ProgrammableAndroid}.
     *
     * @return The android {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the script that is currently stored on the android, as
     * dash-separated instruction tokens.
     *
     * @return The old script
     */
    @Nonnull
    public String getOldScript() {
        return oldScript;
    }

    /**
     * This returns the script that is about to be stored, as dash-separated
     * instruction tokens.
     *
     * @return The new script
     */
    @Nonnull
    public String getNewScript() {
        return newScript;
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

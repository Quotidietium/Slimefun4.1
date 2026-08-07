package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} toggles a per-player guide
 * setting from the Slimefun guide settings menu and the new value is about to be stored.
 * <p>
 * Cancelling this event vetoes the change: the stored setting stays as it is and the
 * settings menu reopens with the old value. Which setting was toggled is told by
 * {@link #getReason()}, and the value is whether that setting is enabled.
 * <p>
 * The two settings govern the research experience: {@link Reason#RESEARCH_FIREWORKS}
 * whether a firework is launched on unlock, {@link Reason#LEARNING_ANIMATION} whether the
 * in-chat learning animation plays. The event is fired before the value is stored,
 * synchronously, since menu clicks happen on the main thread.
 *
 * @author Zurker
 *
 * @see GuideModeChangeEvent
 */
public class PlayerGuideOptionChangeEvent extends PlayerEvent implements Cancellable {

    /**
     * This enum describes which guide setting was toggled.
     */
    public enum Reason {

        /**
         * The "research fireworks" toggle. A value of {@code true} means a firework is launched
         * when a research is unlocked.
         */
        RESEARCH_FIREWORKS,

        /**
         * The "learning animation" toggle. A value of {@code true} means the in-chat learning
         * animation plays during a research.
         */
        LEARNING_ANIMATION
    }

    private static final HandlerList handlers = new HandlerList();

    private final Reason reason;
    private final boolean previousValue;
    private boolean newValue;

    private boolean cancelled;

    public PlayerGuideOptionChangeEvent(@Nonnull Player player, @Nonnull Reason reason, boolean previousValue, boolean newValue) {
        super(player);
        Validate.notNull(reason, "The reason must not be null");

        this.reason = reason;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    /**
     * This returns which guide setting was toggled.
     *
     * @return The {@link Reason}
     */
    @Nonnull
    public Reason getReason() {
        return reason;
    }

    /**
     * This returns whether the setting was enabled before this change.
     *
     * @return The previous setting value
     */
    public boolean getPreviousValue() {
        return previousValue;
    }

    /**
     * This returns whether the setting will be enabled after this change.
     *
     * @return The new setting value
     */
    public boolean getNewValue() {
        return newValue;
    }

    /**
     * This sets whether the setting will be enabled, overriding the toggled value.
     *
     * @param newValue
     *            The new setting value
     */
    public void setNewValue(boolean newValue) {
        this.newValue = newValue;
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

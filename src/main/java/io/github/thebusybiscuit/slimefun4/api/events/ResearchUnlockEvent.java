package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.researches.Research;

/**
 * This {@link Event} is called whenever a {@link Player} unlocks a {@link Research}.
 * <p>
 * On the non-instant unlock path (the survival guide animation), the research takes
 * {@link #DEFAULT_RESEARCH_TIME_TICKS} ticks to complete by default. Add-ons may adjust
 * that duration via {@link #setResearchTimeTicks(long)} - e.g. to let VIP players research
 * faster or to make high-cost researches take longer. The progress messages are spread
 * proportionally across the adjusted duration. On the instant unlock path the value is
 * ignored.
 *
 * @author TheBusyBiscuit
 *
 * @see Research
 *
 */
public class ResearchUnlockEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    /**
     * The default number of ticks a non-instant research takes to complete (5 seconds).
     */
    public static final long DEFAULT_RESEARCH_TIME_TICKS = 100L;

    private final Player player;
    private final Research research;
    private long researchTimeTicks = DEFAULT_RESEARCH_TIME_TICKS;
    private boolean cancelled;

    public ResearchUnlockEvent(@Nonnull Player p, @Nonnull Research research) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(p, "The Player cannot be null");
        Validate.notNull(research, "Research cannot be null");

        this.player = p;
        this.research = research;
    }

    @Nonnull
    public Player getPlayer() {
        return player;
    }

    @Nonnull
    public Research getResearch() {
        return research;
    }

    /**
     * This returns the number of ticks the non-instant research animation will take
     * before the {@link Research} is unlocked. Defaults to
     * {@link #DEFAULT_RESEARCH_TIME_TICKS}. Ignored on the instant unlock path.
     *
     * @return The research duration in ticks
     */
    public long getResearchTimeTicks() {
        return researchTimeTicks;
    }

    /**
     * The maximum research duration an addon may set: 72,000 ticks (one hour).
     * An unbounded duration would leave the player in the "currently researching"
     * set effectively forever - soft-locking any further research - and the
     * scheduled unlock would never run.
     */
    public static final long MAX_RESEARCH_TIME_TICKS = 72_000L;

    /**
     * This sets the number of ticks the non-instant research animation will take before
     * the {@link Research} is unlocked. A value of {@code 0} skips the animation entirely
     * and unlocks on the next tick.
     *
     * @param researchTimeTicks
     *            The research duration in ticks, between {@code 0} and {@link #MAX_RESEARCH_TIME_TICKS}
     */
    public void setResearchTimeTicks(long researchTimeTicks) {
        Validate.isTrue(researchTimeTicks >= 0, "The research time must not be negative");
        Validate.isTrue(researchTimeTicks <= MAX_RESEARCH_TIME_TICKS, "The research time must not exceed " + MAX_RESEARCH_TIME_TICKS + " ticks");

        this.researchTimeTicks = researchTimeTicks;
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

package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.thebusybiscuit.slimefun4.implementation.items.tools.TapeMeasure;

/**
 * This {@link PlayerEvent} is fired whenever a {@link Player} measures the distance between
 * their {@link TapeMeasure}'s anchor and a clicked {@link Block}: the measure sound is about
 * to be played and the distance message sent.
 * <p>
 * Cancelling this event skips the measurement feedback: no sound is played and no message is
 * sent. Addons may also adjust the reported distance via {@link #setDistance(double)}.
 *
 * @author Zurker
 *
 * @see TapeMeasure
 */
public class TapeMeasureMeasureEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final TapeMeasure tapeMeasure;
    private final Location anchor;
    private final Block measuredBlock;

    private double distance;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public TapeMeasureMeasureEvent(Player player, TapeMeasure tapeMeasure, Location anchor, Block measuredBlock, double distance) {
        super(player);
        Validate.notNull(tapeMeasure, "The TapeMeasure must not be null");
        Validate.notNull(anchor, "The anchor Location must not be null");
        Validate.notNull(measuredBlock, "The measured Block must not be null");
        Validate.isTrue(Double.isFinite(distance), "The distance must be finite");

        this.tapeMeasure = tapeMeasure;
        this.anchor = anchor;
        this.measuredBlock = measuredBlock;
        this.distance = distance;
    }

    /**
     * This returns the {@link TapeMeasure} that is being used.
     *
     * @return The {@link TapeMeasure}
     */
    @Nonnull
    public TapeMeasure getTapeMeasure() {
        return tapeMeasure;
    }

    /**
     * This returns the anchor {@link Location} stored on the {@link TapeMeasure}.
     *
     * @return The anchor {@link Location}
     */
    @Nonnull
    public Location getAnchor() {
        return anchor;
    }

    /**
     * This returns the {@link Block} that is being measured against the anchor.
     *
     * @return The measured {@link Block}
     */
    @Nonnull
    public Block getMeasuredBlock() {
        return measuredBlock;
    }

    /**
     * This returns the measured distance between the anchor and the measured {@link Block}.
     *
     * @return The measured distance
     */
    public double getDistance() {
        return distance;
    }

    /**
     * This sets the distance that will be reported to the {@link Player}.
     *
     * @param distance
     *            The distance to report
     */
    public void setDistance(double distance) {
        Validate.isTrue(Double.isFinite(distance), "The distance must be finite");

        this.distance = distance;
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

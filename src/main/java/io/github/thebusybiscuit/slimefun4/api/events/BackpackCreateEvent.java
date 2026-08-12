package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;

/**
 * This {@link Event} is fired whenever a {@link PlayerProfile} is about to create a NEW
 * {@link PlayerBackpack} - typically when a backpack item is opened for the very first
 * time and its storage has to be allocated.
 * <p>
 * The initial size of the new backpack can be adjusted via {@link #setSize(int)}, e.g.
 * to grant certain players a larger backpack than the item's default. Only this new
 * backpack is affected; the item's configured size is never modified.
 * <p>
 * This event is not cancellable - the creating caller expects a {@link PlayerBackpack}
 * to come into existence. Use {@link PlayerBackpackOpenEvent} to veto a backpack from
 * being opened instead.
 * <p>
 * The event is only fired for sizes that passed validation: an out-of-range requested
 * size (not one of 9/18/27/36/45/54) still throws as usual, without firing this event.
 * The event is not a {@link org.bukkit.event.player.PlayerEvent} because
 * {@link PlayerProfile#createBackpack(int)} is a programmatic API whose owner may be
 * offline.
 *
 * @author Zurker
 *
 * @see PlayerBackpackOpenEvent
 * @see BackpackResizeEvent
 * @see PlayerBackpack
 */
public class BackpackCreateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final PlayerProfile profile;
    private final int id;

    private int size;

    public BackpackCreateEvent(@Nonnull PlayerProfile profile, int id, int size) {
        super(!Bukkit.isPrimaryThread());

        Validate.notNull(profile, "The PlayerProfile must not be null");
        Validate.isTrue(id >= 0, "The backpack id must not be negative");
        Validate.isTrue(size >= 9 && size <= 54 && size % 9 == 0, "Invalid size! Size must be one of: [9, 18, 27, 36, 45, 54]");

        this.profile = profile;
        this.id = id;
        this.size = size;
    }

    /**
     * This returns the {@link PlayerProfile} the new {@link PlayerBackpack} is
     * created for.
     *
     * @return The owning {@link PlayerProfile}
     */
    @Nonnull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * This returns the id that will be assigned to the new {@link PlayerBackpack}.
     *
     * @return The backpack id
     */
    public int getId() {
        return id;
    }

    /**
     * This returns the initial size the new {@link PlayerBackpack} will be created
     * with. It defaults to the requested size (usually the backpack item's
     * configured size).
     *
     * @return The initial size
     */
    public int getSize() {
        return size;
    }

    /**
     * This sets the initial size the new {@link PlayerBackpack} will be created
     * with.
     *
     * @param size
     *            The initial size, must be one of 9, 18, 27, 36, 45 or 54
     */
    public void setSize(int size) {
        Validate.isTrue(size >= 9 && size <= 54 && size % 9 == 0, "Invalid size! Size must be one of: [9, 18, 27, 36, 45, 54]");

        this.size = size;
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

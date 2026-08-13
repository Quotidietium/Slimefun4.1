package io.github.thebusybiscuit.slimefun4.api.researches;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerPreResearchEvent;
import io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemState;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.services.localization.Language;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.setup.ResearchSetup;

/**
 * Represents a research, which is bound to one
 * {@link SlimefunItem} or more and requires XP levels to unlock said item(s).
 * 
 * @author TheBusyBiscuit
 * 
 * @see ResearchSetup
 * @see ResearchUnlockEvent
 * 
 */
public class Research implements Keyed {

    private final NamespacedKey key;
    private final int id;
    private final String name;
    private boolean enabled = true;
    private int cost;

    private final List<SlimefunItem> items = new LinkedList<>();

    /**
     * The {@link Research Researches} that must be unlocked before this one can be unlocked
     * via the in-game guide. This is empty by default, so every existing research behaves
     * exactly as before (no gating, no overhead).
     */
    private final Set<Research> dependencies = new LinkedHashSet<>();

    /**
     * The constructor for a {@link Research}.
     * 
     * Create a new research, then bind this research to the Slimefun items you want by calling
     * {@link #addItems(SlimefunItem...)}. Once you're finished, call {@link #register()}
     * to register it.
     * 
     * @param key
     *            A unique identifier for this {@link Research}
     * @param id
     *            old way of identifying researches
     * @param defaultName
     *            The displayed name of this {@link Research}
     * @param defaultCost
     *            The Cost in XP levels to unlock this {@link Research}
     * 
     */
    public Research(@Nonnull NamespacedKey key, int id, @Nonnull String defaultName, int defaultCost) {
        Validate.notNull(key, "A NamespacedKey must be provided");
        Validate.notNull(defaultName, "A default name must be specified");

        this.key = key;
        this.id = id;
        this.name = defaultName;
        this.cost = defaultCost;
    }

    @Override
    public @Nonnull NamespacedKey getKey() {
        return key;
    }

    /**
     * This method returns whether this {@link Research} is enabled.
     * {@code false} can mean that this particular {@link Research} was disabled or that
     * researches altogether have been disabled.
     * 
     * @return Whether this {@link Research} is enabled or not
     */
    public boolean isEnabled() {
        return Slimefun.getRegistry().isResearchingEnabled() && enabled;
    }

    /**
     * Gets the ID of this {@link Research}.
     * This is the old way of identifying Researches, use a {@link NamespacedKey} in the future.
     * 
     * @deprecated Numeric Ids for Researches are deprecated, use {@link #getKey()} for identification instead.
     * 
     * @return The ID of this {@link Research}
     */
    @Deprecated
    public int getID() {
        return id;
    }

    /**
     * This method gives you a localized name for this {@link Research}.
     * The name is automatically taken from the currently selected {@link Language} of
     * the specified {@link Player}.
     * 
     * @param p
     *            The {@link Player} to translate this name for.
     * 
     * @return The localized Name of this {@link Research}.
     */
    public @Nonnull String getName(@Nonnull Player p) {
        String localized = Slimefun.getLocalization().getResearchName(p, key);
        return localized != null ? localized : name;
    }

    /**
     * Retrieve the name of this {@link Research} without any localization nor coloring.
     *
     * @return The unlocalized, decolorized name for this {@link Research}
     */
    public @Nonnull String getUnlocalizedName() {
        return ChatColor.stripColor(name);
    }

    /**
     * Gets the cost in XP levels to unlock this {@link Research}.
     * 
     * @return The cost in XP levels for this {@link Research}
     */
    public int getCost() {
        return cost;
    }

    /**
     * Sets the cost in XP levels to unlock this {@link Research}.
     * 
     * @param cost
     *            The cost in XP levels
     */
    public void setCost(int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Research cost must be zero or greater!");
        }

        this.cost = cost;
    }

    /**
     * Bind the specified {@link SlimefunItem SlimefunItems} to this {@link Research}.
     * 
     * @param items
     *            Instances of {@link SlimefunItem} to bind to this {@link Research}
     */
    public void addItems(SlimefunItem... items) {
        for (SlimefunItem item : items) {
            if (item != null) {
                item.setResearch(this);
            }
        }
    }

    /**
     * Bind the specified ItemStacks to this {@link Research}.
     * 
     * @param items
     *            Instances of {@link ItemStack} to bind to this {@link Research}
     * 
     * @return The current instance of {@link Research}
     */
    @Nonnull
    public Research addItems(ItemStack... items) {
        for (ItemStack item : items) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem != null) {
                sfItem.setResearch(this);
            }
        }

        return this;
    }

    /**
     * Lists every {@link SlimefunItem} that is bound to this {@link Research}.
     * 
     * @return The Slimefun items bound to this {@link Research}.
     */
    @Nonnull
    public List<SlimefunItem> getAffectedItems() {
        return items;
    }

    /**
     * This method checks whether there is at least one enabled {@link SlimefunItem}
     * included in this {@link Research}.
     *
     * @return whether there is at least one enabled {@link SlimefunItem}
     * included in this {@link Research}.
     */
    public boolean hasEnabledItems() {
        for (SlimefunItem item : items) {
            if (item.getState() == ItemState.ENABLED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a prerequisite {@link Research} that a player must unlock before they can unlock
     * this {@link Research} through the in-game guide.
     *
     * <p>
     * This enables add-ons to model research <b>trees</b> / tech prerequisites without having
     * to reimplement the unlock flow. The check is enforced for every unlock: the guide path
     * checks it in {@link #unlockFromGuide} before any cost is taken, and
     * {@link PlayerResearchTask} re-checks it when the unlock runs, so KnowledgeTome shares,
     * {@code /sf research all} and direct {@code unlock()} calls cannot skip prerequisites
     * either. It is a strict no-op for any {@link Research} without dependencies.
     * </p>
     *
     * <p>
     * Circular dependencies are detected and rejected with an {@link IllegalArgumentException}.
     * </p>
     *
     * @param prerequisite
     *            The {@link Research} that must be unlocked first
     */
    public void addDependency(@Nonnull Research prerequisite) {
        Validate.notNull(prerequisite, "The prerequisite Research must not be null");
        Validate.isTrue(prerequisite != this, "A Research cannot depend on itself!");

        if (createsCycle(prerequisite)) {
            throw new IllegalArgumentException("Adding this dependency would create a circular Research dependency!");
        }

        dependencies.add(prerequisite);
    }

    /**
     * Removes a previously added prerequisite {@link Research}.
     *
     * @param prerequisite
     *            The {@link Research} to remove from the prerequisites
     */
    public void removeDependency(@Nonnull Research prerequisite) {
        Validate.notNull(prerequisite, "The prerequisite Research must not be null");
        dependencies.remove(prerequisite);
    }

    /**
     * Returns all prerequisite {@link Research Researches} that must be unlocked first.
     *
     * @return An unmodifiable {@link Set} of prerequisite {@link Research Researches}
     */
    @Nonnull
    public Set<Research> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Whether this {@link Research} has any prerequisite {@link Research Researches}.
     *
     * @return {@code true} if at least one prerequisite was registered
     */
    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    /**
     * Checks whether the given {@link Player} has unlocked every prerequisite
     * {@link Research}.
     *
     * <p>
     * If the player's {@link PlayerProfile} is not currently cached (e.g. they are offline
     * and the profile was unloaded) this returns {@code false}: prerequisite satisfaction
     * cannot be verified without the profile, so the check fails closed rather than
     * potentially letting an unlock slip through while the profile is unavailable.
     * </p>
     *
     * @param player
     *            The {@link Player} to check
     *
     * @return {@code true} if all prerequisites are satisfied (or there are none)
     */
    public boolean meetsDependencies(@Nonnull OfflinePlayer player) {
        Validate.notNull(player, "The player cannot be null");

        if (dependencies.isEmpty()) {
            return true;
        }

        Optional<PlayerProfile> profile = PlayerProfile.find(player);
        return profile.isPresent() && meetsDependencies(profile.get());
    }

    /**
     * Checks whether the given {@link PlayerProfile} has unlocked every prerequisite
     * {@link Research}.
     *
     * @param profile
     *            The {@link PlayerProfile} to check
     *
     * @return {@code true} if all prerequisites are satisfied (or there are none)
     */
    public boolean meetsDependencies(@Nonnull PlayerProfile profile) {
        Validate.notNull(profile, "The PlayerProfile cannot be null");

        for (Research prerequisite : dependencies) {
            if (!profile.hasUnlocked(prerequisite)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the first unsatisfied prerequisite, if any.
     *
     * <p>
     * Convenience method for add-ons that want to surface <i>which</i> prerequisite is
     * still missing (e.g. in a custom message).
     * </p>
     *
     * @param profile
     *            The {@link PlayerProfile} to check
     *
     * @return The first missing {@link Research}, or an empty {@link Optional}
     */
    @Nonnull
    public Optional<Research> getFirstMissingDependency(@Nonnull PlayerProfile profile) {
        Validate.notNull(profile, "The PlayerProfile cannot be null");

        for (Research prerequisite : dependencies) {
            if (!profile.hasUnlocked(prerequisite)) {
                return Optional.of(prerequisite);
            }
        }

        return Optional.empty();
    }

    /**
     * Depth-first search to detect whether adding {@code candidate} as a dependency of
     * {@code this} would introduce a cycle, i.e. whether {@code candidate} (transitively)
     * already depends on {@code this}.
     *
     * @param candidate
     *            The {@link Research} we are about to add as a dependency
     *
     * @return {@code true} if a cycle would be created
     */
    private boolean createsCycle(@Nonnull Research candidate) {
        Set<Research> visited = new HashSet<>();
        Deque<Research> stack = new ArrayDeque<>();
        stack.push(candidate);

        while (!stack.isEmpty()) {
            Research current = stack.pop();

            if (!visited.add(current)) {
                continue;
            }

            if (current == this) {
                return true;
            }

            for (Research dep : current.dependencies) {
                stack.push(dep);
            }
        }

        return false;
    }

    /**
     * Handle what to do when a {@link Player} clicks on an un-researched item in
     * a {@link SlimefunGuideImplementation}.
     *
     * @param guide
     *            The {@link SlimefunGuideImplementation} used.
     * @param player
     *            The {@link Player} who clicked on the item.
     * @param profile
     *            The {@link PlayerProfile} of that {@link Player}.
     * @param sfItem
     *            The {@link SlimefunItem} on which the {@link Player} clicked.
     * @param itemGroup
     *            The {@link ItemGroup} where the {@link Player} was.
     * @param page
     *            The page number of where the {@link Player} was in the {@link ItemGroup};
     *
     */
    @ParametersAreNonnullByDefault
    public void unlockFromGuide(SlimefunGuideImplementation guide, Player player, PlayerProfile profile, SlimefunItem sfItem, ItemGroup itemGroup, int page) {
        if (!Slimefun.getRegistry().getCurrentlyResearchingPlayers().contains(player.getUniqueId())) {
            if (profile.hasUnlocked(this)) {
                guide.openItemGroup(profile, itemGroup, page);
            } else {
                PlayerPreResearchEvent event = new PlayerPreResearchEvent(player, this, sfItem);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    if (!meetsDependencies(player)) {
                        // A prerequisite research has not been unlocked yet. No-op for any
                        // research without dependencies (meetsDependencies() returns true).
                        Slimefun.getLocalization().sendMessage(player, "messages.locked-research", true);
                    } else if (this.canUnlock(player)) {
                        guide.unlockItem(player, sfItem, pl -> guide.openItemGroup(profile, itemGroup, page));
                    } else {
                        Slimefun.getLocalization().sendMessage(player, "messages.not-enough-xp", true);
                    }
                }
            }
        }
    }

    /**
     * Checks if the {@link Player} can unlock this {@link Research}.
     * 
     * @param p
     *            The {@link Player} to check
     * 
     * @return Whether that {@link Player} can unlock this {@link Research}
     */
    public boolean canUnlock(@Nonnull Player p) {
        if (!isEnabled()) {
            return true;
        }

        boolean creativeResearch = p.getGameMode() == GameMode.CREATIVE && Slimefun.getRegistry().isFreeCreativeResearchingEnabled();
        return creativeResearch || p.getLevel() >= cost;
    }

    /**
     * This unlocks this {@link Research} for the given {@link Player} without any form of callback.
     * 
     * @param p
     *            The {@link Player} who should unlock this {@link Research}
     * @param instant
     *            Whether to unlock it instantly
     */
    public void unlock(@Nonnull Player p, boolean instant) {
        unlock(p, instant, null);
    }

    /**
     * Unlocks this {@link Research} for the specified {@link Player}.
     *
     * @param p
     *            The {@link Player} for which to unlock this {@link Research}
     * @param isInstant
     *            Whether to unlock this {@link Research} instantly
     * @param callback
     *            A callback which will be run when the {@link Research} animation completed
     */
    public void unlock(@Nonnull Player p, boolean isInstant, @Nullable Consumer<Player> callback) {
        unlock(p, isInstant, callback, null);
    }

    /**
     * Unlocks this {@link Research} for the specified {@link Player}.
     *
     * @param p
     *            The {@link Player} for which to unlock this {@link Research}
     * @param isInstant
     *            Whether to unlock this {@link Research} instantly
     * @param callback
     *            A callback which will be run when the {@link Research} animation completed
     * @param failureHandler
     *            A {@link Runnable} run on the main Thread when the {@link PlayerProfile}
     *            could not be loaded at all (so the {@link Research} was never unlocked),
     *            allowing callers to compensate any cost they already took
     */
    public void unlock(@Nonnull Player p, boolean isInstant, @Nullable Consumer<Player> callback, @Nullable Runnable failureHandler) {
        unlock(p, isInstant, callback, failureHandler, null);
    }

    /**
     * Unlocks this {@link Research} for the specified {@link Player}.
     *
     * @param p
     *            The {@link Player} who should unlock this {@link Research}
     * @param isInstant
     *            Whether to unlock this {@link Research} instantly
     * @param callback
     *            A callback which will be run when the {@link Research} animation completed
     * @param failureHandler
     *            A {@link Runnable} run on the main Thread when the {@link PlayerProfile}
     *            could not be loaded at all (so the {@link Research} was never unlocked),
     *            allowing callers to compensate any cost they already took
     * @param cancelHandler
     *            A {@link Runnable} run on the main Thread when an addon cancelled the
     *            {@link ResearchUnlockEvent} (so the {@link Research} was never unlocked),
     *            allowing callers to compensate any cost they already took
     */
    public void unlock(@Nonnull Player p, boolean isInstant, @Nullable Consumer<Player> callback, @Nullable Runnable failureHandler, @Nullable Runnable cancelHandler) {
        PlayerProfile.get(p, new PlayerResearchTask(this, isInstant, callback, cancelHandler), failureHandler);
    }

    /**
     * Registers this {@link Research}.
     */
    public void register() {
        Slimefun.getResearchCfg().setDefaultValue("enable-researching", true);
        String path = key.getNamespace() + '.' + key.getKey();

        if (Slimefun.getResearchCfg().contains(path + ".enabled") && !Slimefun.getResearchCfg().getBoolean(path + ".enabled")) {
            for (SlimefunItem item : new ArrayList<>(items)) {
                if (item != null) {
                    item.setResearch(null);
                }
            }

            enabled = false;
            return;
        }

        Slimefun.getResearchCfg().setDefaultValue(path + ".cost", getCost());
        Slimefun.getResearchCfg().setDefaultValue(path + ".enabled", true);

        setCost(Slimefun.getResearchCfg().getInt(path + ".cost"));
        enabled = true;

        Slimefun.getRegistry().getResearches().add(this);
    }

    /**
     * Attempts to get a {@link Research} with the given {@link NamespacedKey}.
     * 
     * @param key
     *            the {@link NamespacedKey} of the {@link Research} you are looking for
     * 
     * @return An {@link Optional} with or without the found {@link Research}
     */
    @Nonnull
    public static Optional<Research> getResearch(@Nullable NamespacedKey key) {
        if (key == null) {
            return Optional.empty();
        }

        for (Research research : Slimefun.getRegistry().getResearches()) {
            if (research.getKey().equals(key)) {
                return Optional.of(research);
            }
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Research (" + getKey() + ')';
    }
}
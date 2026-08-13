package io.github.thebusybiscuit.slimefun4.api.researches;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerAllResearchesUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.options.SlimefunGuideSettings;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.FireworkUtils;

/**
 * A {@link PlayerResearchTask} is run when a {@link Player} unlocks a {@link Research}.
 * 
 * @author TheBusyBiscuit
 * 
 * @see Research
 * @see ResearchUnlockEvent
 * @see PlayerProfile
 *
 */
public class PlayerResearchTask implements Consumer<PlayerProfile> {

    private static final int[] RESEARCH_PROGRESS = { 23, 44, 57, 92 };
    private static final String PLACEHOLDER = "%research%";

    private final Research research;
    private final boolean isInstant;
    private final Consumer<Player> callback;
    private final Runnable cancelHandler;

    /**
     * This constructs a new {@link PlayerResearchTask}.
     *
     * @param research
     *            The {@link Research} to unlock
     * @param isInstant
     *            Whether to unlock this {@link Research} instantaneously
     * @param callback
     *            The callback to run when the task has completed
     */
    PlayerResearchTask(@Nonnull Research research, boolean isInstant, @Nullable Consumer<Player> callback) {
        this(research, isInstant, callback, null);
    }

    /**
     * This constructs a new {@link PlayerResearchTask}.
     *
     * @param research
     *            The {@link Research} to unlock
     * @param isInstant
     *            Whether to unlock this {@link Research} instantaneously
     * @param callback
     *            The callback to run when the task has completed
     * @param cancelHandler
     *            A {@link Runnable} run on the main Thread when an addon cancelled the
     *            {@link ResearchUnlockEvent} (so the {@link Research} was never unlocked),
     *            allowing callers to compensate any cost they already took
     */
    PlayerResearchTask(@Nonnull Research research, boolean isInstant, @Nullable Consumer<Player> callback, @Nullable Runnable cancelHandler) {
        Validate.notNull(research, "The Research must not be null");

        this.research = research;
        this.isInstant = isInstant;
        this.callback = callback;
        this.cancelHandler = cancelHandler;
    }

    @Override
    public void accept(PlayerProfile profile) {
        if (!profile.hasUnlocked(research)) {
            Player p = profile.getPlayer();

            if (p == null) {
                return;
            }

            if (!isInstant) {
                Slimefun.runSync(() -> {
                    SoundEffect.PLAYER_RESEARCHING_SOUND.playFor(p);
                    Slimefun.getLocalization().sendMessage(p, "messages.research.progress", true, msg -> msg.replace(PLACEHOLDER, research.getName(p)).replace("%progress%", "0%"));
                }, 5L);
            }

            ResearchUnlockEvent event = new ResearchUnlockEvent(p, research);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                /*
                 * An addon vetoed the unlock. The caller may already have taken a cost
                 * (e.g. the XP levels deducted by the guide) - let it compensate that
                 * instead of letting the levels vanish without unlocking anything.
                 */
                if (cancelHandler != null) {
                    Slimefun.runSync(cancelHandler);
                }
            } else if (isInstant) {
                Slimefun.runSync(() -> unlockResearch(p, profile));
            } else if (Slimefun.getRegistry().getCurrentlyResearchingPlayers().add(p.getUniqueId())) {
                long duration = event.getResearchTimeTicks();

                Slimefun.getLocalization().sendMessage(p, "messages.research.start", true, msg -> msg.replace(PLACEHOLDER, research.getName(p)));
                sendUpdateMessage(p, duration);

                Slimefun.runSync(() -> {
                    Slimefun.getRegistry().getCurrentlyResearchingPlayers().remove(p.getUniqueId());

                    /*
                     * Re-resolve the profile instead of using the captured one: the
                     * player may have logged off mid-research and their profile been
                     * unloaded by the auto-save since. setResearched on that orphaned
                     * profile would never be persisted - the research would be lost
                     * while the levels were already deducted. PlayerProfile#get
                     * reloads from disk when the profile is gone.
                     */
                    PlayerProfile.get(p, freshProfile -> unlockResearch(p, freshProfile));
                }, duration);
            }
        }
    }

    /**
     * Sends the progress messages, spread proportionally across the research duration.
     * With the default duration of {@value ResearchUnlockEvent#DEFAULT_RESEARCH_TIME_TICKS}
     * ticks this reproduces the historic schedule of one message every 20 ticks.
     */
    private void sendUpdateMessage(@Nonnull Player p, long duration) {
        int steps = RESEARCH_PROGRESS.length + 1;

        for (int i = 1; i < steps; i++) {
            int index = i;

            Slimefun.runSync(() -> {
                SoundEffect.PLAYER_RESEARCHING_SOUND.playFor(p);

                Slimefun.getLocalization().sendMessage(p, "messages.research.progress", true, msg -> {
                    String progress = RESEARCH_PROGRESS[index - 1] + "%";
                    return msg.replace(PLACEHOLDER, research.getName(p)).replace("%progress%", progress);
                });
            }, duration * i / steps);
        }
    }

    private void unlockResearch(@Nonnull Player p, @Nonnull PlayerProfile profile) {
        profile.setResearched(research, true);
        Slimefun.getLocalization().sendMessage(p, "messages.unlocked", true, msg -> msg.replace(PLACEHOLDER, research.getName(p)));

        // Check if this was the last enabled research — fire a milestone event.
        if (PlayerAllResearchesUnlockEvent.getHandlerList().getRegisteredListeners().length > 0) {
            java.util.stream.Stream<io.github.thebusybiscuit.slimefun4.api.researches.Research> enabled = Slimefun.getRegistry().getResearches().stream().filter(io.github.thebusybiscuit.slimefun4.api.researches.Research::isEnabled);
            boolean allUnlocked = enabled.allMatch(profile::hasUnlocked);

            if (allUnlocked) {
                int total = (int) Slimefun.getRegistry().getResearches().stream().filter(io.github.thebusybiscuit.slimefun4.api.researches.Research::isEnabled).count();
                Bukkit.getPluginManager().callEvent(new PlayerAllResearchesUnlockEvent(p, profile, total));
            }
        }

        onFinish(p);

        // Check if the Server and the Player have enabled fireworks for researches
        // (the Player may have logged off while the research animation was playing)
        if (p.isOnline() && Slimefun.getRegistry().isResearchFireworkEnabled() && SlimefunGuideSettings.hasFireworksEnabled(p)) {
            FireworkUtils.launchRandom(p, 1);
        }
    }

    /**
     * This method is called when the {@link Research} successfully finished to unlock.
     * 
     * @param p
     *            The {@link Player} who has unlocked this {@link Research}
     */
    private void onFinish(@Nonnull Player p) {
        if (callback != null) {
            callback.accept(p);
        }
    }
}

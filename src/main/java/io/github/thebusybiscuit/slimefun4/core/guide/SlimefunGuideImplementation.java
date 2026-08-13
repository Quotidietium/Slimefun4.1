package io.github.thebusybiscuit.slimefun4.core.guide;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.ResearchCostEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.guide.options.SlimefunGuideSettings;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;

/**
 * This interface is used for the different implementations that add behaviour
 * to the {@link SlimefunGuide}.
 *
 * @author TheBusyBiscuit
 * 
 * @see SlimefunGuideMode
 * @see SurvivalSlimefunGuide
 *
 */
public interface SlimefunGuideImplementation {

    /**
     * Every {@link SlimefunGuideImplementation} can be associated with a
     * {@link SlimefunGuideMode}.
     *
     * @return The mode this {@link SlimefunGuideImplementation} represents
     */
    @Nonnull
    SlimefunGuideMode getMode();

    /**
     * Returns the {@link ItemStack} representation for this {@link SlimefunGuideImplementation}.
     * In other words: The {@link ItemStack} you hold in your hand and that you use to
     * open your {@link SlimefunGuide}
     *
     * @return The {@link ItemStack} representation for this {@link SlimefunGuideImplementation}
     */
    @Nonnull
    ItemStack getItem();

    @ParametersAreNonnullByDefault
    void openMainMenu(PlayerProfile profile, int page);

    @ParametersAreNonnullByDefault
    void openItemGroup(PlayerProfile profile, ItemGroup group, int page);

    @ParametersAreNonnullByDefault
    void openSearch(PlayerProfile profile, String input, boolean addToHistory);

    @ParametersAreNonnullByDefault
    void displayItem(PlayerProfile profile, ItemStack item, int index, boolean addToHistory);

    @ParametersAreNonnullByDefault
    void displayItem(PlayerProfile profile, SlimefunItem item, boolean addToHistory);

    @ParametersAreNonnullByDefault
    default void unlockItem(Player p, SlimefunItem sfitem, Consumer<Player> callback) {
        Research research = sfitem.getResearch();

        if (p.getGameMode() == GameMode.CREATIVE && Slimefun.getRegistry().isFreeCreativeResearchingEnabled()) {
            research.unlock(p, true, callback);
        } else {
            int cost = research.getCost();

            // Fire a vetoable, cost-modifiable event before any levels are deducted.
            if (ResearchCostEvent.getHandlerList().getRegisteredListeners().length > 0) {
                ResearchCostEvent event = new ResearchCostEvent(p, research, cost);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    cost = 0;
                } else {
                    cost = event.getCost();
                }
            }

            if (cost > 0) {
                if (cost > p.getLevel()) {
                    /*
                     * The ResearchCostEvent may have raised the cost (surcharge).
                     * canUnlock() gates on the base cost only, so re-check here:
                     * deducting more levels than the player has would drive their
                     * level negative (or wipe it entirely, depending on the server).
                     */
                    Slimefun.getLocalization().sendMessage(p, "messages.not-enough-xp", true);
                    return;
                }

                p.setLevel(p.getLevel() - cost);
            }

            final int refundedCost = cost;
            boolean skipLearningAnimation = Slimefun.getRegistry().isLearningAnimationDisabled() || !SlimefunGuideSettings.hasLearningAnimationEnabled(p);
            research.unlock(p, skipLearningAnimation, callback, () -> {
                /*
                 * The PlayerProfile could not be loaded (even after retrying), so
                 * the research was never unlocked - refund the levels we took.
                 * The refund tracks the actually deducted (possibly adjusted) cost.
                 */
                if (p.isOnline() && refundedCost > 0) {
                    p.setLevel(p.getLevel() + refundedCost);
                }
            });
        }
    }

}

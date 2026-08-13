package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import io.github.bakedlibs.dough.common.PlayerList;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

class ResearchCommand extends SubCommand {

    private static final String PLACEHOLDER_PLAYER = "%player%";
    private static final String PLACEHOLDER_RESEARCH = "%research%";

    @ParametersAreNonnullByDefault
    ResearchCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "research", false);
    }

    @Override
    protected String getDescription() {
        return "commands.research.description";
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        // Check if researching is even enabled
        if (!Slimefun.getRegistry().isResearchingEnabled()) {
            Slimefun.getLocalization().sendMessage(sender, "messages.researching-is-disabled");
            return;
        }

        if (args.length == 3) {
            // Allow console (trusted) in addition to permission holders, but NOT command blocks or
            // other non-player senders - they would otherwise bypass the cheat permission.
            if (sender instanceof ConsoleCommandSender || sender.hasPermission("slimefun.cheat.researches")) {
                Optional<Player> player = PlayerList.findByName(args[1]);

                if (player.isPresent()) {
                    Player p = player.get();

                    // Getting the PlayerProfile async
                    PlayerProfile.get(p, profile -> {
                        if (args[2].equalsIgnoreCase("all")) {
                            researchAll(sender, profile, p);
                        } else if (args[2].equalsIgnoreCase("reset")) {
                            reset(profile, p);
                        } else {
                            giveResearch(sender, p, args[2]);
                        }
                    });
                } else {
                    Slimefun.getLocalization().sendMessage(sender, "messages.not-online", true, msg -> msg.replace(PLACEHOLDER_PLAYER, args[1]));
                }
            } else {
                Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
            }
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.usage", true, msg -> msg.replace("%usage%", "/sf research <Player> <all/reset/Research>"));
        }
    }

    @ParametersAreNonnullByDefault
    private void giveResearch(CommandSender sender, Player p, String input) {
        Optional<Research> research = getResearchFromString(input);

        if (research.isPresent()) {
            research.get().unlock(p, true, player -> {
                UnaryOperator<String> variables = msg -> msg.replace(PLACEHOLDER_PLAYER, player.getName()).replace(PLACEHOLDER_RESEARCH, research.get().getName(player));
                Slimefun.getLocalization().sendMessage(player, "messages.give-research", true, variables);
            });
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.invalid-research", true, msg -> msg.replace(PLACEHOLDER_RESEARCH, input));
        }
    }

    @ParametersAreNonnullByDefault
    private void researchAll(CommandSender sender, PlayerProfile profile, Player p) {
        unlockResearchPass(sender, profile, p, new HashSet<>());
    }

    /**
     * Unlocks every {@link Research} whose prerequisites are already unlocked, then
     * re-checks on the next tick. The actual unlock lands in a deferred sync task, so a
     * dependent {@link Research} can only be attempted in a later pass - a single loop
     * would either refuse it (its prerequisite is not unlocked YET) or unlock it in
     * violation of the research tree, depending on registry order.
     * <p>
     * Each {@link Research} is attempted at most once: a research vetoed by an addon via
     * {@link io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent} must not
     * be retried forever.
     */
    @ParametersAreNonnullByDefault
    private void unlockResearchPass(CommandSender sender, PlayerProfile profile, Player p, Set<Research> attempted) {
        boolean attemptedNew = false;

        for (Research res : Slimefun.getRegistry().getResearches()) {
            if (!profile.hasUnlocked(res) && !attempted.contains(res) && res.meetsDependencies(profile)) {
                attempted.add(res);
                attemptedNew = true;

                Slimefun.getLocalization().sendMessage(sender, "messages.give-research", true, msg -> msg.replace(PLACEHOLDER_PLAYER, p.getName()).replace(PLACEHOLDER_RESEARCH, res.getName(p)));
                res.unlock(p, true);
            }
        }

        if (attemptedNew) {
            // The unlocks land on the next tick; only then can dependent researches pass
            Slimefun.runSync(() -> unlockResearchPass(sender, profile, p, attempted));
        }
    }

    @ParametersAreNonnullByDefault
    private void reset(PlayerProfile profile, Player p) {
        for (Research research : Slimefun.getRegistry().getResearches()) {
            profile.setResearched(research, false);
        }

        Slimefun.getLocalization().sendMessage(p, "commands.research.reset", true, msg -> msg.replace(PLACEHOLDER_PLAYER, p.getName()));
    }

    @Nonnull
    private Optional<Research> getResearchFromString(@Nonnull String input) {
        if (!input.contains(":")) {
            return Optional.empty();
        }

        for (Research research : Slimefun.getRegistry().getResearches()) {
            if (research.getKey().toString().equalsIgnoreCase(input)) {
                return Optional.of(research);
            }
        }

        return Optional.empty();
    }

}

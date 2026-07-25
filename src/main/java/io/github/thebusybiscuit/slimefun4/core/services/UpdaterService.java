package io.github.thebusybiscuit.slimefun4.core.services;

import javax.annotation.Nonnull;

import io.github.thebusybiscuit.slimefun4.api.SlimefunBranch;

/**
 * This class determines which {@link SlimefunBranch} the current build is running on,
 * based solely on the version string.
 *
 * <p>
 * <b>Note:</b> Automatic update checks and downloads have been removed from this fork.
 * This class no longer connects to any external server; it only performs local branch
 * detection so that other components can still query {@link #getBranch()}.
 * </p>
 *
 * @author TheBusyBiscuit
 */
public class UpdaterService {

    /**
     * The {@link SlimefunBranch} we are currently on.
     */
    private final SlimefunBranch branch;

    /**
     * This will create a new {@link UpdaterService} for the given version string.
     *
     * @param version
     *            The current version of Slimefun
     */
    public UpdaterService(@Nonnull String version) {
        if (version.contains("UNOFFICIAL")) {
            // This Server is using a modified build that is not a public release.
            branch = SlimefunBranch.UNOFFICIAL;
        } else if (version.startsWith("Dev - ")) {
            // If we are using a development build.
            branch = SlimefunBranch.DEVELOPMENT;
        } else if (version.startsWith("RC - ")) {
            // If we are using a "stable" build.
            branch = SlimefunBranch.STABLE;
        } else {
            branch = SlimefunBranch.UNKNOWN;
        }
    }

    /**
     * This method returns the branch the current build of Slimefun is running on.
     * This can be used to determine whether we are dealing with an official build
     * or a build that was unofficially modified.
     *
     * @return The branch this build of Slimefun is on.
     */
    public @Nonnull SlimefunBranch getBranch() {
        return branch;
    }
}

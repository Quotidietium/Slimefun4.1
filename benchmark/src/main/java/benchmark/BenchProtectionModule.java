package benchmark;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.bakedlibs.dough.protection.ProtectionModule;

/**
 * A protection module with a realistic per-query cost, standing in for a
 * region-protection plugin (WorldGuard-class): every
 * {@link #hasPermission} walks a small list of cuboid regions, checks
 * containment by coordinate comparisons and resolves membership by UUID set
 * lookup. No synthetic delays - this is the shape of work a real module does.
 *
 * <p>All regions allow everyone; the module's answers are indistinguishable
 * from "no module installed" for routing outcomes, only the query cost is
 * added. It exists purely to make the per-(owner, target) permission queries
 * of {@code CargoNetworkTask} measurable.
 */
public final class BenchProtectionModule implements ProtectionModule {

    /** Immutable cuboid region with a member list. */
    private record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Set<UUID> members) {
        boolean contains(Location l) {
            return l.getBlockX() >= minX && l.getBlockX() <= maxX
                && l.getBlockY() >= minY && l.getBlockY() <= maxY
                && l.getBlockZ() >= minZ && l.getBlockZ() <= maxZ;
        }

        boolean isMember(UUID player) {
            return members.contains(player);
        }
    }

    private final Plugin plugin;

    /**
     * A handful of regions over the bench world - enough walk work per query
     * to resemble a real setup, none of them denying anyone.
     */
    private final List<Cuboid> regions;

    public BenchProtectionModule(Plugin plugin, UUID... everyone) {
        this.plugin = plugin;
        Set<UUID> members = Set.of(everyone);

        this.regions = List.of(
            new Cuboid(-500, 0, -500, 500, 128, 500, members),
            new Cuboid(-1000, 0, -1000, -501, 128, 1000, members),
            new Cuboid(501, 0, -1000, 1000, 128, 1000, members),
            new Cuboid(-1000, 0, 501, 1000, 128, 1000, members),
            new Cuboid(-2000, 0, -2000, -1001, 128, 2000, members)
        );
    }

    @Override
    public void load() {
        // Nothing to load - the regions are fixed
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean hasPermission(org.bukkit.OfflinePlayer player, Location l, Interaction action) {
        for (Cuboid region : regions) {
            if (region.contains(l)) {
                return region.isMember(player.getUniqueId());
            }
        }

        return true;
    }
}

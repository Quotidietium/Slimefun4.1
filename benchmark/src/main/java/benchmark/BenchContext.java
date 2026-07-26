package benchmark;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Shared context for all scenarios: the mock server, the loaded plugin and a
 * dedicated benchmark world registered with BlockStorage.
 */
public final class BenchContext {

    private final ServerMock server;
    private final Slimefun plugin;
    private World world;

    public BenchContext(ServerMock server, Slimefun plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    public void prepare() {
        world = server.addSimpleWorld("bench_world");
        Slimefun.getRegistry().getWorlds().put(world.getName(), new BlockStorage(world));

        /*
         * The MockBukkit unit-test environment does not load config.yml
         * defaults, so "URID.enable-tickers" (true in production) reads as
         * false and every ticking item would be disabled on registration.
         * Force the production value.
         */
        Slimefun.getCfg().setValue("URID.enable-tickers", true);

        BenchItems.register(plugin);
    }

    public ServerMock server() {
        return server;
    }

    public Plugin plugin() {
        return plugin;
    }

    public World world() {
        return world;
    }

    /**
     * Creates {@code count} distinct locations in a dense 128-wide grid at the
     * given y-level. Dense placement mimics a realistic machine room and keeps
     * the number of touched chunks small.
     */
    public List<Location> grid(int count, int y) {
        List<Location> locations = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            locations.add(new Location(world, i % 128, y, i / 128));
        }

        return locations;
    }
}

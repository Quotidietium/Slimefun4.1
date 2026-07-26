package benchmark.scenarios;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.core.services.holograms.HologramsService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Measures the hologram label update path.
 *
 * <p>Energy networks push a hologram label update per hologram per network
 * tick, but the label text rarely changes. The baseline always goes through
 * the full update (entity lookup by UUID included); the optimized version
 * skips the update entirely while the label is unchanged and the hologram
 * was recently confirmed alive.
 *
 * <p>MockBukkit cannot create holograms through the service itself
 * ({@code LivingEntityMock#setRemoveWhenFarAway} is unimplemented), so the
 * scenario spawns plain {@link ArmorStand}s and pre-seeds the service's
 * hologram cache via reflection. The measured label-update path is identical
 * to production afterwards.
 */
public final class HologramLabelBench {

    private static final String LABEL = "100 J";
    private static final int HOLOGRAMS = 2000;
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;

    public void run(BenchContext ctx, Results results) {
        HologramsService service = Slimefun.getHologramsService();
        List<Location> locations = ctx.grid(HOLOGRAMS, 112);

        if (!seedHolograms(ctx, service, locations, results)) {
            return;
        }

        // Warmup also gives both versions the first (label-changing) update.
        long[] samples = Bench.timeRounds(WARMUP, ROUNDS, round -> {
            for (Location l : locations) {
                service.setHologramLabel(l, LABEL);
            }
        });

        results.emit("hologram-label", "unchanged-label", "median_ns_per_call", "ns",
            (double) Bench.median(samples) / HOLOGRAMS);
        results.emit("hologram-label", "unchanged-label", "min_ns_per_call", "ns",
            (double) Bench.min(samples) / HOLOGRAMS);
    }

    @SuppressWarnings("unchecked")
    private boolean seedHolograms(BenchContext ctx, HologramsService service, List<Location> locations, Results results) {
        try {
            Field cacheField = HologramsService.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            Map<BlockPosition, Object> cache = (Map<BlockPosition, Object>) cacheField.get(service);

            Class<?> hologramClass = Class.forName("io.github.thebusybiscuit.slimefun4.core.services.holograms.Hologram");
            Constructor<?> constructor = hologramClass.getDeclaredConstructor(UUID.class);
            constructor.setAccessible(true);

            for (Location l : locations) {
                ArmorStand stand = (ArmorStand) ctx.world().spawnEntity(l, EntityType.ARMOR_STAND);
                cache.put(new BlockPosition(l), constructor.newInstance(stand.getUniqueId()));
            }

            return true;
        } catch (ReflectiveOperationException | ClassCastException x) {
            results.note("hologram-label: cannot seed hologram cache (" + x + "), scenario skipped");
            return false;
        }
    }
}

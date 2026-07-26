package benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import benchmark.scenarios.BlockStorageWriteBench;
import benchmark.scenarios.CapacitorTextureBench;
import benchmark.scenarios.HologramLabelBench;
import benchmark.scenarios.MachineIdleScanBench;
import benchmark.scenarios.TickerRunBench;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Entry point of the SlimeFun4.1 performance benchmark.
 *
 * <p>Usage: {@code mvn compile exec:java -Dsf.classes=<slimefun target/classes>
 * -Dbench.label=<label> -Dbench.out=<results file>}
 *
 * <p>The harness boots a MockBukkit server with the full Slimefun plugin
 * loaded from the classes directory given by {@code sf.classes}, then runs
 * all scenarios against that version. Running it twice (once against the
 * baseline build, once against the optimized build) yields directly
 * comparable numbers.
 *
 * <p>Scenario order matters: the ticker scenario registers thousands of
 * ticking locations that the plugin's own scheduled ticker would then keep
 * ticking in the background, so it runs last.
 */
public final class BenchMain {

    private BenchMain() {}

    public static void main(String[] args) {
        String label = args.length > 0 ? args[0] : "current";
        Path out = Path.of(args.length > 1 ? args[1] : "report/results-" + label + ".txt");

        // BlockStorage persists under the relative path "data-storage/...".
        // Start from a clean slate so runs are independent of each other.
        deleteRecursively(Path.of("data-storage"));

        // ErrorReports are written to "plugins/Slimefun/error-reports/...".
        // Make sure that directory exists so a failing block cannot mask the
        // original exception behind a FileNotFoundException.
        try {
            Files.createDirectories(Path.of("plugins/Slimefun/error-reports"));
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }

        try (Results results = new Results(label, out)) {
            results.prepare();

            ServerMock server = MockBukkit.mock();
            Slimefun plugin = MockBukkit.load(Slimefun.class);

            results.note("Slimefun version under test: " + Slimefun.getVersion());
            results.note("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");

            BenchContext ctx = new BenchContext(server, plugin);
            ctx.prepare();

            new BlockStorageWriteBench().run(ctx, results);
            Bench.gcSettle();

            new MachineIdleScanBench().run(ctx, results);
            Bench.gcSettle();

            new CapacitorTextureBench().run(ctx, results);
            Bench.gcSettle();

            new HologramLabelBench().run(ctx, results);
            Bench.gcSettle();

            // Must run last: registers thousands of ticking locations.
            new TickerRunBench().run(ctx, results);

            MockBukkit.unmock();
        }

        // The plugin's async scheduler threads would keep the JVM alive.
        System.exit(0);
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException x) {
                    throw new UncheckedIOException(x);
                }
            });
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }
}

package benchmark;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Collects benchmark results. Every result is printed to stdout and appended
 * to a results file in a machine-readable line format:
 *
 * <pre>RESULT|&lt;label&gt;|&lt;scenario&gt;|&lt;variant&gt;|&lt;metric&gt;|&lt;unit&gt;|&lt;value&gt;</pre>
 */
public final class Results implements AutoCloseable {

    private final String label;
    private final Path out;

    public Results(String label, Path out) {
        this.label = label;
        this.out = out;
    }

    public void prepare() {
        try {
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }

            Files.deleteIfExists(out);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    public void emit(String scenario, String variant, String metric, String unit, double value) {
        String line = String.format(Locale.ROOT, "RESULT|%s|%s|%s|%s|%s|%.3f", label, scenario, variant, metric, unit, value);
        System.out.println(line);
        append(line);
    }

    public void note(String text) {
        String line = "NOTE|" + label + "|" + text;
        System.out.println(line);
        append(line);
    }

    private void append(String line) {
        try {
            Files.writeString(out, line + System.lineSeparator(),
                Files.exists(out) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    @Override
    public void close() {
        // Nothing to release; files are appended per line.
    }
}

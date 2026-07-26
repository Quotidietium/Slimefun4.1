package benchmark;

import java.util.Arrays;
import java.util.function.IntConsumer;

/**
 * Small timing helpers shared by all benchmark scenarios.
 */
public final class Bench {

    private Bench() {}

    /**
     * Runs {@code warmup} untimed rounds followed by {@code rounds} timed rounds.
     * The round index (0-based, counting warmups too) is passed to the callback.
     *
     * @return The measured durations in nanoseconds, sorted ascending.
     */
    public static long[] timeRounds(int warmup, int rounds, IntConsumer round) {
        for (int i = 0; i < warmup; i++) {
            round.accept(i);
        }

        long[] samples = new long[rounds];

        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            round.accept(warmup + i);
            samples[i] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        return samples;
    }

    public static long median(long[] sortedSamples) {
        return sortedSamples[sortedSamples.length / 2];
    }

    public static long min(long[] sortedSamples) {
        return sortedSamples[0];
    }

    /**
     * Lets the JVM settle between scenarios so that GC pauses from one
     * scenario do not bleed into the measurements of the next.
     */
    public static void gcSettle() {
        System.gc();
        System.gc();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import com.percussion.delivery.feeds.data.*;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.TimeUnit;

/**
 * Performance testing and data generation utilities for feed tests.
 * Provides methods for measuring performance and generating test data.
 */
public final class FeedPerformanceUtils {
    private static final Random RANDOM = new Random();
    private static final String[] SAMPLE_TITLES = {
        "Breaking News", "Tech Update", "Sports Coverage",
        "Weather Alert", "Business Report", "Entertainment News"
    };
    private static final String[] SAMPLE_DESCRIPTIONS = {
        "Latest updates on breaking news story",
        "Technology trends and analysis",
        "Live sports coverage and results",
        "Weather updates and forecasts",
        "Business market analysis",
        "Entertainment industry news"
    };

    private FeedPerformanceUtils() {
        // Utility class, no instantiation
    }

    /**
     * Measures operation performance with warmup.
     *
     * @param operation Operation to measure
     * @param warmupRuns Number of warmup runs
     * @param measureRuns Number of measurement runs
     * @return Average execution time in milliseconds
     */
    public static double measureAverageTime(Runnable operation,
            int warmupRuns,
            int measureRuns) {
        // Warm up JVM
        IntStream.range(0, warmupRuns).forEach(i -> operation.run());

        // Measure
        var times = IntStream.range(0, measureRuns)
            .mapToLong(i -> {
                var start = System.nanoTime();
                operation.run();
                return System.nanoTime() - start;
            })
            .toArray();

        return calculateAverageMs(times);
    }

    /**
     * Generates random feed items for performance testing.
     *
     * @param count Number of items to generate
     * @param baseUrl Base URL for item links
     * @return List of random feed items
     */
    public static List<PSFeedItem> generateRandomItems(int count, String baseUrl) {
        return IntStream.range(0, count)
            .mapToObj(i -> PSFeedItem.builder()
                .title(getRandomTitle())
                .description(getRandomDescription())
                .link(String.format("%s/item/%d", baseUrl, i))
                .publishDate(Instant.now().minusSeconds(RANDOM.nextInt(86400)))
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Generates feed descriptors for performance testing.
     *
     * @param count Number of descriptors
     * @param site Site name
     * @return List of test descriptors
     */
    public static List<PSFeedDescriptor> generateTestDescriptors(int count,
            String site) {
        return IntStream.range(0, count)
            .mapToObj(i -> PSFeedDescriptor.builder()
                .name("test-feed-" + i)
                .site(site)
                .title("Test Feed " + i)
                .description("Test Description " + i)
                .link("https://test.percussion.com/feed/" + i)
                .type(i % 2 == 0 ? "ATOM" : "RSS2")
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Runs performance test with timing and memory measurements.
     *
     * @param name Test name
     * @param operation Operation to test
     * @param iterations Number of iterations
     * @return PerformanceResult with timing and memory stats
     */
    public static PerformanceResult runPerformanceTest(String name,
            Supplier<?> operation,
            int iterations) {
        System.gc(); // Hint to GC
        var startMemory = Runtime.getRuntime().totalMemory() -
                         Runtime.getRuntime().freeMemory();

        var result = new PerformanceResult();
        result.name = name;
        result.iterations = iterations;

        var times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            var start = System.nanoTime();
            operation.get();
            times[i] = System.nanoTime() - start;
        }

        System.gc(); // Hint to GC
        var endMemory = Runtime.getRuntime().totalMemory() -
                       Runtime.getRuntime().freeMemory();

        result.averageTimeMs = calculateAverageMs(times);
        result.memoryUsedBytes = endMemory - startMemory;

        return result;
    }

    private static double calculateAverageMs(long[] times) {
        return IntStream.range(0, times.length)
            .mapToDouble(i -> (double) times[i] / TimeUnit.MILLISECONDS.toNanos(1))
            .average()
            .orElse(0.0);
    }

    private static String getRandomTitle() {
        return SAMPLE_TITLES[RANDOM.nextInt(SAMPLE_TITLES.length)];
    }

    private static String getRandomDescription() {
        return SAMPLE_DESCRIPTIONS[RANDOM.nextInt(SAMPLE_DESCRIPTIONS.length)];
    }

    /**
     * Container for performance test results.
     */
    public static class PerformanceResult {
        private String name;
        private int iterations;
        private double averageTimeMs;
        private long memoryUsedBytes;

        public String getName() { return name; }
        public int getIterations() { return iterations; }
        public double getAverageTimeMs() { return averageTimeMs; }
        public long getMemoryUsedBytes() { return memoryUsedBytes; }

        @Override
        public String toString() {
            return String.format(
                "Performance Test: %s%n" +
                "Iterations: %d%n" +
                "Average Time: %.2f ms%n" +
                "Memory Used: %.2f MB%n",
                name, iterations, averageTimeMs,
                memoryUsedBytes / (1024.0 * 1024.0)
            );
        }
    }
}

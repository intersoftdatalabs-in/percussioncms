// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Benchmarking utility for XML operations.
 * Provides methods for measuring performance with statistical analysis.
 */
public final class XmlBenchmarkUtils {
    private static final int DEFAULT_WARMUP_ITERATIONS = 5;
    private static final int DEFAULT_TEST_ITERATIONS = 10;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private XmlBenchmarkUtils() {
        // Utility class, no instantiation
    }

    /**
     * Runs performance benchmark with warmup.
     *
     * @param operation Operation to benchmark
     * @return Benchmark results with statistics
     */
    public static BenchmarkResult benchmark(Callable<String> operation) {
        return benchmark(operation, DEFAULT_WARMUP_ITERATIONS, DEFAULT_TEST_ITERATIONS);
    }

    /**
     * Runs performance benchmark with custom iterations.
     *
     * @param operation Operation to benchmark
     * @param warmupIterations Number of warmup runs
     * @param testIterations Number of test runs
     * @return Benchmark results with statistics
     */
    public static BenchmarkResult benchmark(
            Callable<String> operation,
            int warmupIterations,
            int testIterations) {
        // Warm up JVM
        IntStream.range(0, warmupIterations)
            .forEach(i -> {
                try {
                    operation.call();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });

        // Run benchmark
        var times = new ArrayList<Duration>();
        var results = new ArrayList<String>();

        for (var i = 0; i < testIterations; i++) {
            var start = Instant.now();
            try {
                var result = operation.call();
                var duration = Duration.between(start, Instant.now());
                times.add(duration);
                results.add(result);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return new BenchmarkResult(times, results);
    }

    /**
     * Runs concurrent performance benchmark.
     *
     * @param operation Operation to benchmark
     * @param concurrentUsers Number of simulated users
     * @return Optional containing benchmark results or empty if timeout
     */
    public static Optional<ConcurrentBenchmarkResult> benchmarkConcurrent(
            Callable<String> operation,
            int concurrentUsers) {
        try (var executor = Executors.newFixedThreadPool(concurrentUsers)) {
            var futures = new ArrayList<CompletableFuture<BenchmarkResult>>();

            // Create tasks
            for (var i = 0; i < concurrentUsers; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return benchmark(operation);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }, executor));
            }

            // Wait for completion
            var results = new ArrayList<BenchmarkResult>();
            for (var future : futures) {
                var result = future.get(DEFAULT_TIMEOUT.toMillis(),
                    java.util.concurrent.TimeUnit.MILLISECONDS);
                if (result != null) {
                    results.add(result);
                }
            }

            return Optional.of(new ConcurrentBenchmarkResult(results));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Results for a single benchmark run.
     */
    public static class BenchmarkResult {
        private final List<Duration> times;
        private final List<String> results;
        private final DoubleSummaryStatistics stats;

        private BenchmarkResult(List<Duration> times, List<String> results) {
            this.times = List.copyOf(times);
            this.results = List.copyOf(results);
            this.stats = times.stream()
                .mapToDouble(Duration::toMillis)
                .summaryStatistics();
        }

        public double getAverageTimeMs() {
            return stats.getAverage();
        }

        public long getMinTimeMs() {
            return (long) stats.getMin();
        }

        public long getMaxTimeMs() {
            return (long) stats.getMax();
        }

        public List<String> getResults() {
            return results;
        }

        @Override
        public String toString() {
            return String.format(
                "Benchmark Results:%n" +
                "Average: %.2f ms%n" +
                "Min: %d ms%n" +
                "Max: %d ms%n" +
                "Total Runs: %d",
                getAverageTimeMs(),
                getMinTimeMs(),
                getMaxTimeMs(),
                results.size()
            );
        }
    }

    /**
     * Results for concurrent benchmark runs.
     */
    public static class ConcurrentBenchmarkResult {
        private final List<BenchmarkResult> results;
        private final DoubleSummaryStatistics combinedStats;

        private ConcurrentBenchmarkResult(List<BenchmarkResult> results) {
            this.results = List.copyOf(results);
            this.combinedStats = results.stream()
                .mapToDouble(BenchmarkResult::getAverageTimeMs)
                .summaryStatistics();
        }

        public double getOverallAverageMs() {
            return combinedStats.getAverage();
        }

        public long getConcurrentUsers() {
            return results.size();
        }

        @Override
        public String toString() {
            return String.format(
                "Concurrent Benchmark Results:%n" +
                "Users: %d%n" +
                "Overall Average: %.2f ms%n" +
                "Min Average: %.2f ms%n" +
                "Max Average: %.2f ms",
                getConcurrentUsers(),
                getOverallAverageMs(),
                combinedStats.getMin(),
                combinedStats.getMax()
            );
        }
    }
}

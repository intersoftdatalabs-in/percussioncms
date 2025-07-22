// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.delivery.feeds.PSFeedGenerator;
import com.percussion.delivery.feeds.data.PSFeedItem;
import com.percussion.delivery.feeds.test.PerformanceTestTemplateInvocationContextProvider.PerformanceScenario;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Performance tests for feed generation using test templates.
 * Tests different load scenarios with configurable parameters.
 */
@Tag("performance")
class FeedGeneratorPerformanceTest {
    private final PSFeedGenerator feedGenerator = new PSFeedGenerator();

    @TestTemplate
    @ExtendWith(PerformanceTestTemplateInvocationContextProvider.class)
    @DisplayName("Feed generation performance test")
    void testFeedGenerationPerformance(PerformanceScenario scenario) {
        // Create test data
        var items = FeedPerformanceUtils.generateRandomItems(
            scenario.operations(),
            "https://test.percussion.com/feeds"
        );

        // Create executor for concurrent testing
        try (var executor = Executors.newFixedThreadPool(scenario.concurrentUsers())) {
            var monitor = FeedPerformanceMonitor.create("FeedTest-" + scenario.name())
                .orElseThrow();

            // Create concurrent tasks
            var tasks = IntStream.range(0, scenario.operations())
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        var startTime = System.nanoTime();
                        feedGenerator.generateFeed(items);
                        var duration = Duration.ofNanos(System.nanoTime() - startTime);

                        monitor.recordExecution(duration);

                        if (duration.toMillis() > scenario.maxResponseTimeMs()) {
                            monitor.recordError();
                        }
                    } catch (Exception e) {
                        monitor.recordError();
                    }
                }, executor))
                .collect(Collectors.toList());

            // Wait for all tasks
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
                .join();

            // Verify results
            assertAll(
                () -> assertTrue(monitor.getMetric("totalRequests") == scenario.operations(),
                    "All operations should complete"),
                () -> assertTrue(monitor.getMetric("maxResponseTime") <= scenario.maxResponseTimeMs(),
                    "Response time should not exceed maximum"),
                () -> assertTrue(monitor.getMetric("totalErrors") == 0,
                    "Should have no errors")
            );
        }
    }

    @TestTemplate
    @ExtendWith(PerformanceTestTemplateInvocationContextProvider.class)
    @DisplayName("Feed memory usage test")
    void testFeedMemoryUsage(PerformanceScenario scenario) {
        var items = FeedPerformanceUtils.generateRandomItems(
            scenario.operations(),
            "https://test.percussion.com/feeds"
        );

        var result = FeedPerformanceUtils.runPerformanceTest(
            scenario.name(),
            () -> feedGenerator.generateFeed(items),
            10
        );

        assertTrue(result.getMemoryUsedBytes() <= Runtime.getRuntime().maxMemory() * 0.1,
            "Memory usage should not exceed 10% of max heap");
    }
}

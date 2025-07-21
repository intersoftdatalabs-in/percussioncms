// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

/**
 * Tests for feed performance utilities.
 */
class FeedPerformanceUtilsTest {

    @Test
    @DisplayName("Should measure average execution time")
    void shouldMeasureAverageExecutionTime() {
        double avgTime = FeedPerformanceUtils.measureAverageTime(
            () -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },
            2, // warmup runs
            3  // measure runs
        );

        assertTrue(avgTime >= 10.0,
            "Average time should be at least 10ms");
    }

    @Test
    @DisplayName("Should generate random feed items")
    void shouldGenerateRandomFeedItems() {
        var items = FeedPerformanceUtils.generateRandomItems(
            5, "https://test.com"
        );

        assertAll(
            () -> assertEquals(5, items.size()),
            () -> assertNotNull(items.get(0).getTitle()),
            () -> assertTrue(items.get(0).getLink()
                .startsWith("https://test.com")),
            () -> assertNotNull(items.get(0).getPublishDate())
        );
    }

    @Test
    @DisplayName("Should generate test descriptors")
    void shouldGenerateTestDescriptors() {
        var descriptors = FeedPerformanceUtils.generateTestDescriptors(
            3, "test-site"
        );

        assertAll(
            () -> assertEquals(3, descriptors.size()),
            () -> assertEquals("test-site",
                descriptors.get(0).getSite().orElse("")),
            () -> assertTrue(descriptors.get(0).getName()
                .orElse("").startsWith("test-feed-"))
        );
    }

    @Test
    @DisplayName("Should run performance test with memory tracking")
    void shouldRunPerformanceTestWithMemoryTracking() {
        var result = FeedPerformanceUtils.runPerformanceTest(
            "Memory Test",
            () -> {
                // Allocate some memory
                byte[] data = new byte[1024 * 1024];
                return data;
            },
            5
        );

        assertAll(
            () -> assertEquals("Memory Test", result.getName()),
            () -> assertEquals(5, result.getIterations()),
            () -> assertTrue(result.getAverageTimeMs() > 0),
            () -> assertTrue(result.getMemoryUsedBytes() > 0),
            () -> assertNotNull(result.toString())
        );
    }
}

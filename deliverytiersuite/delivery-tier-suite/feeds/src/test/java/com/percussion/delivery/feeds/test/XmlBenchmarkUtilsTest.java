// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;

/**
 * Tests for XML benchmarking utilities.
 */
class XmlBenchmarkUtilsTest {
    private static final String TEST_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Test Feed</title>
            <link href="http://test.com/feed"/>
        </feed>
        """;

    private static final Callable<String> TEST_OPERATION = () -> {
        Thread.sleep(50); // Simulate work
        return TEST_XML;
    };

    @Test
    @DisplayName("Should run basic benchmark")
    void shouldRunBasicBenchmark() throws Exception {
        var result = XmlBenchmarkUtils.benchmark(TEST_OPERATION);

        assertAll(
            () -> assertTrue(result.getAverageTimeMs() >= 50,
                "Average time should be at least 50ms"),
            () -> assertTrue(result.getMinTimeMs() >= 50,
                "Min time should be at least 50ms"),
            () -> assertFalse(result.getResults().isEmpty(),
                "Should have results")
        );
    }

    @Test
    @DisplayName("Should run custom iteration benchmark")
    void shouldRunCustomIterationBenchmark() throws Exception {
        var result = XmlBenchmarkUtils.benchmark(TEST_OPERATION, 2, 3);

        assertEquals(3, result.getResults().size(),
            "Should have exactly 3 test results");
    }

    @Test
    @DisplayName("Should run concurrent benchmark")
    void shouldRunConcurrentBenchmark() {
        var result = XmlBenchmarkUtils.benchmarkConcurrent(
            TEST_OPERATION,
            5 // concurrent users
        );

        assertTrue(result.isPresent(), "Should complete concurrent benchmark");
        assertAll(
            () -> assertEquals(5, result.get().getConcurrentUsers(),
                "Should have 5 concurrent users"),
            () -> assertTrue(result.get().getOverallAverageMs() >= 50,
                "Overall average should be at least 50ms")
        );
    }

    @Test
    @DisplayName("Should handle operation failure")
    void shouldHandleOperationFailure() {
        var failingOperation = (Callable<String>) () -> {
            throw new RuntimeException("Test failure");
        };

        var result = XmlBenchmarkUtils.benchmarkConcurrent(
            failingOperation,
            3
        );

        assertTrue(result.isEmpty(),
            "Should handle operation failure gracefully");
    }

    @Test
    @DisplayName("Should generate readable report")
    void shouldGenerateReadableReport() throws Exception {
        var result = XmlBenchmarkUtils.benchmark(TEST_OPERATION);
        var report = result.toString();

        assertAll(
            () -> assertTrue(report.contains("Benchmark Results"),
                "Should have title"),
            () -> assertTrue(report.contains("Average:"),
                "Should show average"),
            () -> assertTrue(report.contains("Min:"),
                "Should show min"),
            () -> assertTrue(report.contains("Max:"),
                "Should show max")
        );
    }
}

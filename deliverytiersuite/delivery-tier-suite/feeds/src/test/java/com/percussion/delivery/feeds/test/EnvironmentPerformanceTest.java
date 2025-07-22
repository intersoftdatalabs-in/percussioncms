// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

/**
 * Environment-specific performance configuration tests.
 */
@Tag("performance")
class EnvironmentPerformanceTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    @DisplayName("Should use CI-specific thresholds")
    void shouldUseCiThresholds() {
        // CI environment typically has limited resources
        assertAll(
            () -> assertTrue(BenchmarkConfig.getConcurrentUsers() <= 5,
                "Should limit concurrent users in CI"),
            () -> assertTrue(BenchmarkConfig.getTestIterations() <= 50,
                "Should limit iterations in CI")
        );
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Should optimize for Windows")
    void shouldOptimizeForWindows() {
        assertTrue(BenchmarkConfig.getBoolean("xml.cache.schema", true),
            "Should enable schema caching on Windows");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("Should optimize for Unix-like OS")
    void shouldOptimizeForUnix() {
        assertTrue(BenchmarkConfig.getBoolean("xml.parser.pool-size", true),
            "Should enable parser pooling on Unix");
    }

    @Test
    @EnabledIfSystemProperty(named = "java.version", matches = "11.*")
    @DisplayName("Should use Java 11 features")
    void shouldUseJava11Features() {
        assertTrue(BenchmarkConfig.getBoolean("performance.java11.string-concat", true),
            "Should use Java 11 string concatenation");
    }

    @Test
    @DisplayName("Should respect memory constraints")
    void shouldRespectMemoryConstraints() {
        var maxHeapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        var configuredMemoryMb = BenchmarkConfig.getLong(
            "performance.threshold.memory-mb",
            Long.MAX_VALUE
        );

        assertTrue(configuredMemoryMb <= maxHeapMb * 0.8,
            "Memory threshold should not exceed 80% of max heap");
    }

    @Test
    @DisplayName("Should adapt to available processors")
    void shouldAdaptToAvailableProcessors() {
        var availableProcessors = Runtime.getRuntime().availableProcessors();
        var configuredThreads = BenchmarkConfig.getInt(
            "benchmark.concurrent.users",
            Integer.MAX_VALUE
        );

        assertTrue(configuredThreads <= availableProcessors * 2,
            "Thread count should not exceed 2x available processors");
    }

    @Test
    @DisplayName("Should configure timeouts based on environment")
    void shouldConfigureTimeouts() {
        var isCI = System.getenv("CI") != null;
        var timeoutSeconds = BenchmarkConfig.getLong(
            "benchmark.timeout.seconds",
            30
        );

        if (isCI) {
            assertTrue(timeoutSeconds <= 60,
                "Should use shorter timeouts in CI");
        } else {
            assertTrue(timeoutSeconds >= 30,
                "Should use longer timeouts in dev/prod");
        }
    }
}

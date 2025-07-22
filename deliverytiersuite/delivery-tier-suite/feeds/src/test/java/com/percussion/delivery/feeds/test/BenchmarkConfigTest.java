// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import java.util.List;

/**
 * Tests for benchmark configuration loading with comprehensive edge case coverage.
 */
@Tag("performance")
class BenchmarkConfigTest {

    @Test
    @DisplayName("Should load integer properties")
    void shouldLoadIntegerProperties() {
        assertAll(
            () -> assertTrue(BenchmarkConfig.getWarmupIterations() > 0,
                "Should have positive warmup iterations"),
            () -> assertTrue(BenchmarkConfig.getTestIterations() > 0,
                "Should have positive test iterations"),
            () -> assertTrue(BenchmarkConfig.getConcurrentUsers() > 0,
                "Should have positive concurrent users")
        );
    }

    @Test
    @DisplayName("Should handle missing properties")
    void shouldHandleMissingProperties() {
        assertAll(
            () -> assertEquals(42, BenchmarkConfig.getInt("non.existent", 42),
                "Should use default for missing int"),
            () -> assertEquals(42L, BenchmarkConfig.getLong("non.existent", 42L),
                "Should use default for missing long"),
            () -> assertEquals(42.0, BenchmarkConfig.getDouble("non.existent", 42.0),
                "Should use default for missing double"),
            () -> assertEquals(true, BenchmarkConfig.getBoolean("non.existent", true),
                "Should use default for missing boolean"),
            () -> assertEquals("default", BenchmarkConfig.getString("non.existent", "default"),
                "Should use default for missing string")
        );
    }

    @Test
    @DisplayName("Should validate feed settings")
    void shouldValidateFeedSettings() {
        assertAll(
            () -> assertTrue(BenchmarkConfig.getFeedBatchSize() > 0,
                "Should have positive batch size"),
            () -> assertTrue(BenchmarkConfig.getResponseTimeThreshold() > 0,
                "Should have positive response time threshold")
        );
    }

    @ParameterizedTest(name = "Number format {0} should return default {2}")
    @MethodSource("provideInvalidNumberFormats")
    void shouldHandleInvalidNumberFormats(String key, String value, int defaultValue) {
        System.setProperty(key, value);
        assertEquals(defaultValue, BenchmarkConfig.getInt(key, defaultValue),
            "Should use default for invalid format");
    }

    private static Stream<Arguments> provideInvalidNumberFormats() {
        return Stream.of(
            Arguments.of("test.invalid.int", "not a number", 42),
            Arguments.of("test.invalid.int", "-1", 42),
            Arguments.of("test.invalid.int", "1.5", 42),
            Arguments.of("test.invalid.int", "", 42)
        );
    }

    @ParameterizedTest(name = "Boolean format {0} should parse to {1}")
    @MethodSource("provideBooleanValues")
    void shouldHandleBooleanFormats(String value, boolean expected) {
        var key = "test.bool";
        System.setProperty(key, value);
        assertEquals(expected, BenchmarkConfig.getBoolean(key, !expected),
            "Should parse boolean value correctly");
    }

    private static Stream<Arguments> provideBooleanValues() {
        return Stream.of(
            Arguments.of("true", true),
            Arguments.of("TRUE", true),
            Arguments.of("false", false),
            Arguments.of("FALSE", false),
            Arguments.of("invalid", false),
            Arguments.of("", false)
        );
    }

    @ParameterizedTest(name = "Performance setting {0} should be positive")
    @MethodSource("providePerformanceSettings")
    void shouldValidatePerformanceSettings(String description, long value) {
        assertTrue(value > 0,
            () -> description + " should be positive, but was: " + value);
    }

    private static Stream<Arguments> providePerformanceSettings() {
        return Stream.of(
            Arguments.of("Warmup iterations",
                BenchmarkConfig.getWarmupIterations()),
            Arguments.of("Test iterations",
                BenchmarkConfig.getTestIterations()),
            Arguments.of("Feed batch size",
                BenchmarkConfig.getFeedBatchSize()),
            Arguments.of("Response time threshold",
                BenchmarkConfig.getResponseTimeThreshold())
        );
    }

    @Test
    @DisplayName("Should clean up system properties after tests")
    void shouldCleanUpSystemProperties() {
        var testKey = "test.cleanup";
        System.setProperty(testKey, "value");

        try {
            assertNotNull(System.getProperty(testKey),
                "Property should be set");
        } finally {
            System.clearProperty(testKey);
        }

        assertNull(System.getProperty(testKey),
            "Property should be cleaned up");
    }

    @ParameterizedTest(name = "Feed batch size {0} should be valid")
    @ValueSource(ints = {1, 10, 100, 1000})
    void shouldValidateFeedBatchSizes(int batchSize) {
        System.setProperty("feed.batch-size", String.valueOf(batchSize));
        assertTrue(BenchmarkConfig.getFeedBatchSize() > 0,
            "Feed batch size should be positive");
    }

    @ParameterizedTest(name = "Load test duration {0} should be valid")
    @ValueSource(longs = {30, 60, 300, 600})
    void shouldValidateLoadTestDurations(long seconds) {
        System.setProperty("loadtest.steady-state-seconds", String.valueOf(seconds));
        assertTrue(BenchmarkConfig.getLoadTestDuration().getSeconds() >= 30,
            "Load test duration should be at least 30 seconds");
    }

    @ParameterizedTest(name = "Response threshold {0}ms should be valid")
    @MethodSource("provideResponseThresholds")
    void shouldValidateResponseThresholds(long threshold, boolean valid) {
        System.setProperty("performance.threshold.response-time-ms",
            String.valueOf(threshold));

        if (valid) {
            assertTrue(BenchmarkConfig.getResponseTimeThreshold() > 0,
                "Valid threshold should be accepted");
        } else {
            assertEquals(500L, BenchmarkConfig.getResponseTimeThreshold(),
                "Invalid threshold should use default");
        }
    }

    private static Stream<Arguments> provideResponseThresholds() {
        return Stream.of(
            Arguments.of(100L, true),   // Valid threshold
            Arguments.of(500L, true),   // Valid threshold
            Arguments.of(0L, false),    // Invalid - zero
            Arguments.of(-1L, false),   // Invalid - negative
            Arguments.of(Long.MAX_VALUE, false)  // Invalid - too large
        );
    }

    @ParameterizedTest(name = "XML setting {0} should have default {1}")
    @MethodSource("provideXmlSettings")
    void shouldProvideXmlDefaults(String key, boolean defaultValue) {
        assertEquals(defaultValue,
            BenchmarkConfig.getBoolean(key, defaultValue),
            "Should use correct default for XML setting");
    }

    private static Stream<Arguments> provideXmlSettings() {
        return Stream.of(
            Arguments.of("xml.validation.enabled", true),
            Arguments.of("xml.format.indent", true),
            Arguments.of("xml.format.normalize", true),
            Arguments.of("xml.security.external-entities", false),
            Arguments.of("xml.cache.schema", true)
        );
    }

    @Test
    @DisplayName("Should handle property dependencies")
    void shouldHandlePropertyDependencies() {
        // Set up dependent properties
        System.setProperty("benchmark.concurrent.users", "10");
        System.setProperty("benchmark.test.iterations", "5");

        var users = BenchmarkConfig.getConcurrentUsers();
        var iterations = BenchmarkConfig.getTestIterations();

        assertTrue(users * iterations <= 1000,
            "Total test operations should not exceed limits");
    }

    @TestFactory
    @DisplayName("Dynamic performance threshold tests")
    Stream<DynamicTest> dynamicThresholdTests() {
        record ThresholdTest(String key, long value, boolean valid) {}

        var tests = List.of(
            new ThresholdTest("response-time-ms", 500, true),
            new ThresholdTest("memory-mb", 256, true),
            new ThresholdTest("cpu-percent", 80, true),
            new ThresholdTest("gc-overhead", 10, true),
            new ThresholdTest("invalid-metric", -1, false)
        );

        return tests.stream()
            .map(test -> DynamicTest.dynamicTest(
                "Threshold " + test.key() + " should be " +
                (test.valid() ? "valid" : "invalid"),
                () -> {
                    var propertyKey = "performance.threshold." + test.key();
                    System.setProperty(propertyKey, String.valueOf(test.value()));

                    if (test.valid()) {
                        assertTrue(BenchmarkConfig.getLong(propertyKey, 0) > 0,
                            "Valid threshold should be positive");
                    } else {
                        assertEquals(0, BenchmarkConfig.getLong(propertyKey, 0),
                            "Invalid threshold should use default");
                    }
                }
            ));
    }

    @TestFactory
    @DisplayName("Dynamic XML configuration tests")
    Stream<DynamicTest> dynamicXmlConfigTests() {
        record XmlConfig(String key, String value, boolean expected) {}

        var configs = List.of(
            new XmlConfig("validation.enabled", "true", true),
            new XmlConfig("format.indent", "true", true),
            new XmlConfig("security.external-entities", "false", false),
            new XmlConfig("cache.schema", "true", true),
            new XmlConfig("unknown.setting", "true", false)
        );

        return configs.stream()
            .map(config -> DynamicTest.dynamicTest(
                "XML config " + config.key() + " should be " + config.expected(),
                () -> {
                    var propertyKey = "xml." + config.key();
                    System.setProperty(propertyKey, config.value());
                    assertEquals(config.expected(),
                        BenchmarkConfig.getBoolean(propertyKey, !config.expected()),
                        "XML setting should match expected value");
                }
            ));
    }

    @TestFactory
    @DisplayName("Dynamic load test configuration")
    Stream<DynamicTest> dynamicLoadTestConfig() {
        record LoadTestParam(String key, String value, boolean valid) {}

        return Stream.of(
            new LoadTestParam("ramp-up-seconds", "30", true),
            new LoadTestParam("steady-state-minutes", "5", true),
            new LoadTestParam("target-throughput", "100", true),
            new LoadTestParam("error-rate-threshold", "0.01", true),
            new LoadTestParam("ramp-up-seconds", "-1", false),
            new LoadTestParam("target-throughput", "0", false)
        ).map(param -> DynamicTest.dynamicTest(
            "Load test param " + param.key() + " should be " +
            (param.valid() ? "valid" : "invalid"),
            () -> {
                var propertyKey = "loadtest." + param.key();
                System.setProperty(propertyKey, param.value());

                if (param.valid()) {
                    assertTrue(BenchmarkConfig.getDouble(propertyKey, 0.0) > 0,
                        "Valid parameter should be positive");
                } else {
                    assertEquals(0.0, BenchmarkConfig.getDouble(propertyKey, 0.0),
                        "Invalid parameter should use default");
                }
            }
        ));
    }

    @Test
    @DisplayName("Should validate feed limits")
    void shouldValidateFeedLimits() {
        var maxItems = 1000;
        var batchSize = 100;

        System.setProperty("feed.max-items", String.valueOf(maxItems));
        System.setProperty("feed.batch-size", String.valueOf(batchSize));

        assertAll(
            () -> assertTrue(BenchmarkConfig.getFeedBatchSize() <= maxItems,
                "Batch size should not exceed max items"),
            () -> assertEquals(0, maxItems % batchSize,
                "Max items should be divisible by batch size")
        );
    }
}

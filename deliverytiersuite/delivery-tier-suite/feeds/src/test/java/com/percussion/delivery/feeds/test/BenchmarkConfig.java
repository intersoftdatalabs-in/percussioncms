// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import java.io.IOException;
import java.util.Properties;
import java.util.Optional;
import java.util.function.Function;

/**
 * Performance configuration loader and validator.
 * Thread-safe configuration handling for benchmarks.
 */
public final class BenchmarkConfig {
    private static final Properties CONFIG = new Properties();
    private static final String CONFIG_FILE = "performance.properties";

    static {
        try (var input = BenchmarkConfig.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                CONFIG.load(input);
            }
        } catch (IOException e) {
            // Use defaults if config not found
        }
    }

    private BenchmarkConfig() {
        // Utility class, no instantiation
    }

    /**
     * Gets integer property with default.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return Property value or default
     */
    public static int getInt(String key, int defaultValue) {
        return getProperty(key, Integer::parseInt).orElse(defaultValue);
    }

    /**
     * Gets long property with default.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return Property value or default
     */
    public static long getLong(String key, long defaultValue) {
        return getProperty(key, Long::parseLong).orElse(defaultValue);
    }

    /**
     * Gets double property with default.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return Property value or default
     */
    public static double getDouble(String key, double defaultValue) {
        return getProperty(key, Double::parseDouble).orElse(defaultValue);
    }

    /**
     * Gets boolean property with default.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return Property value or default
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return getProperty(key, Boolean::parseBoolean).orElse(defaultValue);
    }

    /**
     * Gets string property with default.
     *
     * @param key Property key
     * @param defaultValue Default if not found
     * @return Property value or default
     */
    public static String getString(String key, String defaultValue) {
        return Optional.ofNullable(CONFIG.getProperty(key))
            .orElse(defaultValue);
    }

    /**
     * Gets warmup iterations setting.
     *
     * @return Number of warmup iterations
     */
    public static int getWarmupIterations() {
        return getInt("benchmark.warmup.iterations", 5);
    }

    /**
     * Gets test iterations setting.
     *
     * @return Number of test iterations
     */
    public static int getTestIterations() {
        return getInt("benchmark.test.iterations", 10);
    }

    /**
     * Gets concurrent users setting.
     *
     * @return Number of concurrent users
     */
    public static int getConcurrentUsers() {
        return getInt("benchmark.concurrent.users", 10);
    }

    /**
     * Gets response time threshold.
     *
     * @return Maximum allowed response time in ms
     */
    public static long getResponseTimeThreshold() {
        return getLong("performance.threshold.response-time-ms", 500);
    }

    /**
     * Gets feed batch size.
     *
     * @return Number of items per batch
     */
    public static int getFeedBatchSize() {
        return getInt("feed.batch-size", 100);
    }

    private static <T> Optional<T> getProperty(String key, Function<String, T> parser) {
        return Optional.ofNullable(CONFIG.getProperty(key))
            .map(value -> {
                try {
                    return parser.apply(value);
                } catch (Exception e) {
                    return null;
                }
            });
    }
}

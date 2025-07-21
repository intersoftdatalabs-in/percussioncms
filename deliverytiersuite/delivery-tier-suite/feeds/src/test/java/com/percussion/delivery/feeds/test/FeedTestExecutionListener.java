// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import java.util.Properties;

/**
 * Spring test execution listener for feeds integration tests.
 * Handles test database cleanup and context preparation.
 */
public class FeedTestExecutionListener implements TestExecutionListener {
    private static final Properties testProperties = new Properties();

    static {
        try (var input = FeedTestExecutionListener.class
                .getClassLoader()
                .getResourceAsStream("test.properties")) {
            testProperties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test properties", e);
        }
    }

    @Override
    public void beforeTestClass(TestContext testContext) {
        // Load test properties into Spring context
        var env = testContext.getApplicationContext().getEnvironment();
        testProperties.forEach((key, value) ->
            System.setProperty(key.toString(), value.toString()));
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        // Clean up system properties
        testProperties.forEach((key, value) ->
            System.clearProperty(key.toString()));
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        // Check if method needs transaction
        var method = testContext.getTestMethod();
        if (method.isAnnotationPresent(Transactional.class)) {
            // Transaction handling is done by Spring
            return;
        }
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        // Clean up any test data if needed
    }

    @Override
    public void beforeTestExecution(TestContext testContext) {
        // Pre-execution setup if needed
    }

    @Override
    public void afterTestExecution(TestContext testContext) {
        // Post-execution cleanup if needed
    }

    /**
     * Gets a test property value.
     *
     * @param key Property key
     * @return Property value or null if not found
     */
    public static String getTestProperty(String key) {
        return testProperties.getProperty(key);
    }

    /**
     * Gets a test property value with default.
     *
     * @param key Property key
     * @param defaultValue Default value if not found
     * @return Property value or default
     */
    public static String getTestProperty(String key, String defaultValue) {
        return testProperties.getProperty(key, defaultValue);
    }
}

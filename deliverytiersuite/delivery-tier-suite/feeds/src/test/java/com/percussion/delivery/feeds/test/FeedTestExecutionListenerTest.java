// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for feed test execution listener.
 * Validates test context preparation and cleanup.
 */
@ExtendWith(MockitoExtension.class)
class FeedTestExecutionListenerTest {
    @Mock
    private TestContext testContext;

    @Mock
    private ConfigurableApplicationContext appContext;

    @Mock
    private ConfigurableEnvironment environment;

    private FeedTestExecutionListener listener;

    @BeforeEach
    void setUp() {
        listener = new FeedTestExecutionListener();
        when(testContext.getApplicationContext()).thenReturn(appContext);
        when(appContext.getEnvironment()).thenReturn(environment);
    }

    @Test
    @DisplayName("Should load test properties")
    void shouldLoadTestProperties() {
        assertDoesNotThrow(() -> {
            listener.beforeTestClass(testContext);
        });

        var baseUrl = FeedTestExecutionListener.getTestProperty("test.feed.baseUrl");
        assertNotNull(baseUrl, "Base URL should be loaded from properties");
    }

    @Test
    @DisplayName("Should provide default values for missing properties")
    void shouldProvideDefaultValuesForMissingProperties() {
        var nonExistentProp = FeedTestExecutionListener.getTestProperty(
            "non.existent.property",
            "default-value"
        );

        assertEquals("default-value", nonExistentProp);
    }

    @Test
    @DisplayName("Should clean up system properties after test")
    void shouldCleanUpSystemPropertiesAfterTest() {
        // Set up test property
        listener.beforeTestClass(testContext);
        var baseUrl = System.getProperty("test.feed.baseUrl");
        assertNotNull(baseUrl, "Property should be set");

        // Clean up
        listener.afterTestClass(testContext);
        var cleanedUrl = System.getProperty("test.feed.baseUrl");
        assertNull(cleanedUrl, "Property should be cleaned up");
    }

    @Test
    @DisplayName("Should handle test method with @Transactional")
    void shouldHandleTestMethodWithTransactional() throws NoSuchMethodException {
        var method = TransactionalTestExample.class
            .getDeclaredMethod("transactionalMethod");

        when(testContext.getTestMethod()).thenReturn(method);

        assertDoesNotThrow(() -> {
            listener.beforeTestMethod(testContext);
        });
    }

    // Example class for testing @Transactional handling
    private static class TransactionalTestExample {
        @org.springframework.transaction.annotation.Transactional
        void transactionalMethod() {}
    }
}

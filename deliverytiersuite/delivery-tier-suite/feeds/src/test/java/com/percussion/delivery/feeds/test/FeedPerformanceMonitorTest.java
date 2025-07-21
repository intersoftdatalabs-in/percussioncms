// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

/**
 * Tests for JMX-based feed performance monitoring.
 */
class FeedPerformanceMonitorTest {
    private static final String TEST_MONITOR = "TestMonitor";
    private FeedPerformanceMonitor monitor;
    private MBeanServer mBeanServer;

    @BeforeEach
    void setUp() {
        monitor = FeedPerformanceMonitor.create(TEST_MONITOR)
            .orElseThrow(() -> new IllegalStateException("Failed to create monitor"));
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    @AfterEach
    void tearDown() throws Exception {
        var objectName = new javax.management.ObjectName(
            "com.percussion.feeds.test:type=FeedPerformance,name=" + TEST_MONITOR
        );
        if (mBeanServer.isRegistered(objectName)) {
            mBeanServer.unregisterMBean(objectName);
        }
    }

    @Test
    @DisplayName("Should record execution times")
    void shouldRecordExecutionTimes() {
        monitor.recordExecution(Duration.ofMillis(100));
        monitor.recordExecution(Duration.ofMillis(200));

        assertAll(
            () -> assertEquals(2, monitor.getMetric("totalRequests")),
            () -> assertEquals(150, monitor.getMetric("averageResponseTime")),
            () -> assertEquals(200, monitor.getMetric("maxResponseTime"))
        );
    }

    @Test
    @DisplayName("Should track error count")
    void shouldTrackErrorCount() {
        monitor.recordError();
        monitor.recordError();

        assertEquals(2, monitor.getMetric("totalErrors"));
    }

    @Test
    @DisplayName("Should be visible via JMX")
    void shouldBeVisibleViaJmx() throws Exception {
        monitor.recordExecution(Duration.ofMillis(150));
        monitor.recordError();

        var objectName = new javax.management.ObjectName(
            "com.percussion.feeds.test:type=FeedPerformance,name=" + TEST_MONITOR
        );

        assertAll(
            () -> assertTrue(mBeanServer.isRegistered(objectName)),
            () -> assertEquals(1L, mBeanServer.getAttribute(objectName, "totalRequests")),
            () -> assertEquals(1L, mBeanServer.getAttribute(objectName, "totalErrors")),
            () -> assertEquals(150L, mBeanServer.getAttribute(objectName, "averageResponseTime"))
        );
    }

    @Test
    @DisplayName("Should handle concurrent access")
    void shouldHandleConcurrentAccess() throws Exception {
        var threads = 10;
        var thread = new Thread[threads];

        // Create and start threads
        for (var i = 0; i < threads; i++) {
            thread[i] = new Thread(() -> {
                for (var j = 0; j < 100; j++) {
                    monitor.recordExecution(Duration.ofMillis(j));
                    if (j % 10 == 0) {
                        monitor.recordError();
                    }
                }
            });
            thread[i].start();
        }

        // Wait for all threads
        for (var t : thread) {
            t.join();
        }

        assertAll(
            () -> assertEquals(1000, monitor.getMetric("totalRequests")),
            () -> assertEquals(99, monitor.getMetric("maxResponseTime")),
            () -> assertEquals(100, monitor.getMetric("totalErrors"))
        );
    }

    @Test
    @DisplayName("Should handle missing metrics gracefully")
    void shouldHandleMissingMetricsGracefully() {
        assertEquals(0, monitor.getMetric("nonexistentMetric"));
    }

    @Test
    @DisplayName("Should prevent duplicate registration")
    void shouldPreventDuplicateRegistration() {
        var duplicate = FeedPerformanceMonitor.create(TEST_MONITOR);
        assertTrue(duplicate.isEmpty(), "Should not allow duplicate registration");
    }

    @Test
    @DisplayName("Should expose MBean information")
    void shouldExposeMBeanInformation() throws Exception {
        var objectName = new javax.management.ObjectName(
            "com.percussion.feeds.test:type=FeedPerformance,name=" + TEST_MONITOR
        );
        var info = mBeanServer.getMBeanInfo(objectName);

        assertAll(
            () -> assertTrue(info.getAttributes().length > 0),
            () -> assertNotNull(info.getDescription()),
            () -> assertEquals(monitor.getClass().getName(), info.getClassName())
        );
    }
}

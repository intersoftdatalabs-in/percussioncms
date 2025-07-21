// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JMX-based performance monitoring utility for feed tests.
 * Enables real-time monitoring of test performance metrics.
 */
public final class FeedPerformanceMonitor implements DynamicMBean {
    private static final String DOMAIN = "com.percussion.feeds.test";
    private final Map<String, AtomicLong> metrics = new HashMap<>();
    private final Map<String, String> descriptions = new HashMap<>();
    private final ObjectName objectName;

    private FeedPerformanceMonitor(String name) throws JMException {
        this.objectName = new ObjectName(DOMAIN + ":type=FeedPerformance,name=" + name);
        registerMetric("totalRequests", "Total number of feed requests");
        registerMetric("averageResponseTime", "Average response time in milliseconds");
        registerMetric("maxResponseTime", "Maximum response time in milliseconds");
        registerMetric("totalErrors", "Total number of feed generation errors");

        ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
    }

    /**
     * Creates and registers a new performance monitor.
     *
     * @param name Monitor name
     * @return Optional containing the monitor or empty if registration failed
     */
    public static Optional<FeedPerformanceMonitor> create(String name) {
        try {
            return Optional.of(new FeedPerformanceMonitor(name));
        } catch (JMException e) {
            return Optional.empty();
        }
    }

    /**
     * Records execution time for a feed operation.
     *
     * @param duration Operation duration
     */
    public void recordExecution(Duration duration) {
        metrics.get("totalRequests").incrementAndGet();
        var timeMs = duration.toMillis();
        updateAverage(timeMs);
        updateMax(timeMs);
    }

    /**
     * Records an error during feed generation.
     */
    public void recordError() {
        metrics.get("totalErrors").incrementAndGet();
    }

    /**
     * Gets current value of a metric.
     *
     * @param name Metric name
     * @return Current value
     */
    public long getMetric(String name) {
        return Optional.ofNullable(metrics.get(name))
            .map(AtomicLong::get)
            .orElse(0L);
    }

    @Override
    public Object getAttribute(String attribute) {
        return getMetric(attribute);
    }

    @Override
    public void setAttribute(Attribute attribute) {
        // Read-only metrics
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        var list = new AttributeList();
        for (String name : attributes) {
            list.add(new Attribute(name, getMetric(name)));
        }
        return list;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList(); // Read-only metrics
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {
        return null; // No operations supported
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        var attributes = metrics.entrySet().stream()
            .map(e -> new MBeanAttributeInfo(
                e.getKey(),
                "long",
                descriptions.get(e.getKey()),
                true,   // readable
                false,  // not writable
                false   // not boolean
            ))
            .toArray(MBeanAttributeInfo[]::new);

        return new MBeanInfo(
            getClass().getName(),
            "Feed Performance Monitor",
            attributes,
            null,  // constructors
            null,  // operations
            null   // notifications
        );
    }

    private void registerMetric(String name, String description) {
        metrics.put(name, new AtomicLong(0));
        descriptions.put(name, description);
    }

    private void updateAverage(long timeMs) {
        var total = metrics.get("totalRequests").get();
        var current = metrics.get("averageResponseTime").get();
        var newAvg = (current * (total - 1) + timeMs) / total;
        metrics.get("averageResponseTime").set(newAvg);
    }

    private void updateMax(long timeMs) {
        metrics.get("maxResponseTime").updateAndGet(
            current -> Math.max(current, timeMs)
        );
    }
}

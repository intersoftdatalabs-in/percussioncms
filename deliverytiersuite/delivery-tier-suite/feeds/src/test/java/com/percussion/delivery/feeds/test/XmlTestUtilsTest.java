// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XML validation and performance utilities.
 */
class XmlTestUtilsTest {
    private static final String VALID_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<feed><title>Test Feed</title></feed>";

    private static final String INVALID_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<feed><unclosed>Test</feed>";

    private static final String XXE_ATTACK_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" +
        "<feed>&xxe;</feed>";

    @Test
    @DisplayName("Should validate well-formed XML")
    void shouldValidateWellFormedXml() {
        var doc = XmlTestUtils.parseXmlSafely(VALID_XML);
        assertTrue(doc.isPresent(), "Valid XML should be parsed");
    }

    @Test
    @DisplayName("Should reject malformed XML")
    void shouldRejectMalformedXml() {
        var doc = XmlTestUtils.parseXmlSafely(INVALID_XML);
        assertTrue(doc.isEmpty(), "Invalid XML should not be parsed");
    }

    @Test
    @DisplayName("Should prevent XXE attacks")
    void shouldPreventXxeAttacks() {
        var doc = XmlTestUtils.parseXmlSafely(XXE_ATTACK_XML);
        assertTrue(doc.isEmpty(), "XXE attack should be blocked");
    }

    @Test
    @DisplayName("Should measure execution time")
    void shouldMeasureExecutionTime() {
        var duration = XmlTestUtils.measureExecutionTime(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(duration.toMillis() >= 100,
            "Duration should be at least 100ms");
    }

    @Test
    @DisplayName("Should measure execution with result")
    void shouldMeasureExecutionWithResult() {
        var result = XmlTestUtils.measureExecutionTimeWithResult(() -> {
            try {
                Thread.sleep(50);
                return "test-result";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        });

        assertAll(
            () -> assertEquals("test-result", result.result()),
            () -> assertTrue(result.duration().toMillis() >= 50,
                "Duration should be at least 50ms"),
            () -> assertTrue(result.getMillis() >= 50,
                "Millis should be at least 50")
        );
    }
}

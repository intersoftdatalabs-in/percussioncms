// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for schema-aware XML validation.
 */
class XmlSchemaValidatorTest {
    private static final String VALID_ATOM = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Test Feed</title>
            <link href="http://test.com/feed"/>
            <updated>2025-07-21T12:00:00Z</updated>
            <id>urn:uuid:1</id>
            <entry>
                <title>Test Entry</title>
                <link href="http://test.com/entry/1"/>
                <id>urn:uuid:2</id>
                <updated>2025-07-21T12:00:00Z</updated>
            </entry>
        </feed>
        """;

    private static final String INVALID_ATOM = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Invalid Feed</title>
            <!-- Missing required elements -->
        </feed>
        """;

    @Test
    @DisplayName("Should validate valid ATOM feed")
    void shouldValidateValidAtomFeed() {
        var error = XmlSchemaValidator.validateXml(VALID_ATOM, "atom-feed.xsd");
        assertTrue(error.isEmpty(), "Valid ATOM feed should pass validation");
    }

    @Test
    @DisplayName("Should detect invalid ATOM feed")
    void shouldDetectInvalidAtomFeed() {
        var error = XmlSchemaValidator.validateXml(INVALID_ATOM, "atom-feed.xsd");
        assertTrue(error.isPresent(), "Invalid ATOM feed should fail validation");
    }

    @Test
    @DisplayName("Should validate against multiple schemas")
    void shouldValidateAgainstMultipleSchemas() {
        var schemas = List.of("atom-feed.xsd", "atom-feed.xsd");
        var errors = XmlSchemaValidator.validateAgainstMultipleSchemas(
            INVALID_ATOM, schemas
        );

        assertEquals(2, errors.size(), "Should have errors for both schemas");
    }

    @Test
    @DisplayName("Should retry validation on failure")
    void shouldRetryValidationOnFailure() {
        var attempts = new AtomicInteger(0);
        var xmlSupplier = () -> {
            attempts.incrementAndGet();
            return attempts.get() < 3 ? INVALID_ATOM : VALID_ATOM;
        };

        var error = XmlSchemaValidator.validateWithRetry(
            xmlSupplier, "atom-feed.xsd", 5
        );

        assertAll(
            () -> assertTrue(error.isEmpty(), "Should eventually succeed"),
            () -> assertEquals(3, attempts.get(), "Should take 3 attempts")
        );
    }

    @Test
    @DisplayName("Should provide namespace URIs")
    void shouldProvideNamespaceUris() {
        var atomUri = XmlSchemaValidator.getNamespaceUri("atom");
        var rssUri = XmlSchemaValidator.getNamespaceUri("rss");
        var unknownUri = XmlSchemaValidator.getNamespaceUri("unknown");

        assertAll(
            () -> assertTrue(atomUri.isPresent(), "Should find ATOM namespace"),
            () -> assertTrue(rssUri.isPresent(), "Should find RSS namespace"),
            () -> assertTrue(unknownUri.isEmpty(), "Should handle unknown namespace")
        );
    }

    @Test
    @DisplayName("Should handle missing schema")
    void shouldHandleMissingSchema() {
        var error = XmlSchemaValidator.validateXml(VALID_ATOM, "missing.xsd");
        assertTrue(error.isPresent(), "Should handle missing schema gracefully");
    }
}

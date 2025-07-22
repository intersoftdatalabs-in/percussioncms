// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.StringReader;
import org.xml.sax.InputSource;

/**
 * Tests for XML comparison utilities.
 */
class XmlCompareUtilsTest {
    private static final String BASE_XML = """
        <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Test Feed</title>
            <link href="http://test.com/feed"/>
            <entry>
                <title>Entry 1</title>
                <link href="http://test.com/1"/>
                <id>urn:uuid:1</id>
                <updated>2025-07-21T12:00:00Z</updated>
            </entry>
        </feed>
        """;

    private Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    @Test
    @DisplayName("Should detect identical XML")
    void shouldDetectIdenticalXml() throws Exception {
        var doc1 = parseXml(BASE_XML);
        var doc2 = parseXml(BASE_XML);

        var diff = XmlCompareUtils.compareXml(doc1, doc2);
        assertTrue(diff.isEmpty(), "Documents should be identical");
    }

    @Test
    @DisplayName("Should detect XML differences")
    void shouldDetectXmlDifferences() throws Exception {
        var doc1 = parseXml(BASE_XML);
        var doc2 = parseXml(BASE_XML.replace("Entry 1", "Entry 2"));

        var diff = XmlCompareUtils.compareXml(doc1, doc2);
        assertTrue(diff.isPresent(), "Should detect title difference");
    }

    @Test
    @DisplayName("Should find node in document")
    void shouldFindNodeInDocument() throws Exception {
        var container = parseXml(BASE_XML);
        var entry = parseXml("""
            <entry>
                <title>Entry 1</title>
                <link href="http://test.com/1"/>
            </entry>
            """);

        assertTrue(XmlCompareUtils.containsNode(container, entry.getDocumentElement()),
            "Should find entry node");
    }

    @Test
    @DisplayName("Should validate feed entry")
    void shouldValidateFeedEntry() throws Exception {
        var validEntry = parseXml("""
            <entry>
                <title>Test Entry</title>
                <link href="http://test.com"/>
                <id>urn:uuid:1</id>
                <updated>2025-07-21T12:00:00Z</updated>
            </entry>
            """);

        var invalidEntry = parseXml("""
            <entry>
                <title>Invalid Entry</title>
            </entry>
            """);

        assertAll(
            () -> assertTrue(XmlCompareUtils.validateFeedEntry(validEntry).isEmpty(),
                "Valid entry should pass validation"),
            () -> assertTrue(XmlCompareUtils.validateFeedEntry(invalidEntry).isPresent(),
                "Invalid entry should fail validation")
        );
    }

    @Test
    @DisplayName("Should ignore whitespace and comments")
    void shouldIgnoreWhitespaceAndComments() throws Exception {
        var doc1 = parseXml(BASE_XML);
        var doc2 = parseXml(BASE_XML.replace("\n", "")
            .replace("    ", "")
            .replace("\t", ""));

        var diff = XmlCompareUtils.compareXml(doc1, doc2);
        assertTrue(diff.isEmpty(), "Should ignore whitespace differences");
    }
}

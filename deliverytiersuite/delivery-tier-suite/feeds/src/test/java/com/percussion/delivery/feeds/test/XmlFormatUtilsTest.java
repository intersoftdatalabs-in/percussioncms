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
 * Tests for XML formatting utilities.
 */
class XmlFormatUtilsTest {
    private static final String TEST_XML = """
        <feed xmlns="http://www.w3.org/2005/Atom">
            <title>Test Feed</title>
            <link href="http://test.com/feed"/>
        </feed>
        """;

    private Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    @Test
    @DisplayName("Should format XML with proper indentation")
    void shouldFormatXmlWithProperIndentation() throws Exception {
        var doc = parseXml(TEST_XML);
        var formatted = XmlFormatUtils.formatXml(doc);

        assertTrue(formatted.isPresent(), "Should return formatted XML");
        assertTrue(formatted.get().contains("\n"), "Should contain line breaks");
        assertTrue(formatted.get().contains("  "), "Should contain indentation");
    }

    @Test
    @DisplayName("Should extract node text using XPath")
    void shouldExtractNodeTextUsingXPath() throws Exception {
        var doc = parseXml(TEST_XML);
        var title = XmlFormatUtils.getNodeText(doc, "//title");

        assertTrue(title.isPresent(), "Should find title node");
        assertEquals("Test Feed", title.get(), "Should extract correct title");
    }

    @Test
    @DisplayName("Should get attribute value")
    void shouldGetAttributeValue() throws Exception {
        var doc = parseXml(TEST_XML);
        var linkNode = doc.getElementsByTagName("link").item(0);
        var href = XmlFormatUtils.getAttributeValue(linkNode, "href");

        assertTrue(href.isPresent(), "Should find href attribute");
        assertEquals("http://test.com/feed", href.get(), "Should extract correct URL");
    }

    @Test
    @DisplayName("Should create valid test entry")
    void shouldCreateValidTestEntry() throws Exception {
        var entry = XmlFormatUtils.createTestEntry(
            "Test Title",
            "http://test.com/entry/1"
        );

        var doc = parseXml(entry);
        assertTrue(XmlFormatUtils.xmlContentMatches(
            doc, "//title", "Test Title"
        ), "Should contain correct title");

        var linkNode = doc.getElementsByTagName("link").item(0);
        var href = XmlFormatUtils.getAttributeValue(linkNode, "href");
        assertEquals("http://test.com/entry/1", href.orElse(""),
            "Should have correct link");
    }

    @Test
    @DisplayName("Should handle missing nodes gracefully")
    void shouldHandleMissingNodesGracefully() throws Exception {
        var doc = parseXml("<root><empty/></root>");

        assertAll(
            () -> assertTrue(XmlFormatUtils.getNodeText(doc, "//missing").isEmpty()),
            () -> assertTrue(XmlFormatUtils.getAttributeValue(
                doc.getElementsByTagName("empty").item(0),
                "missing"
            ).isEmpty())
        );
    }
}

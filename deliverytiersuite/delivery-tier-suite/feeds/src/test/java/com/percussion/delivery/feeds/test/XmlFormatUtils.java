// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Optional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * XML utility methods for feed testing.
 * Provides secure XML formatting and validation.
 */
public final class XmlFormatUtils {
    private static final TransformerFactory TRANSFORMER_FACTORY;

    static {
        TRANSFORMER_FACTORY = TransformerFactory.newInstance();
        try {
            // Secure XML processing
            TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            TRANSFORMER_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException e) {
            // Some implementations might not support these attributes
        }
    }

    private XmlFormatUtils() {
        // Utility class, no instantiation
    }

    /**
     * Formats XML node with proper indentation.
     *
     * @param node XML node to format
     * @return Optional containing formatted XML or empty if formatting fails
     */
    public static Optional<String> formatXml(Node node) {
        try {
            var transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            var writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return Optional.of(writer.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Extracts text content from an XML node, handling missing nodes gracefully.
     *
     * @param node XML node
     * @param xpath XPath expression to locate text
     * @return Optional containing text or empty if not found
     */
    public static Optional<String> getNodeText(Node node, String xpath) {
        try {
            var xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
            var xpathExpression = xpathFactory.newXPath();
            return Optional.ofNullable(
                xpathExpression.evaluate(xpath, node)
            ).filter(s -> !s.isEmpty());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Extracts attribute value from an XML node.
     *
     * @param node XML node
     * @param attributeName Attribute name
     * @return Optional containing attribute value or empty if not found
     */
    public static Optional<String> getAttributeValue(Node node, String attributeName) {
        return Optional.ofNullable(node.getAttributes())
            .map(attrs -> attrs.getNamedItem(attributeName))
            .map(Node::getNodeValue);
    }

    /**
     * Checks if XML content matches expected structure.
     *
     * @param actual Actual XML document
     * @param xpath XPath to element to check
     * @param expectedValue Expected value
     * @return true if values match
     */
    public static boolean xmlContentMatches(Document actual, String xpath, String expectedValue) {
        return getNodeText(actual, xpath)
            .map(value -> value.equals(expectedValue))
            .orElse(false);
    }

    /**
     * Creates a simple test feed entry.
     *
     * @param title Entry title
     * @param link Entry link
     * @return XML string for feed entry
     */
    public static String createTestEntry(String title, String link) {
        return String.format("""
            <entry xmlns="http://www.w3.org/2005/Atom">
                <title>%s</title>
                <link href="%s"/>
                <id>urn:uuid:%s</id>
                <updated>%s</updated>
            </entry>
            """,
            title,
            link,
            java.util.UUID.randomUUID(),
            java.time.Instant.now()
        );
    }
}

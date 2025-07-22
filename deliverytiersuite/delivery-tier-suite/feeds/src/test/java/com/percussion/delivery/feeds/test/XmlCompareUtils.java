// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.util.Optional;

/**
 * XML comparison utilities for feed testing.
 * Uses XMLUnit for semantic XML comparison.
 */
public final class XmlCompareUtils {

    private XmlCompareUtils() {
        // Utility class, no instantiation
    }

    /**
     * Compares two XML documents ignoring whitespace and comments.
     *
     * @param expected Expected XML document
     * @param actual Actual XML document
     * @return Optional containing difference description or empty if identical
     */
    public static Optional<String> compareXml(Document expected, Document actual) {
        var diff = DiffBuilder.compare(toSource(expected))
            .withTest(toSource(actual))
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
            .ignoreWhitespace()
            .ignoreComments()
            .build();

        return diff.hasDifferences() ?
            Optional.of(diff.toString()) :
            Optional.empty();
    }

    /**
     * Checks if actual XML contains expected node structure.
     *
     * @param container Container XML document
     * @param expected Expected node structure
     * @return true if expected structure is found
     */
    public static boolean containsNode(Document container, Node expected) {
        try {
            var xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
            var expression = createXPathFromNode(expected);
            var result = xpath.evaluate(expression, container,
                javax.xml.xpath.XPathConstants.BOOLEAN);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates feed entry structure.
     *
     * @param entry Feed entry document
     * @return Optional containing validation error or empty if valid
     */
    public static Optional<String> validateFeedEntry(Document entry) {
        var requiredElements = new String[]{"title", "id", "updated"};

        for (var element : requiredElements) {
            if (!hasElement(entry, element)) {
                return Optional.of("Missing required element: " + element);
            }
        }

        if (!hasLinkWithHref(entry)) {
            return Optional.of("Missing link element with href attribute");
        }

        return Optional.empty();
    }

    private static Source toSource(Document doc) {
        return new DOMSource(doc);
    }

    private static String createXPathFromNode(Node node) {
        var path = new StringBuilder();
        var current = node;

        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            path.insert(0, "/" + current.getNodeName());
            current = current.getParentNode();
        }

        return path.toString();
    }

    private static boolean hasElement(Document doc, String elementName) {
        return doc.getElementsByTagName(elementName).getLength() > 0;
    }

    private static boolean hasLinkWithHref(Document doc) {
        var links = doc.getElementsByTagName("link");
        for (var i = 0; i < links.getLength(); i++) {
            if (links.item(i).getAttributes().getNamedItem("href") != null) {
                return true;
            }
        }
        return false;
    }
}

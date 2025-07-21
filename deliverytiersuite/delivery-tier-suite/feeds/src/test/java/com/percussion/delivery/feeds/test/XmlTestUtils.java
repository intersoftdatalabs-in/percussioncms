// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * XML validation and performance testing utilities.
 * Thread-safe and secure XML processing with XXE prevention.
 */
public final class XmlTestUtils {
    private static final DocumentBuilderFactory SECURE_DOC_BUILDER_FACTORY;
    private static final SchemaFactory SECURE_SCHEMA_FACTORY;

    static {
        SECURE_DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
        SECURE_SCHEMA_FACTORY = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // Configure secure XML processing
        configureSecureXmlProcessing(SECURE_DOC_BUILDER_FACTORY);
        configureSecureXmlProcessing(SECURE_SCHEMA_FACTORY);
    }

    private XmlTestUtils() {
        // Utility class, no instantiation
    }

    /**
     * Validates XML against an XSD schema.
     *
     * @param xml XML content to validate
     * @param xsdPath Path to XSD schema
     * @return Optional containing validation error or empty if valid
     */
    public static Optional<String> validateXmlAgainstSchema(String xml, String xsdPath) {
        try {
            var schema = SECURE_SCHEMA_FACTORY.newSchema(
                XmlTestUtils.class.getClassLoader().getResource(xsdPath)
            );
            var validator = schema.newValidator();
            var source = new StreamSource(new StringReader(xml));
            validator.validate(source);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }
    }

    /**
     * Measures execution time of a code block.
     *
     * @param operation Code block to measure
     * @return Duration of execution
     */
    public static Duration measureExecutionTime(Runnable operation) {
        var start = Instant.now();
        operation.run();
        return Duration.between(start, Instant.now());
    }

    /**
     * Measures execution time of a code block returning a value.
     *
     * @param operation Code block to measure
     * @return Tuple of result and duration
     */
    public static <T> ExecutionResult<T> measureExecutionTimeWithResult(Supplier<T> operation) {
        var start = Instant.now();
        var result = operation.get();
        var duration = Duration.between(start, Instant.now());
        return new ExecutionResult<>(result, duration);
    }

    /**
     * Parses XML string into DOM Document with secure settings.
     *
     * @param xml XML string to parse
     * @return Optional containing parsed document or empty if invalid
     */
    public static Optional<Document> parseXmlSafely(String xml) {
        try {
            var builder = SECURE_DOC_BUILDER_FACTORY.newDocumentBuilder();
            return Optional.ofNullable(
                builder.parse(new InputSource(new StringReader(xml)))
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static void configureSecureXmlProcessing(Object factory) {
        try {
            // Disable external entities
            setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);

            if (factory instanceof DocumentBuilderFactory dbf) {
                dbf.setXIncludeAware(false);
                dbf.setExpandEntityReferences(false);
            }
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Failed to configure secure XML processing", e);
        }
    }

    private static void setFeature(Object factory, String feature, boolean value)
            throws ParserConfigurationException {
        if (factory instanceof DocumentBuilderFactory dbf) {
            dbf.setFeature(feature, value);
        } else if (factory instanceof SchemaFactory sf) {
            try {
                sf.setFeature(feature, value);
            } catch (SAXException e) {
                throw new ParserConfigurationException("Failed to set SchemaFactory feature");
            }
        }
    }

    /**
     * Container for execution result and timing.
     */
    public static record ExecutionResult<T>(T result, Duration duration) {
        /**
         * Gets execution time in milliseconds.
         * @return Duration in milliseconds
         */
        public long getMillis() {
            return duration.toMillis();
        }
    }
}

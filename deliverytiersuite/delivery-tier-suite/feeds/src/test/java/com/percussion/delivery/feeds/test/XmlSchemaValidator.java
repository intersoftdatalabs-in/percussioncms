// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Schema-aware XML validation for feed testing.
 * Thread-safe schema caching and validation.
 */
public final class XmlSchemaValidator {
    private static final Map<String, javax.xml.validation.Schema> SCHEMA_CACHE =
        new ConcurrentHashMap<>();

    private static final Map<String, String> NAMESPACE_MAPPINGS = Map.of(
        "atom", "http://www.w3.org/2005/Atom",
        "rss", "http://purl.org/rss/1.0/",
        "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    );

    private XmlSchemaValidator() {
        // Utility class, no instantiation
    }

    /**
     * Validates XML against a cached schema.
     *
     * @param xml XML content to validate
     * @param schemaPath Path to XSD schema
     * @return Optional containing error message or empty if valid
     */
    public static Optional<String> validateXml(String xml, String schemaPath) {
        try {
            var schema = getOrLoadSchema(schemaPath);
            var validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }
    }

    /**
     * Validates XML against multiple schemas.
     *
     * @param xml XML content to validate
     * @param schemaPaths List of schema paths
     * @return Map of schema paths to error messages, empty if all valid
     */
    public static Map<String, String> validateAgainstMultipleSchemas(
            String xml,
            Iterable<String> schemaPaths) {
        var errors = new HashMap<String, String>();
        for (var path : schemaPaths) {
            validateXml(xml, path)
                .ifPresent(error -> errors.put(path, error));
        }
        return errors;
    }

    /**
     * Validates XML with retry on failure.
     *
     * @param xmlSupplier XML content supplier
     * @param schemaPath Schema path
     * @param maxAttempts Maximum retry attempts
     * @return Optional containing final error or empty if eventually valid
     */
    public static Optional<String> validateWithRetry(
            Supplier<String> xmlSupplier,
            String schemaPath,
            int maxAttempts) {
        var attempts = 0;
        Optional<String> lastError;

        do {
            lastError = validateXml(xmlSupplier.get(), schemaPath);
            attempts++;
        } while (lastError.isPresent() && attempts < maxAttempts);

        return lastError;
    }

    /**
     * Gets namespace URI for prefix.
     *
     * @param prefix Namespace prefix
     * @return Optional containing namespace URI
     */
    public static Optional<String> getNamespaceUri(String prefix) {
        return Optional.ofNullable(NAMESPACE_MAPPINGS.get(prefix));
    }

    private static javax.xml.validation.Schema getOrLoadSchema(String schemaPath)
            throws Exception {
        return SCHEMA_CACHE.computeIfAbsent(schemaPath, path -> {
            try {
                var factory = SchemaFactory.newInstance(
                    javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
                );
                factory.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

                var schemaUrl = XmlSchemaValidator.class
                    .getClassLoader()
                    .getResource(path);

                if (schemaUrl == null) {
                    throw new IllegalArgumentException("Schema not found: " + path);
                }

                return factory.newSchema(schemaUrl);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load schema: " + path, e);
            }
        });
    }
}

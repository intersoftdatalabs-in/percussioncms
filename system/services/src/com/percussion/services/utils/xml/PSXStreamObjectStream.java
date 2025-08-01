/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.utils.xml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern Java 11 implementation of {@link PSObjectStream} that uses XStream for XML serialization.
 * This implementation provides secure, efficient XML-based object streaming with enhanced
 * performance and type safety.
 * <p>
 * Key features include:
 * <ul>
 *   <li>Secure XStream configuration with proper type permissions</li>
 *   <li>Modern Java 11 features for enhanced performance</li>
 *   <li>Thread-safe XStream instance management</li>
 *   <li>Comprehensive error handling and logging</li>
 * </ul>
 *
 * @author adamgent
 * @param <T> the type of objects to stream
 */
public class PSXStreamObjectStream<T> extends PSObjectStream<T> {

    private static final Logger ms_log = LogManager.getLogger(PSXStreamObjectStream.class);

    /**
     * Cache for configured XStream instances by class type
     */
    private static final Map<Class<?>, XStream> XSTREAM_CACHE = new ConcurrentHashMap<>();

    /**
     * Default XStream instance for general use
     */
    private static final XStream DEFAULT_XSTREAM = createSecureXStream();

    /**
     * XStream instance for this object stream
     */
    private final XStream xstream;

    /**
     * The class type for objects in this stream
     */
    private final Class<T> objectClass;

    /**
     * Buffer for XML content
     */
    private final ByteArrayOutputStream xmlBuffer = new ByteArrayOutputStream();

    /**
     * Default constructor using the default XStream configuration
     */
    public PSXStreamObjectStream() {
        this(null, DEFAULT_XSTREAM);
    }

    /**
     * Constructor with specific object class for optimized XStream configuration
     *
     * @param objectClass the class of objects to stream, may be {@code null}
     */
    public PSXStreamObjectStream(Class<T> objectClass) {
        this(objectClass, getOrCreateXStreamForClass(objectClass));
    }

    /**
     * Constructor with custom XStream instance
     *
     * @param objectClass the class of objects to stream, may be {@code null}
     * @param xstream the XStream instance to use, not {@code null}
     */
    public PSXStreamObjectStream(Class<T> objectClass, XStream xstream) {
        this.objectClass = objectClass;
        this.xstream = Objects.requireNonNull(xstream, "XStream instance cannot be null");

        ms_log.debug("Created PSXStreamObjectStream for class: {}",
            objectClass != null ? objectClass.getName() : "generic");
    }

    @Override
    protected OutputStream createOutputStream() throws IOException {
        xmlBuffer.reset();
        return new BufferedOutputStream(xmlBuffer);
    }

    @Override
    protected InputStream createInputStream() throws IOException {
        var xmlContent = xmlBuffer.toByteArray();
        if (xmlContent.length == 0) {
            ms_log.warn("No XML content available for reading");
            return new ByteArrayInputStream(new byte[0]);
        }

        ms_log.debug("Creating input stream from {} bytes of XML content", xmlContent.length);
        return new BufferedInputStream(new ByteArrayInputStream(xmlContent));
    }

    @Override
    protected void writeObject(T object, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(object, "Object to write cannot be null");
        Objects.requireNonNull(outputStream, "Output stream cannot be null");

        try {
            var xml = xstream.toXML(object);
            var xmlBytes = xml.getBytes(StandardCharsets.UTF_8);

            // Write object delimiter and length for proper separation
            writeObjectMarker(outputStream, xmlBytes.length);
            outputStream.write(xmlBytes);
            outputStream.flush();

            ms_log.trace("Wrote object of type {} ({} bytes)",
                object.getClass().getSimpleName(), xmlBytes.length);

        } catch (Exception e) {
            ms_log.error("Failed to serialize object of type {}: {}",
                object.getClass().getName(), e.getMessage(), e);
            throw new IOException("Failed to serialize object to XML", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T readObject(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "Input stream cannot be null");

        try {
            var objectLength = readObjectMarker(inputStream);
            if (objectLength == -1) {
                return null; // End of stream
            }

            var xmlBytes = inputStream.readNBytes(objectLength);
            if (xmlBytes.length != objectLength) {
                throw new IOException(
                    String.format("Expected %d bytes but read %d", objectLength, xmlBytes.length));
            }

            var xml = new String(xmlBytes, StandardCharsets.UTF_8);
            var object = (T) xstream.fromXML(xml);

            ms_log.trace("Read object of type {} ({} bytes)",
                object.getClass().getSimpleName(), xmlBytes.length);

            return object;

        } catch (IOException e) {
            throw e; // Re-throw IO exceptions as-is
        } catch (Exception e) {
            ms_log.error("Failed to deserialize object from XML: {}", e.getMessage(), e);
            throw new IOException("Failed to deserialize object from XML", e);
        }
    }

    /**
     * Write an object marker (length) to the stream
     *
     * @param outputStream the output stream
     * @param length the length of the object data
     * @throws IOException if writing fails
     */
    private void writeObjectMarker(OutputStream outputStream, int length) throws IOException {
        var lengthBytes = new byte[4];
        lengthBytes[0] = (byte) (length >>> 24);
        lengthBytes[1] = (byte) (length >>> 16);
        lengthBytes[2] = (byte) (length >>> 8);
        lengthBytes[3] = (byte) length;
        outputStream.write(lengthBytes);
    }

    /**
     * Read an object marker (length) from the stream
     *
     * @param inputStream the input stream
     * @return the length of the next object, or -1 if end of stream
     * @throws IOException if reading fails
     */
    private int readObjectMarker(InputStream inputStream) throws IOException {
        var lengthBytes = inputStream.readNBytes(4);
        if (lengthBytes.length == 0) {
            return -1; // End of stream
        }

        if (lengthBytes.length != 4) {
            throw new IOException("Incomplete object marker");
        }

        return ((lengthBytes[0] & 0xFF) << 24) |
               ((lengthBytes[1] & 0xFF) << 16) |
               ((lengthBytes[2] & 0xFF) << 8) |
               (lengthBytes[3] & 0xFF);
    }

    /**
     * Get or create a cached XStream instance for a specific class
     *
     * @param clazz the class to get XStream for, may be {@code null}
     * @return a configured XStream instance
     */
    private static XStream getOrCreateXStreamForClass(Class<?> clazz) {
        if (clazz == null) {
            return DEFAULT_XSTREAM;
        }

        return XSTREAM_CACHE.computeIfAbsent(clazz, PSXStreamObjectStream::createXStreamForClass);
    }

    /**
     * Create a new XStream instance configured for a specific class
     *
     * @param clazz the class to configure for
     * @return a new configured XStream instance
     */
    private static XStream createXStreamForClass(Class<?> clazz) {
        var xstream = createSecureXStream();

        // Allow the specific class and common collection types
        xstream.allowTypes(new Class[]{clazz});

        // Configure aliases for cleaner XML
        var simpleName = clazz.getSimpleName();
        xstream.alias(simpleName.toLowerCase(), clazz);

        ms_log.debug("Created optimized XStream instance for class: {}", clazz.getName());
        return xstream;
    }

    /**
     * Create a secure XStream instance with proper security configuration
     *
     * @return a new secure XStream instance
     */
    private static XStream createSecureXStream() {
        var xstream = new XStream();

        // Initialize security framework
        initSecurityFramework(xstream);

        ms_log.debug("Created secure XStream instance");
        return xstream;
    }

    /**
     * Initialize the XStream security framework with safe defaults
     *
     * @param xstream the XStream instance to configure
     */
    private static void initSecurityFramework(XStream xstream) {
        // Clear all permissions first for security
        xstream.addPermission(NoTypePermission.NONE);

        // Allow primitive types and null values
        xstream.addPermission(NullPermission.NULL);
        xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);

        // Allow common collection types
        xstream.allowTypesByWildcard(new String[]{
            "java.util.**",
            "java.lang.**",
            "java.time.**"
        });

        // Allow CGLIB proxies if present
        try {
            xstream.addPermission(new CGLIBProxyTypePermission());
        } catch (NoClassDefFoundError e) {
            ms_log.debug("CGLIB not available, skipping proxy permission");
        }

        ms_log.debug("Initialized XStream security framework");
    }

    /**
     * Configure XStream to allow specific types
     *
     * @param types the types to allow
     */
    public void allowTypes(Class<?>... types) {
        Objects.requireNonNull(types, "Types array cannot be null");

        xstream.allowTypes(types);
        ms_log.debug("Added type permissions for {} classes", types.length);
    }

    /**
     * Configure XStream to allow types by wildcard patterns
     *
     * @param patterns the wildcard patterns to allow
     */
    public void allowTypesByWildcard(String... patterns) {
        Objects.requireNonNull(patterns, "Patterns array cannot be null");

        xstream.allowTypesByWildcard(patterns);
        ms_log.debug("Added wildcard type permissions for {} patterns", patterns.length);
    }

    /**
     * Create an alias for a class to produce cleaner XML
     *
     * @param alias the alias name
     * @param clazz the class to alias
     */
    public void alias(String alias, Class<?> clazz) {
        Objects.requireNonNull(alias, "Alias cannot be null");
        Objects.requireNonNull(clazz, "Class cannot be null");

        xstream.alias(alias, clazz);
        ms_log.debug("Created alias '{}' for class: {}", alias, clazz.getName());
    }

    /**
     * Get the object class for this stream
     *
     * @return the object class, may be {@code null}
     */
    public Optional<Class<T>> getObjectClass() {
        return Optional.ofNullable(objectClass);
    }

    /**
     * Get the size of the XML buffer
     *
     * @return the buffer size in bytes
     */
    public int getXmlBufferSize() {
        return xmlBuffer.size();
    }

    /**
     * Clear the XStream cache - useful for testing or memory management
     */
    public static void clearXStreamCache() {
        XSTREAM_CACHE.clear();
        ms_log.info("Cleared XStream instance cache");
    }

    /**
     * Get statistics about the XStream cache
     *
     * @return a map containing cache statistics
     */
    public static Map<String, Object> getXStreamCacheStats() {
        var stats = new HashMap<String, Object>();
        stats.put("cacheSize", XSTREAM_CACHE.size());
        stats.put("cachedClasses", XSTREAM_CACHE.keySet().stream()
            .map(Class::getName)
            .sorted()
            .toList());

        return Collections.unmodifiableMap(stats);
    }

    @Override
    public Map<String, Object> getStatistics() {
        var stats = new HashMap<>(super.getStatistics());
        stats.put("objectClass", objectClass != null ? objectClass.getName() : "generic");
        stats.put("xmlBufferSize", getXmlBufferSize());
        stats.put("xstreamCacheSize", XSTREAM_CACHE.size());

        return Collections.unmodifiableMap(stats);
    }

    @Override
    public String toString() {
        return String.format("PSXStreamObjectStream[class=%s, state=%s, size=%d, bufferSize=%d]",
            objectClass != null ? objectClass.getSimpleName() : "generic",
            getState(), size(), getXmlBufferSize());
    }
}

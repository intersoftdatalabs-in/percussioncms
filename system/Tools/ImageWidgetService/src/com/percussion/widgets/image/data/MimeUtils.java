/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.widgets.image.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities for handling MIME types and image format detection.
 * This class provides thread-safe caching and supports various image formats.
 *
 * @since Java 11
 */
public final class MimeUtils {

    private static final Logger log = LogManager.getLogger(MimeUtils.class);

    /** Properties file location for MIME type mappings */
    private static final String MIME_TYPES_PROPERTIES =
            "com/percussion/widgets/image/image-mime-types.properties";

    /** Cache for loaded MIME type properties */
    private static final Map<String, String> mimeTypeCache = new ConcurrentHashMap<>();

    /** Set of supported MIME types from ImageIO */
    private static volatile Set<String> supportedMimeTypes;

    /** Lock object for thread-safe initialization */
    private static final Object INIT_LOCK = new Object();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets all supported MIME types from ImageIO.
     * Results are cached for performance.
     *
     * @return array of supported MIME types, never {@code null}
     */
    public static String[] getSupportedMimeTypes() {
        if (supportedMimeTypes == null) {
            synchronized (INIT_LOCK) {
                if (supportedMimeTypes == null) {
                    ImageIO.scanForPlugins();
                    var mimeTypes = ImageIO.getReaderMIMETypes();
                    supportedMimeTypes = Set.of(mimeTypes != null ? mimeTypes : new String[0]);
                }
            }
        }
        return supportedMimeTypes.toArray(String[]::new);
    }

    /**
     * Gets supported MIME types as an unmodifiable Set.
     *
     * @return unmodifiable set of supported MIME types
     */
    public static Set<String> getSupportedMimeTypesAsSet() {
        getSupportedMimeTypes(); // Ensure initialization
        return Collections.unmodifiableSet(supportedMimeTypes);
    }

    /**
     * Checks if a MIME type is supported for image processing.
     *
     * @param mimeType the MIME type to check
     * @return {@code true} if the MIME type is supported, {@code false} otherwise
     */
    public static boolean isSupportedMimeType(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return false;
        }
        return getSupportedMimeTypesAsSet().contains(mimeType.toLowerCase());
    }

    /**
     * Gets the MIME type for a file extension.
     * Results are cached for performance.
     *
     * @param ext the file extension (with or without leading dot)
     * @return the MIME type for the extension, or {@code null} if not found
     */
    public static String getMimeTypeByExtension(String ext) {
        if (StringUtils.isBlank(ext)) {
            return null;
        }

        var normalizedExt = normalizeExtension(ext);

        // Check cache first
        var cachedMimeType = mimeTypeCache.get(normalizedExt);
        if (cachedMimeType != null) {
            return cachedMimeType;
        }

        // Load from properties if not cached
        return loadMimeTypeFromProperties(normalizedExt);
    }

    /**
     * Gets the MIME type for a file extension as an Optional.
     *
     * @param ext the file extension
     * @return Optional containing the MIME type, or empty if not found
     */
    public static Optional<String> getMimeTypeByExtensionOptional(String ext) {
        return Optional.ofNullable(getMimeTypeByExtension(ext));
    }

    /**
     * Gets the file extension for a MIME type.
     *
     * @param mimeType the MIME type
     * @return the file extension (without leading dot), or {@code null} if not found
     */
    public static String getExtensionByMimeType(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return null;
        }

        ensureMimeTypesLoaded();

        return mimeTypeCache.entrySet().stream()
                .filter(entry -> mimeType.equalsIgnoreCase(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the file extension for a MIME type as an Optional.
     *
     * @param mimeType the MIME type
     * @return Optional containing the file extension, or empty if not found
     */
    public static Optional<String> getExtensionByMimeTypeOptional(String mimeType) {
        return Optional.ofNullable(getExtensionByMimeType(mimeType));
    }

    /**
     * Checks if a file extension is supported for image processing.
     *
     * @param ext the file extension
     * @return {@code true} if the extension is supported, {@code false} otherwise
     */
    public static boolean isSupportedExtension(String ext) {
        return getMimeTypeByExtensionOptional(ext)
                .map(MimeUtils::isSupportedMimeType)
                .orElse(false);
    }

    /**
     * Normalizes a file extension by removing leading dots and converting to lowercase.
     *
     * @param ext the extension to normalize
     * @return normalized extension
     */
    private static String normalizeExtension(String ext) {
        return ext.replace(".", "").toLowerCase().trim();
    }

    /**
     * Loads MIME type from properties file and caches the result.
     *
     * @param normalizedExt the normalized extension
     * @return the MIME type, or {@code null} if not found
     */
    private static String loadMimeTypeFromProperties(String normalizedExt) {
        ensureMimeTypesLoaded();
        return mimeTypeCache.get(normalizedExt);
    }

    /**
     * Ensures that MIME type properties are loaded into cache.
     * This method is thread-safe and loads properties only once.
     */
    private static void ensureMimeTypesLoaded() {
        if (mimeTypeCache.isEmpty()) {
            synchronized (INIT_LOCK) {
                if (mimeTypeCache.isEmpty()) {
                    loadMimeTypeProperties();
                }
            }
        }
    }

    /**
     * Loads MIME type properties from the properties file.
     */
    private static void loadMimeTypeProperties() {
        var props = new Properties();

        try (var is = MimeUtils.class.getClassLoader().getResourceAsStream(MIME_TYPES_PROPERTIES)) {
            if (is == null) {
                log.warn("MIME types properties file not found: {}", MIME_TYPES_PROPERTIES);
                return;
            }

            props.load(is);

            // Load all properties into cache
            props.stringPropertyNames().forEach(key -> {
                var value = props.getProperty(key);
                if (StringUtils.isNotBlank(value)) {
                    mimeTypeCache.put(key.toLowerCase(), value.toLowerCase());
                }
            });

            log.debug("Loaded {} MIME type mappings from properties file", mimeTypeCache.size());

        } catch (IOException e) {
            log.error("Unable to load MIME types properties file: {}", MIME_TYPES_PROPERTIES, e);
        }
    }

    /**
     * Clears the MIME type cache. Primarily used for testing.
     */
    static void clearCache() {
        synchronized (INIT_LOCK) {
            mimeTypeCache.clear();
            supportedMimeTypes = null;
        }
    }

    /**
     * Gets the number of cached MIME type mappings.
     *
     * @return the cache size
     */
    public static int getCacheSize() {
        return mimeTypeCache.size();
    }

    /**
     * Gets all cached MIME type mappings as an unmodifiable map.
     *
     * @return unmodifiable map of extension to MIME type mappings
     */
    public static Map<String, String> getAllMimeTypeMappings() {
        ensureMimeTypesLoaded();
        return Collections.unmodifiableMap(new HashMap<>(mimeTypeCache));
    }
}

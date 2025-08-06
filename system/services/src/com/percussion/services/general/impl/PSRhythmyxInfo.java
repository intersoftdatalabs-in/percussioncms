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
package com.percussion.services.general.impl;

import com.percussion.server.PSServer;
import com.percussion.services.general.IPSRhythmyxInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern Java 11 implementation of {@link IPSRhythmyxInfo} interface.
 *
 * <p>This class provides thread-safe access to Rhythmyx server information with
 * lazy initialization of properties. It uses {@link ConcurrentHashMap} for optimal
 * concurrent access and includes comprehensive validation and error handling.
 *
 * <p>Properties are initialized lazily on first access and cached for subsequent
 * requests. Initial configuration can be provided through Spring configuration
 * using the {@link #setBindings(Map)} method.
 *
 * <p>This object is designed to be created as a singleton by the Spring framework
 * and provides thread-safe access to server configuration information throughout
 * the application lifecycle.
 *
 * @since Java 11 Modernization
 */
public final class PSRhythmyxInfo implements IPSRhythmyxInfo {

    /**
     * Thread-safe map of Rhythmyx information properties.
     * Uses ConcurrentHashMap for optimal concurrent access without synchronization.
     */
    private final Map<Key, Object> propertyMap = new ConcurrentHashMap<>();

    /**
     * Sets initial property bindings from Spring configuration.
     *
     * <p>This method is called by the Spring framework during bean initialization
     * to provide initial values for server properties. It supports setting values
     * for properties that can be determined at startup time.
     *
     * <p><strong>Note:</strong> This method should only be called by the Spring
     * configuration framework, never call it directly from application code.
     *
     * @param initialData initial property values map, where keys are string representations
     *                   of {@link Key} enum values and values are string representations
     *                   of the property values, may be null or empty
     */
    public void setBindings(Map<String, String> initialData) {
        if (initialData == null || initialData.isEmpty()) {
            return;
        }

        // Process unit testing flag
        Optional.ofNullable(initialData.get(Key.UNIT_TESTING.toString()))
                .filter(value -> !value.trim().isEmpty())
                .ifPresent(value -> {
                    try {
                        propertyMap.put(Key.UNIT_TESTING, Boolean.parseBoolean(value));
                    } catch (Exception e) {
                        // Log warning but don't fail - default to false
                        propertyMap.put(Key.UNIT_TESTING, false);
                    }
                });

        // Process root directory
        Optional.ofNullable(initialData.get(Key.ROOT_DIRECTORY.toString()))
                .filter(value -> !value.trim().isEmpty())
                .ifPresent(value -> propertyMap.put(Key.ROOT_DIRECTORY, value.trim()));
    }

    @Override
    public Object getProperty(Key key) {
        Objects.requireNonNull(key, "Property key cannot be null");

        // Use computeIfAbsent for thread-safe lazy initialization
        return propertyMap.computeIfAbsent(key, this::initializeProperty);
    }

    /**
     * Initializes a property value based on the specified key.
     *
     * <p>This method handles lazy initialization of properties that are not
     * explicitly set during startup. It queries the {@link PSServer} for
     * runtime configuration values and caches them for subsequent access.
     *
     * @param key the property key to initialize, never null
     * @return the initialized property value, may be null for some properties
     */
    private Object initializeProperty(Key key) {
        try {
            switch (key) {
                case ROOT_DIRECTORY:
                    var rxDir = PSServer.getRxDir();
                    return rxDir != null ? rxDir.getAbsolutePath() : null;
                case LISTENER_PORT:
                    return PSServer.getListenerPort();
                case LISTENER_SSL_PORT:
                    return PSServer.getSslListenerPort();
                case VERSION:
                    return PSServer.getVersion();
                case UNIT_TESTING:
                    return false; // Default if not explicitly set
                default:
                    return null;
            }
        } catch (Exception e) {
            // Return null if property cannot be determined
            return null;
        }
    }

    /**
     * Gets all currently cached properties as an immutable map.
     *
     * <p>This method returns a snapshot of currently cached properties.
     * Properties that have not been accessed yet will not be included
     * in the returned map.
     *
     * @return an immutable map of cached properties, never null
     */
    public Map<Key, Object> getCachedProperties() {
        return Map.copyOf(propertyMap);
    }

    /**
     * Checks if a property has been cached (previously accessed).
     *
     * @param key the property key to check, must not be null
     * @return true if the property is cached, false otherwise
     * @throws IllegalArgumentException if key is null
     */
    public boolean isPropertyCached(Key key) {
        Objects.requireNonNull(key, "Property key cannot be null");
        return propertyMap.containsKey(key);
    }

    /**
     * Gets the number of currently cached properties.
     *
     * @return the count of cached properties
     */
    public int getCachedPropertyCount() {
        return propertyMap.size();
    }

    /**
     * Clears the property cache, forcing reinitialization on next access.
     *
     * <p><strong>Warning:</strong> This method should only be used for testing
     * purposes or when server configuration has changed and cached values
     * need to be refreshed.
     */
    public void clearCache() {
        propertyMap.clear();
    }

    /**
     * Refreshes a specific property by removing it from cache.
     *
     * <p>The property will be reinitialized on next access. This is useful
     * when a specific configuration value has changed and needs to be updated.
     *
     * @param key the property key to refresh, must not be null
     * @throws IllegalArgumentException if key is null
     */
    public void refreshProperty(Key key) {
        Objects.requireNonNull(key, "Property key cannot be null");
        propertyMap.remove(key);
    }

    @Override
    public String toString() {
        var cachedCount = getCachedPropertyCount();
        return String.format("PSRhythmyxInfo{cachedProperties=%d}", cachedCount);
    }
}

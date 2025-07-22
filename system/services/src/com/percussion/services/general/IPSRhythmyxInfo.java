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

package com.percussion.services.general;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Modern Java 11 interface for accessing Rhythmyx server information and configuration properties.
 *
 * <p>This interface provides a convenient mechanism to access server information such as:
 * <ul>
 *   <li>Installation root directory</li>
 *   <li>Server HTTP and HTTPS ports</li>
 *   <li>Version information</li>
 *   <li>Testing mode flags</li>
 * </ul>
 *
 * <p>All property access is type-safe through the {@link Key} enum and includes both traditional
 * and modern Java 11 access patterns with Optional support for null-safe operations.
 *
 * <p>Properties are initialized in the {@link com.percussion.server.PSServer} init method
 * and cached for efficient access throughout the application lifecycle.
 *
 * @since Java 11 Modernization
 */
public interface IPSRhythmyxInfo {

    /**
     * Modern Java 11 enum for Rhythmyx server property keys.
     *
     * <p>This enum defines the available server properties that can be accessed
     * through the {@link #getProperty(Key)} method. Each key represents a specific
     * aspect of the server configuration or runtime information.
     */
    enum Key {
        /**
         * The root directory where Rhythmyx is installed.
         * Returns: String representing the absolute path to the installation directory.
         */
        ROOT_DIRECTORY("Root installation directory"),

        /**
         * The HTTP listener port for the server.
         * Returns: Integer representing the port number for HTTP connections.
         */
        LISTENER_PORT("HTTP listener port"),

        /**
         * The HTTPS/SSL listener port for the server.
         * Returns: Integer representing the port number for HTTPS connections.
         */
        LISTENER_SSL_PORT("HTTPS/SSL listener port"),

        /**
         * The version information of the Rhythmyx server.
         * Returns: String representing the current server version.
         */
        VERSION("Server version information"),

        /**
         * Flag indicating whether the server is running in unit testing mode.
         * Returns: Boolean indicating if unit testing mode is enabled.
         */
        UNIT_TESTING("Unit testing mode flag");

        /**
         * Human-readable description of this property key.
         */
        private final String description;

        /**
         * Creates a new property key with the specified description.
         *
         * @param description human-readable description of the property
         */
        Key(String description) {
            this.description = description;
        }

        /**
         * Gets the human-readable description of this property key.
         *
         * @return the property's description, never null
         */
        public String getDescription() {
            return description;
        }

        /**
         * Checks if this key represents a port-related property.
         *
         * @return true if this key is for port configuration, false otherwise
         */
        public boolean isPortProperty() {
            return this == LISTENER_PORT || this == LISTENER_SSL_PORT;
        }

        /**
         * Checks if this key represents a directory-related property.
         *
         * @return true if this key is for directory configuration, false otherwise
         */
        public boolean isDirectoryProperty() {
            return this == ROOT_DIRECTORY;
        }

        /**
         * Checks if this key represents a boolean flag property.
         *
         * @return true if this key is for boolean configuration, false otherwise
         */
        public boolean isBooleanProperty() {
            return this == UNIT_TESTING;
        }

        /**
         * Gets all port-related property keys.
         *
         * @return a set of keys that represent port properties
         */
        public static Set<Key> getPortProperties() {
            return Set.of(LISTENER_PORT, LISTENER_SSL_PORT);
        }

        /**
         * Gets all directory-related property keys.
         *
         * @return a set of keys that represent directory properties
         */
        public static Set<Key> getDirectoryProperties() {
            return Set.of(ROOT_DIRECTORY);
        }

        /**
         * Gets all boolean flag property keys.
         *
         * @return a set of keys that represent boolean properties
         */
        public static Set<Key> getBooleanProperties() {
            return Set.of(UNIT_TESTING);
        }

        /**
         * Streams all available property keys for functional processing.
         *
         * @return a stream of all Key values
         */
        public static Stream<Key> stream() {
            return Stream.of(values());
        }

        /**
         * Finds a property key by its name, case-insensitive.
         *
         * @param name the key name to search for, must not be null
         * @return an Optional containing the matching key, or empty if not found
         * @throws IllegalArgumentException if name is null
         */
        public static Optional<Key> findByName(String name) {
            Objects.requireNonNull(name, "Key name cannot be null");
            return stream()
                    .filter(key -> key.name().equalsIgnoreCase(name.trim()))
                    .findFirst();
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", name(), description);
        }
    }

    /**
     * Gets the value of a Rhythmyx server property.
     *
     * <p>This method retrieves the current value of the specified server property.
     * Properties are typically initialized during server startup and cached for
     * efficient access throughout the application lifecycle.
     *
     * @param key the property key to retrieve, must not be null
     * @return the property's value, or null if not found or not yet initialized
     * @throws IllegalArgumentException if key is null
     */
    Object getProperty(Key key);

    /**
     * Safely gets the value of a server property with Optional wrapper.
     *
     * <p>This method provides null-safe access to server properties, returning
     * an empty Optional if the property is not found or not yet initialized.
     *
     * @param key the property key to retrieve, must not be null
     * @return an Optional containing the property's value, or empty if not found
     * @throws IllegalArgumentException if key is null
     */
    default Optional<Object> getPropertySafely(Key key) {
        Objects.requireNonNull(key, "Property key cannot be null");
        return Optional.ofNullable(getProperty(key));
    }

    /**
     * Gets a property value as a specific type with Optional wrapper.
     *
     * @param <T> the expected type of the property value
     * @param key the property key to retrieve, must not be null
     * @param expectedType the expected class type, must not be null
     * @return an Optional containing the typed property value, or empty if not found or wrong type
     * @throws IllegalArgumentException if key or expectedType is null
     */
    default <T> Optional<T> getPropertyAs(Key key, Class<T> expectedType) {
        Objects.requireNonNull(key, "Property key cannot be null");
        Objects.requireNonNull(expectedType, "Expected type cannot be null");

        return getPropertySafely(key)
                .filter(expectedType::isInstance)
                .map(expectedType::cast);
    }

    /**
     * Gets a string property value.
     *
     * @param key the property key to retrieve, must not be null
     * @return an Optional containing the string value, or empty if not found or not a string
     * @throws IllegalArgumentException if key is null
     */
    default Optional<String> getStringProperty(Key key) {
        return getPropertyAs(key, String.class);
    }

    /**
     * Gets an integer property value.
     *
     * @param key the property key to retrieve, must not be null
     * @return an Optional containing the integer value, or empty if not found or not an integer
     * @throws IllegalArgumentException if key is null
     */
    default Optional<Integer> getIntegerProperty(Key key) {
        return getPropertyAs(key, Integer.class);
    }

    /**
     * Gets a boolean property value.
     *
     * @param key the property key to retrieve, must not be null
     * @return an Optional containing the boolean value, or empty if not found or not a boolean
     * @throws IllegalArgumentException if key is null
     */
    default Optional<Boolean> getBooleanProperty(Key key) {
        return getPropertyAs(key, Boolean.class);
    }

    /**
     * Checks if a property is currently set (has a non-null value).
     *
     * @param key the property key to check, must not be null
     * @return true if the property has a non-null value, false otherwise
     * @throws IllegalArgumentException if key is null
     */
    default boolean hasProperty(Key key) {
        return getPropertySafely(key).isPresent();
    }

    /**
     * Gets the server's root directory as a string.
     *
     * @return an Optional containing the root directory path, or empty if not configured
     */
    default Optional<String> getRootDirectory() {
        return getStringProperty(Key.ROOT_DIRECTORY);
    }

    /**
     * Gets the HTTP listener port.
     *
     * @return an Optional containing the HTTP port number, or empty if not configured
     */
    default Optional<Integer> getListenerPort() {
        return getIntegerProperty(Key.LISTENER_PORT);
    }

    /**
     * Gets the HTTPS/SSL listener port.
     *
     * @return an Optional containing the HTTPS port number, or empty if not configured
     */
    default Optional<Integer> getSslListenerPort() {
        return getIntegerProperty(Key.LISTENER_SSL_PORT);
    }

    /**
     * Gets the server version information.
     *
     * @return an Optional containing the version string, or empty if not available
     */
    default Optional<String> getVersion() {
        return getStringProperty(Key.VERSION);
    }

    /**
     * Checks if the server is running in unit testing mode.
     *
     * @return true if unit testing mode is enabled, false otherwise
     */
    default boolean isUnitTestingMode() {
        return getBooleanProperty(Key.UNIT_TESTING).orElse(false);
    }
}

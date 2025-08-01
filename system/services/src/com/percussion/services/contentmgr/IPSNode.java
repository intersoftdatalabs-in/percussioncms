// REFACTORED: CP-JAVA11
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
package com.percussion.services.contentmgr;

import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.jsr170.IPSJcrCacheItem;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Modern Java 11 extended JCR node interface with enhanced functionality.
 *
 * <p>This interface extends the standard JCR {@link Node} interface with Percussion-specific
 * functionality including:
 * <ul>
 *   <li>GUID-based node identification with Optional-based safe access</li>
 *   <li>Enhanced property access with Stream support for multi-valued properties</li>
 *   <li>Improved parent-child relationship management</li>
 *   <li>Modern exception handling and null safety</li>
 * </ul>
 *
 * <p>All operations follow Java 11 best practices with null safety, Optional usage,
 * and comprehensive parameter validation.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSNode extends Node, IPSJcrCacheItem {

    /**
     * Gets the global unique identifier for this node.
     *
     * @return the GUID, never null
     * @throws IllegalStateException if the node doesn't have a valid GUID
     */
    IPSGuid getGuid();

    /**
     * Safely gets the global unique identifier for this node.
     *
     * @return an Optional containing the GUID, or empty if not available
     */
    default Optional<IPSGuid> getGuidSafely() {
        try {
            return Optional.of(getGuid());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Sets the depth level of this node in the tree hierarchy.
     *
     * @param depth the depth level, must be non-negative
     * @throws IllegalArgumentException if depth is negative
     */
    void setDepth(int depth);

    /**
     * Sets the depth level of this node with validation.
     *
     * @param depth the depth level, must be non-negative
     * @throws IllegalArgumentException if depth is negative
     */
    default void setDepthSafely(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Depth cannot be negative: " + depth);
        }
        setDepth(depth);
    }

    /**
     * Sets the parent node for this node.
     *
     * @param parent the parent node, must not be null
     * @throws RepositoryException if the parent relationship cannot be established
     * @throws IllegalArgumentException if parent is null
     */
    void setParent(Node parent) throws RepositoryException;

    /**
     * Safely sets the parent node with enhanced validation.
     *
     * @param parent the parent node, must not be null
     * @return true if the parent was set successfully, false otherwise
     * @throws IllegalArgumentException if parent is null
     */
    default boolean setParentSafely(Node parent) {
        Objects.requireNonNull(parent, "Parent node cannot be null");
        try {
            setParent(parent);
            return true;
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     * Gets the multi-valued string properties for the specified property name.
     *
     * <p>Returns a sorted list of unique string values. The list is immutable
     * and sorted in ascending order for consistent behavior.
     *
     * @param name the property name, must not be null or empty
     * @return an immutable list of unique string values, never null but may be empty
     * @throws PathNotFoundException if the property does not exist
     * @throws RepositoryException if failed to retrieve the property values
     * @throws IllegalArgumentException if name is null or empty
     */
    List<String> getPropertyStringValues(String name) throws PathNotFoundException, RepositoryException;

    /**
     * Safely gets multi-valued string properties with Optional return.
     *
     * @param name the property name, must not be null or empty
     * @return an Optional containing the list of string values, or empty if not found
     * @throws IllegalArgumentException if name is null or empty
     */
    default Optional<List<String>> getPropertyStringValuesSafely(String name) {
        Objects.requireNonNull(name, "Property name cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Property name cannot be empty");
        }
        try {
            return Optional.of(getPropertyStringValues(name));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Streams the multi-valued string properties for efficient processing.
     *
     * @param name the property name, must not be null or empty
     * @return a stream of string values, never null but may be empty
     * @throws IllegalArgumentException if name is null or empty
     */
    default Stream<String> streamPropertyStringValues(String name) {
        return getPropertyStringValuesSafely(name)
                .map(List::stream)
                .orElse(Stream.empty());
    }

    /**
     * Checks if a property exists and has string values.
     *
     * @param name the property name, must not be null or empty
     * @return true if the property exists and has values, false otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default boolean hasPropertyStringValues(String name) {
        return getPropertyStringValuesSafely(name)
                .map(list -> !list.isEmpty())
                .orElse(false);
    }

    /**
     * Gets the count of string values for a multi-valued property.
     *
     * @param name the property name, must not be null or empty
     * @return the count of string values, 0 if property doesn't exist or is empty
     * @throws IllegalArgumentException if name is null or empty
     */
    default int getPropertyStringValueCount(String name) {
        return getPropertyStringValuesSafely(name)
                .map(List::size)
                .orElse(0);
    }

    /**
     * Gets the first string value for a multi-valued property.
     *
     * @param name the property name, must not be null or empty
     * @return an Optional containing the first string value, or empty if not found
     * @throws IllegalArgumentException if name is null or empty
     */
    default Optional<String> getFirstPropertyStringValue(String name) {
        return streamPropertyStringValues(name).findFirst();
    }

    /**
     * Checks if this node has a valid GUID.
     *
     * @return true if the node has a valid GUID, false otherwise
     */
    default boolean hasValidGuid() {
        return getGuidSafely().isPresent();
    }

    /**
     * Gets a string representation of this node's GUID.
     *
     * @return an Optional containing the GUID string, or empty if no valid GUID
     */
    default Optional<String> getGuidAsString() {
        return getGuidSafely().map(Object::toString);
    }
}

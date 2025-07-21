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

package com.percussion.services.data;

import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Modern Java 11 interface for objects that can be identified by a GUID.
 *
 * <p>This interface represents the contract for objects that have a unique identifier
 * in the form of a GUID ({@link IPSGuid}). It provides both traditional and modern
 * Java 11 access patterns with Optional-based safe operations.
 *
 * <p>Implementations should ensure that:
 * <ul>
 *   <li>The item ID is never null once the object is properly initialized</li>
 *   <li>The item ID remains consistent throughout the object's lifecycle</li>
 *   <li>The item ID uniquely identifies the object within its domain</li>
 * </ul>
 *
 * <p>This interface is commonly used across the Percussion CMS system for objects
 * that need to be tracked, referenced, or persisted with stable identifiers.
 *
 * @author adamgent
 * @since Java 11 Modernization
 */
public interface IPSIdentifiableItem {

    /**
     * Gets the item's identifying GUID.
     *
     * <p>This method returns the unique identifier for this object. The GUID should
     * be stable throughout the object's lifecycle and unique within the appropriate
     * domain (e.g., content items, templates, workflows).
     *
     * @return the item's GUID, never null for properly initialized objects
     * @throws IllegalStateException if the object is not properly initialized
     */
    IPSGuid getItemId();

    /**
     * Safely gets the item's identifying GUID with Optional wrapper.
     *
     * <p>This method provides null-safe access to the item's GUID, returning
     * an empty Optional if the GUID is not available or if the object is not
     * properly initialized.
     *
     * @return an Optional containing the item's GUID, or empty if not available
     */
    default Optional<IPSGuid> getItemIdSafely() {
        try {
            return Optional.ofNullable(getItemId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if this object has a valid item ID.
     *
     * <p>This method determines whether the object has been properly initialized
     * with a valid GUID. It's useful for validation and debugging purposes.
     *
     * @return true if the object has a non-null item ID, false otherwise
     */
    default boolean hasValidItemId() {
        return getItemIdSafely().isPresent();
    }

    /**
     * Gets the string representation of the item's GUID.
     *
     * <p>This method provides a convenient way to get the string form of the
     * item's identifier, which is often needed for logging, debugging, or
     * external system integration.
     *
     * @return an Optional containing the GUID as a string, or empty if no valid GUID
     */
    default Optional<String> getItemIdAsString() {
        return getItemIdSafely().map(Object::toString);
    }

    /**
     * Checks if this object has the same item ID as another identifiable object.
     *
     * <p>This method provides a safe way to compare item IDs between two objects,
     * handling null cases gracefully. It's useful for equality checks and
     * duplicate detection.
     *
     * @param other the other identifiable object to compare with, may be null
     * @return true if both objects have the same non-null item ID, false otherwise
     */
    default boolean hasSameItemIdAs(IPSIdentifiableItem other) {
        if (other == null) {
            return false;
        }
        var thisId = getItemIdSafely();
        var otherId = other.getItemIdSafely();
        return thisId.isPresent() && otherId.isPresent() && Objects.equals(thisId.get(), otherId.get());
    }

    /**
     * Validates that this object's item ID matches the expected GUID.
     *
     * <p>This method is useful for verification and validation scenarios where
     * you need to ensure an object has a specific identifier.
     *
     * @param expectedId the expected GUID, must not be null
     * @return true if the object's item ID matches the expected ID, false otherwise
     * @throws IllegalArgumentException if expectedId is null
     */
    default boolean hasItemId(IPSGuid expectedId) {
        Objects.requireNonNull(expectedId, "Expected ID cannot be null");
        return getItemIdSafely()
                .map(actualId -> Objects.equals(actualId, expectedId))
                .orElse(false);
    }

    /**
     * Gets the hash code based on the item ID.
     *
     * <p>This method provides a consistent hash code implementation based on
     * the item's GUID, which is useful for collections and equality operations.
     *
     * @return the hash code of the item ID, or 0 if no valid ID
     */
    default int getItemIdHashCode() {
        return getItemIdSafely()
                .map(Objects::hashCode)
                .orElse(0);
    }
}

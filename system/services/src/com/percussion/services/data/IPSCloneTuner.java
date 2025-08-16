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
package com.percussion.services.data;

import java.util.Optional;

/**
 * Modern Java 11 interface for tuning cloned objects to make them persistable.
 *
 * <p>This interface defines the contract for making cloned objects valid for persistence.
 * Every object that supports cloning (or copy construction) should implement this interface
 * to ensure proper handling of identifiers and persistence-specific adjustments.
 *
 * <p>The tuning process typically involves:
 * <ul>
 *   <li>Adjusting the object's identifier to avoid conflicts</li>
 *   <li>Resetting transient or calculated fields</li>
 *   <li>Updating relationships and references as needed</li>
 *   <li>Ensuring all required fields are properly initialized</li>
 * </ul>
 *
 * <p><strong>Important:</strong> This interface assumes the object is already a clone
 * of a persisted object and modifies the current instance rather than creating a new one.
 *
 * @author Percussion Software
 * @since Java 11 Modernization
 */
public interface IPSCloneTuner {

    /**
     * Tunes this cloned object to make it ready for persistence.
     *
     * <p>This method modifies the current instance to ensure it can be safely persisted.
     * The primary adjustment is setting a new identifier, but implementations may perform
     * additional modifications as needed for their specific persistence requirements.
     *
     * <p><strong>Behavior:</strong>
     * <ul>
     *   <li>Sets the new identifier on this object</li>
     *   <li>Performs any additional persistence-related adjustments</li>
     *   <li>Returns the modified instance (fluent interface pattern)</li>
     *   <li>Does not create a new object instance</li>
     * </ul>
     *
     * @param newId the new identifier for this object, must be positive
     * @return this object after tuning, never null
     * @throws IllegalArgumentException if newId is not positive
     * @throws IllegalStateException if the object cannot be tuned for persistence
     */
    Object tuneClone(long newId);

    /**
     * Safely tunes this cloned object with enhanced validation and error handling.
     *
     * <p>This method provides a null-safe wrapper around {@link #tuneClone(long)}
     * that validates the input and handles any exceptions that might occur during tuning.
     *
     * @param newId the new identifier for this object, must be positive
     * @return an Optional containing the tuned object, or empty if tuning fails
     * @throws IllegalArgumentException if newId is not positive
     */
    default Optional<Object> tuneCloneSafely(long newId) {
        if (newId <= 0) {
            throw new IllegalArgumentException("New ID must be positive: " + newId);
        }
        try {
            return Optional.of(tuneClone(newId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if this object can be tuned for persistence.
     *
     * <p>Implementations can override this method to provide validation logic
     * that determines whether the object is in a state suitable for tuning.
     *
     * @return true if the object can be tuned, false otherwise
     */
    default boolean canTune() {
        return true;
    }

    /**
     * Validates that a new identifier is acceptable for this object.
     *
     * <p>This method allows implementations to enforce specific rules about
     * what constitutes a valid identifier for their object type.
     *
     * @param newId the identifier to validate
     * @return true if the identifier is valid, false otherwise
     */
    default boolean isValidNewId(long newId) {
        return newId > 0;
    }

    /**
     * Performs tuning with comprehensive validation and error handling.
     *
     * <p>This method provides the most robust tuning operation, combining
     * validation, error handling, and state checking into a single operation.
     *
     * @param newId the new identifier for this object, must be positive
     * @return an Optional containing the tuned object, or empty if any validation fails
     */
    default Optional<Object> tuneCloneWithValidation(long newId) {
        if (!isValidNewId(newId)) {
            return Optional.empty();
        }
        if (!canTune()) {
            return Optional.empty();
        }
        return tuneCloneSafely(newId);
    }
}

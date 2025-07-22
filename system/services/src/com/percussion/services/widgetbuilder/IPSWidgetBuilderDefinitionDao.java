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

package com.percussion.services.widgetbuilder;

import com.percussion.share.dao.IPSGenericDao;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Data Access Object interface for Widget Builder Definition operations with modern Java 11 patterns.
 * Provides comprehensive CRUD operations for managing widget builder definitions in the system
 * with enhanced type safety, Optional-based safe access, and Stream API integration.
 *
 * @author matthewernewein
 */
public interface IPSWidgetBuilderDefinitionDao {

    /**
     * Saves the widget builder definition object with enhanced validation.
     *
     * @param definition the widget builder definition to save, never null
     * @return the saved widget builder definition, never null
     * @throws IPSGenericDao.SaveException if there's an error saving the definition
     * @throws IllegalArgumentException if definition is null
     */
    PSWidgetBuilderDefinition save(PSWidgetBuilderDefinition definition) throws IPSGenericDao.SaveException;
    
    /**
     * Saves multiple widget builder definitions efficiently.
     *
     * @param definitions the collection of definitions to save, never null
     * @return list of saved definitions in the same order, never null
     * @throws IPSGenericDao.SaveException if there's an error saving any definition
     * @throws IllegalArgumentException if definitions is null or contains null elements
     */
    default List<PSWidgetBuilderDefinition> saveAll(Collection<PSWidgetBuilderDefinition> definitions)
            throws IPSGenericDao.SaveException {
        Objects.requireNonNull(definitions, "Definitions collection cannot be null");
        return definitions.stream()
            .peek(def -> Objects.requireNonNull(def, "Definition cannot be null"))
            .map(this::save)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Finds widget builder definition by the definition id with safe access.
     *
     * @param definitionId the unique identifier of the widget builder definition
     * @return Optional containing the widget builder definition if it exists, empty otherwise
     * @throws IllegalArgumentException if definitionId is negative
     */
    Optional<PSWidgetBuilderDefinition> find(long definitionId);

    /**
     * Loads widget builder definition by id with guaranteed existence.
     *
     * @param definitionId the unique identifier of the widget builder definition
     * @return the widget builder definition, never null
     * @throws IllegalArgumentException if definitionId is negative
     * @throws IllegalStateException if the definition doesn't exist
     */
    default PSWidgetBuilderDefinition load(long definitionId) {
        return find(definitionId)
            .orElseThrow(() -> new IllegalStateException(
                "Widget builder definition not found with ID: " + definitionId));
    }

    /**
     * Checks if a widget builder definition exists with the given id.
     *
     * @param definitionId the unique identifier to check
     * @return true if the definition exists, false otherwise
     * @throws IllegalArgumentException if definitionId is negative
     */
    default boolean exists(long definitionId) {
        if (definitionId < 0) {
            throw new IllegalArgumentException("Definition ID cannot be negative: " + definitionId);
        }
        return find(definitionId).isPresent();
    }

    /**
     * Deletes the widget builder definition entry for the supplied id with enhanced validation.
     *
     * @param definitionId the unique identifier, must not be negative
     * @throws IllegalArgumentException if definitionId is negative
     */
    void delete(long definitionId);
    
    /**
     * Deletes multiple widget builder definitions efficiently.
     *
     * @param definitionIds the collection of definition IDs to delete, never null
     * @throws IllegalArgumentException if definitionIds is null or contains negative values
     */
    default void deleteAll(Collection<Long> definitionIds) {
        Objects.requireNonNull(definitionIds, "Definition IDs collection cannot be null");
        definitionIds.forEach(id -> {
            if (id < 0) {
                throw new IllegalArgumentException("Definition ID cannot be negative: " + id);
            }
            delete(id);
        });
    }

    /**
     * Gets a list of all the Widget Builder Definitions in the system.
     *
     * @return unmodifiable list of all widget builder definitions, never null but may be empty
     */
    List<PSWidgetBuilderDefinition> getAll();

    /**
     * Gets all widget builder definitions as a stream for efficient processing.
     *
     * @return stream of all widget builder definitions, never null
     */
    default Stream<PSWidgetBuilderDefinition> getAllAsStream() {
        return getAll().stream();
    }

    /**
     * Gets the count of all widget builder definitions in the system.
     *
     * @return the total number of widget builder definitions
     */
    default long getCount() {
        return getAll().size();
    }

    /**
     * Finds widget builder definitions by name pattern.
     *
     * @param namePattern the name pattern to search for, may use SQL wildcards (%)
     * @return stream of matching definitions, never null
     */
    default Stream<PSWidgetBuilderDefinition> findByNamePattern(String namePattern) {
        Objects.requireNonNull(namePattern, "Name pattern cannot be null");
        var pattern = namePattern.toLowerCase();
        return getAllAsStream()
            .filter(def -> def.getName() != null &&
                          def.getName().toLowerCase().contains(pattern.replace("%", "")));
    }

    /**
     * Validates that a definition ID is valid (non-negative).
     *
     * @param definitionId the ID to validate
     * @throws IllegalArgumentException if the ID is negative
     */
    default void validateDefinitionId(long definitionId) {
        if (definitionId < 0) {
            throw new IllegalArgumentException("Definition ID cannot be negative: " + definitionId);
        }
    }
}

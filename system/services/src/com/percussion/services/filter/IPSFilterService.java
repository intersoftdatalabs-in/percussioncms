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
// REFACTORED: CP-JAVA11
package com.percussion.services.filter;

import com.percussion.services.catalog.IPSCataloger;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.percussion.services.filter.data.PSItemFilterRuleDef;
import com.intsof.percussioncms.auditlog.codes.FilterServiceErrorCodes;
/**
 * The filter service manages item filters and applies them to lists of content IDs.
 * This service provides a higher-level abstraction that uses other services to
 * implement content filtering functionality with type-safe operations and efficient
 * processing capabilities.
 *
 * @author dougrand
 */
public interface IPSFilterService extends IPSCataloger {

    /**
     * Create a new item filter with the specified name and description.
     *
     * @param name a unique name, not {@code null} or empty
     * @param description a description, may be {@code null} or empty
     * @return a newly created filter object, never {@code null}
     * @throws IllegalArgumentException if name is null or empty
     */
    IPSItemFilter createFilter(String name, String description);

    /**
     * Load one or more filters from the GUIDs.
     *
     * @param ids a list of GUIDs, not empty, never {@code null}
     * @return a list of filter objects, never {@code null} or empty
     * @throws PSNotFoundException if one or more IDs are not found or invalid
     * @throws IllegalArgumentException if ids is null or empty
     */
    List<IPSItemFilter> loadFilter(List<IPSGuid> ids) throws PSNotFoundException;

    /**
     * Load the item filter for the supplied ID.
     *
     * @param id the ID of the item filter to load, not {@code null}
     * @return the loaded item filter, never {@code null}
     * @throws PSNotFoundException if no filter was found for the supplied ID
     * @throws IllegalArgumentException if id is null
     */
    IPSItemFilter loadFilter(IPSGuid id) throws PSNotFoundException;

    /**
     * Load the item filter for the supplied ID, returning an Optional for safe access.
     *
     * @param id the ID of the item filter to load, not {@code null}
     * @return an Optional containing the filter if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    default Optional<IPSItemFilter> findFilter(IPSGuid id) {
        try {
            return Optional.of(loadFilter(id));
        } catch (PSNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Loads one filter by name. The returned filter should be considered
     * read-only as this method uses an in-memory cache that is shared between threads.
     *
     * @param name name of filter, not {@code null} or empty
     * @return the item filter, never {@code null}
     * @throws PSFilterException if no filter is found with the given name
     * @throws IllegalArgumentException if name is null or empty
     */
    IPSItemFilter findFilterByName(String name) throws PSFilterException;

    /**
     * Loads one filter by name, returning an Optional for safe access.
     *
     * @param name name of filter, not {@code null} or empty
     * @return an Optional containing the filter if found, empty otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default Optional<IPSItemFilter> findFilterByNameSafe(String name) {
        try {
            return Optional.of(findFilterByName(name));
        } catch (PSFilterException e) {
            // Only "not found" is empty. Other filter errors must not look like a miss
            // (avoids callers inserting duplicates / masking DB failures).
            if (e.getErrorCode() == FilterServiceErrorCodes.FILTER_MISSING.numericCode()) {
                return Optional.empty();
            }
            throw new IllegalStateException(
                    "Filter lookup failed for name: " + name, e);
        }
    }

    /**
     * Loads one filter by ID. The returned filter should be considered
     * read-only as this method uses an in-memory cache that is shared between threads.
     *
     * @param id ID of filter, not {@code null}
     * @return the item filter, may be {@code null} if filter not found
     * @throws PSNotFoundException if filter lookup fails
     * @throws IllegalArgumentException if id is null
     */
    IPSItemFilter findFilterByID(IPSGuid id) throws PSNotFoundException;

    /**
     * Find all filters for the supplied name pattern.
     *
     * @param name the name of the filter to find, may be {@code null} or empty.
     *             Finds all filters if {@code null} or empty, SQL type (%) wildcards are supported
     * @return a list with all found item filters for the supplied name, never {@code null},
     *         may be empty, ascending alphabetically ordered by name
     */
    List<IPSItemFilter> findFiltersByName(String name);

    /**
     * Convenience method to retrieve all filters.
     *
     * @return list of all filters, never <code>null</code>
     */
    default List<IPSItemFilter> findAllFilters() {
        return findFiltersByName(null);
    }

    /**
     * Get a stream of filters for efficient processing.
     *
     * @param name the name pattern to match filters
     * @return Stream of item filters, never {@code null}
     */
    default Stream<IPSItemFilter> streamFilters(String name) {
        return findFiltersByName(name).stream();
    }

    /**
     * Loads one filter by the legacy authtype.
     *
     * @param authtype the authtype identifier
     * @return the item filter, never {@code null}
     * @throws PSFilterException if no filter is found with the given authtype
     * @throws IllegalArgumentException if authtype is null
     */
    IPSItemFilter findFilterByAuthType(String authtype) throws PSFilterException;

    /**
     * Load a filter that should not be modified by the caller (read-only view).
     * Default implementation delegates to {@link #loadFilter(IPSGuid)} for
     * backward compatibility.
     *
     * @param id the id of the filter to load, not {@code null}
     * @return the loaded filter, never {@code null}
     * @throws PSNotFoundException if the filter cannot be found
     */
    default IPSItemFilter loadUnmodifiableFilter(IPSGuid id) throws PSNotFoundException {
        return loadFilter(id);
    }

    /**
     * Loads one filter by the legacy authtype, returning an Optional for safe access.
     *
     * @param authtype the authtype identifier
     * @return an Optional containing the filter if found, empty otherwise
     * @throws IllegalArgumentException if authtype is null
     */
    default Optional<IPSItemFilter> findFilterByAuthTypeSafe(String authtype) {
        try {
            return Optional.of(findFilterByAuthType(authtype));
        } catch (PSFilterException e) {
            return Optional.empty();
        }
    }

    /**
     * Save the supplied item filters to the repository.
     *
     * @param filters a list of item filters to save, not {@code null} or empty
     * @throws IllegalArgumentException if filters is null or empty
     */
    void saveFilter(List<IPSItemFilter> filters);

    /**
     * Save a single item filter to the repository.
     *
     * @param filter the item filter to save, not {@code null}
     * @throws IllegalArgumentException if filter is null
     */
    default void saveFilter(IPSItemFilter filter) {
        saveFilter(List.of(filter));
    }

    /**
     * Delete the supplied item filters from the repository.
     *
     * @param filters a list of item filters to delete, not {@code null} or empty
     * @throws IllegalArgumentException if filters is null or empty
     */
    void deleteFilter(List<IPSItemFilter> filters);

    /**
     * Delete a single item filter from the repository.
     *
     * @param filter the item filter to delete, not {@code null}
     * @throws IllegalArgumentException if filter is null
     */
    default void deleteFilter(IPSItemFilter filter) {
        deleteFilter(List.of(filter));
    }

    /**
     * Apply the specified filter to a list of content IDs.
     *
     * @param filter the filter to apply, not {@code null}
     * @param contentIds the list of content IDs to filter, not {@code null}
     * @param params additional parameters for filtering, may be {@code null}
     * @return filtered list of content IDs, never {@code null}, may be empty
     * @throws IllegalArgumentException if filter or contentIds is null
     */
    List<IPSGuid> applyFilter(IPSItemFilter filter, List<IPSGuid> contentIds, Map<String, Object> params);

    /**
     * Apply the specified filter asynchronously to a list of content IDs.
     *
     * @param filter the filter to apply, not {@code null}
     * @param contentIds the list of content IDs to filter, not {@code null}
     * @param params additional parameters for filtering, may be {@code null}
     * @return CompletableFuture containing filtered list of content IDs
     * @throws IllegalArgumentException if filter or contentIds is null
     */
    default CompletableFuture<List<IPSGuid>> applyFilterAsync(IPSItemFilter filter,
                                                             List<IPSGuid> contentIds,
                                                             Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> applyFilter(filter, contentIds, params));
    }

    /**
     * Check if a filter exists by name.
     *
     * @param name the filter name to check, not {@code null} or empty
     * @return {@code true} if the filter exists, {@code false} otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    default boolean filterExists(String name) {
        return findFilterByNameSafe(name).isPresent();
    }

    /**
     * Check if a filter exists by ID.
     *
     * @param id the filter ID to check, not {@code null}
     * @return {@code true} if the filter exists, {@code false} otherwise
     * @throws IllegalArgumentException if id is null
     */
    default boolean filterExists(IPSGuid id) {
        return findFilter(id).isPresent();
    }

    /**
     * Backwards-compatible helper to create a rule definition for a filter.
     *
     * @param rulename the name of the rule, not {@code null} or empty
     * @param params the parameters map (String->String), may be {@code null}
     * @return a new rule def instance
     * @throws PSFilterException if the rule cannot be created
     */
    default IPSItemFilterRuleDef createRuleDef(String rulename, Map<String, String> params) throws PSFilterException {
        Objects.requireNonNull(rulename, "rulename may not be null or empty");
        PSItemFilterRuleDef def = new PSItemFilterRuleDef();
        def.setRule(rulename);
        if (params != null) {
            params.forEach((k, v) -> {
                if (k != null && v != null) {
                    def.setParam(k, v);
                }
            });
        }
        return def;
    }

    /**
     * Get all available filters in the system.
     *
     * @return a list of all filters, never {@code null}, may be empty
     */
    default List<IPSItemFilter> getAllFilters() {
        return findFiltersByName("");
    }

    /**
     * Get a stream of all available filters for efficient processing.
     *
     * @return Stream of all filters, never {@code null}
     */
    default Stream<IPSItemFilter> streamAllFilters() {
        return getAllFilters().stream();
    }

    /**
     * Find filters that match the given predicate.
     *
     * @param predicate the condition to test filters against, not {@code null}
     * @return a list of matching filters, never {@code null}, may be empty
     * @throws IllegalArgumentException if predicate is null
     */
    default List<IPSItemFilter> findFiltersWhere(Predicate<IPSItemFilter> predicate) {
        return streamAllFilters()
            .filter(predicate)
            .toList();
    }

    /**
     * Get the total number of filters in the system.
     *
     * @return the total filter count
     */
    default long getFilterCount() {
        return streamAllFilters().count();
    }

    /**
     * Get filter statistics and information.
     *
     * @return a string representation of filter statistics
     */
    default String getFilterStatistics() {
        var filterCount = getFilterCount();
        return String.format("Filter Statistics: %d total filters", filterCount);
    }

    /**
     * Validate that a filter is properly configured and can be used.
     *
     * @param filter the filter to validate, not {@code null}
     * @return {@code true} if the filter is valid, {@code false} otherwise
     * @throws IllegalArgumentException if filter is null
     */
    default boolean validateFilter(IPSItemFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter cannot be null");
        }
        return filter.getName() != null && !filter.getName().trim().isEmpty();
    }
}

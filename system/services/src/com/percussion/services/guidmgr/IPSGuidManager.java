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
package com.percussion.services.guidmgr;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Creates globally unique IDs for use when creating service objects. The methods
 * that begin with 'create' are generally only useful to the system.
 * Implementers will be interested in the methods that begin with 'make'.
 * <p>
 * There are also methods for converting between old-style item locators and
 * GUIDs. These are provided for interoperability between the old and new models.
 * <p>
 * This service is thread-safe and supports efficient batch operations for
 * high-performance GUID generation.
 *
 * @author dougrand
 * @author stephenbolton
 */
public interface IPSGuidManager {

    /**
     * Create a single new GUID for the given type.
     *
     * @param type the type, not {@code null}
     * @return a new GUID, never {@code null}
     * @throws IllegalArgumentException if type is null
     */
    IPSGuid createGuid(PSTypeEnum type);

    /**
     * Create a series of GUIDs for the given type.
     *
     * @param type the type, not {@code null}
     * @param count the number of GUIDs to create, must be a positive number
     * @return a list of GUIDs, the size of the list will equal count, never {@code null}
     * @throws IllegalArgumentException if type is null or count is not positive
     */
    List<IPSGuid> createGuids(PSTypeEnum type, int count);

    /**
     * Create a stream of GUIDs for efficient processing.
     *
     * @param type the type, not {@code null}
     * @param count the number of GUIDs to create, must be a positive number
     * @return a stream of GUIDs, never {@code null}
     * @throws IllegalArgumentException if type is null or count is not positive
     */
    default Stream<IPSGuid> streamGuids(PSTypeEnum type, int count) {
        return createGuids(type, count).stream();
    }

    /**
     * Create a single new GUID for the given type and repository.
     *
     * @param repositoryId the repository ID, must be greater than zero. Zero is the same
     *                     as not using a repository. Negative is invalid
     * @param type the type, not {@code null}
     * @return a new GUID, never {@code null}
     * @throws IllegalArgumentException if repositoryId is negative or type is null
     */
    IPSGuid createGuid(byte repositoryId, PSTypeEnum type);

    /**
     * Create a series of GUIDs for the given type and repository.
     *
     * @param repositoryId the repository ID, must be greater than zero. Zero is the same
     *                     as not using a repository. Negative is invalid
     * @param type the type, not {@code null}
     * @param count the number of GUIDs to create, must be a positive number
     * @return a list of GUIDs, the size of the list will equal count, never {@code null}
     * @throws IllegalArgumentException if repositoryId is negative, type is null, or count is not positive
     */
    List<IPSGuid> createGuids(byte repositoryId, PSTypeEnum type, int count);

    /**
     * Create a single ID using values stored in the next number table.
     *
     * @param key the key, not {@code null} or empty
     * @return the next allocated ID
     * @throws IllegalArgumentException if key is null or empty
     */
    int createId(String key);

    /**
     * Create multiple IDs using values stored in the next number table.
     *
     * @param key the key, not {@code null} or empty
     * @param count the number of IDs to create, must be positive
     * @return a list of allocated IDs, never {@code null}
     * @throws IllegalArgumentException if key is null/empty or count is not positive
     */
    default List<Integer> createIds(String key, int count) {
        return IntStream.range(0, count)
            .map(i -> createId(key))
            .boxed()
            .toList();
    }

    /**
     * Allocate the next ID for a given type and return only the 64-bit ID.
     * Most GUIDs are only using the lower 32 bits, which is not always adequate
     * for longer-lived data for non-design objects.
     *
     * @param type the type, provides the index into the saved next numbers, not {@code null}
     * @return the next allocated 64-bit ID
     * @throws IllegalArgumentException if type is null
     */
    long createLongId(PSTypeEnum type);

    /**
     * Create a GUID from a locator for backward compatibility.
     *
     * @param locator the PSLocator to convert, not {@code null}
     * @return a GUID representing the locator, never {@code null}
     * @throws IllegalArgumentException if locator is null
     */
    IPSGuid makeGuid(PSLocator locator);

    /**
     * Create a GUID from a locator, returning an Optional for safe access.
     *
     * @param locator the PSLocator to convert, may be {@code null}
     * @return an Optional containing the GUID if conversion is successful, empty otherwise
     */
    default Optional<IPSGuid> makeGuidSafe(PSLocator locator) {
        try {
            return locator != null ? Optional.of(makeGuid(locator)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Create a locator from a GUID for backward compatibility.
     *
     * @param guid the GUID to convert, not {@code null}
     * @return a PSLocator representing the GUID, never {@code null}
     * @throws IllegalArgumentException if guid is null
     */
    PSLocator makeLocator(IPSGuid guid);

    /**
     * Create a locator from a GUID, returning an Optional for safe access.
     *
     * @param guid the GUID to convert, may be {@code null}
     * @return an Optional containing the locator if conversion is successful, empty otherwise
     */
    default Optional<PSLocator> makeLocatorSafe(IPSGuid guid) {
        try {
            return guid != null ? Optional.of(makeLocator(guid)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Create GUIDs asynchronously for high-performance scenarios.
     *
     * @param type the type, not {@code null}
     * @param count the number of GUIDs to create, must be positive
     * @return a CompletableFuture containing the list of GUIDs
     * @throws IllegalArgumentException if type is null or count is not positive
     */
    default CompletableFuture<List<IPSGuid>> createGuidsAsync(PSTypeEnum type, int count) {
        return CompletableFuture.supplyAsync(() -> createGuids(type, count));
    }

    /**
     * Check if a GUID is valid and properly formatted.
     *
     * @param guid the GUID to validate, not {@code null}
     * @return {@code true} if the GUID is valid, {@code false} otherwise
     * @throws IllegalArgumentException if guid is null
     */
    default boolean isValidGuid(IPSGuid guid) {
        if (guid == null) {
            throw new IllegalArgumentException("guid cannot be null");
        }
        return guid.getUUID() > 0 && guid.getType() != null;
    }

    /**
     * Get all supported GUID types.
     *
     * @return a set of supported PSTypeEnum values, never {@code null}
     */
    Set<PSTypeEnum> getSupportedTypes();

    /**
     * Get statistics about GUID generation.
     *
     * @return a string representation of GUID manager statistics
     */
    default String getGuidStatistics() {
        var supportedTypes = getSupportedTypes();
        return String.format("GUID Manager Statistics: %d supported types", supportedTypes.size());
    }

    /**
     * Batch create GUIDs for multiple types efficiently.
     *
     * @param requests a list of type-count pairs for batch creation
     * @return a list of GUID lists corresponding to each request
     * @throws IllegalArgumentException if requests is null or contains invalid entries
     */
    default List<List<IPSGuid>> batchCreateGuids(List<GuidRequest> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("requests cannot be null");
        }
        return requests.stream()
            .map(request -> createGuids(request.getType(), request.getCount()))
            .toList();
    }

    /**
     * A request for GUID creation containing type and count information.
     */
    class GuidRequest {
        private final PSTypeEnum type;
        private final int count;

        public GuidRequest(PSTypeEnum type, int count) {
            this.type = type;
            this.count = count;
        }

        public PSTypeEnum getType() {
            return type;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Create a GUID request for batch operations.
     *
     * @param type the GUID type, not {@code null}
     * @param count the number of GUIDs to create, must be positive
     * @return a new GuidRequest, never {@code null}
     * @throws IllegalArgumentException if type is null or count is not positive
     */
    static GuidRequest request(PSTypeEnum type, int count) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        return new GuidRequest(type, count);
    }
}

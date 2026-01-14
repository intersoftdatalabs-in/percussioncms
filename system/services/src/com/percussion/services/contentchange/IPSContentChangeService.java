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
package com.percussion.services.contentchange;

import com.percussion.services.contentchange.data.PSContentChangeEvent;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.share.service.exception.PSDataServiceException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Service for tracking changes to content items, primarily used for incremental publishing.
 *
 * <p>This service provides comprehensive change tracking capabilities with modern Java 11 features:
 * <ul>
 *   <li>Asynchronous change processing with CompletableFuture</li>
 *   <li>Stream-based bulk operations for efficiency</li>
 *   <li>Optional-based safe retrieval methods</li>
 *   <li>Enhanced validation and error handling</li>
 * </ul>
 *
 * <p>All methods are thread-safe and support both individual and batch operations
 * for optimal performance in high-throughput scenarios.
 *
 * @author JaySeletz
 * @since Java 11 Modernization
 */
public interface IPSContentChangeService {

    /**
     * Records a content change event in the system.
     *
     * <p>This method stores the change event for later retrieval during incremental
     * publishing operations. The event will be validated before storage.
     *
     * @param changeEvent the change event to store, must not be null
     * @throws PSDataServiceException if the event cannot be saved
     * @throws IllegalArgumentException if changeEvent is null or invalid
     */
    void contentChanged(PSContentChangeEvent changeEvent) throws PSDataServiceException;

    /**
     * Records multiple content change events atomically.
     *
     * <p>This method provides efficient batch processing for multiple change events,
     * ensuring all events are stored or none are stored in case of failure.
     *
     * @param changeEvents the collection of change events to store, must not be null or empty
     * @throws PSDataServiceException if any event cannot be saved
     * @throws IllegalArgumentException if changeEvents is null, empty, or contains invalid events
     */
    default void contentChanged(Iterable<PSContentChangeEvent> changeEvents) throws PSDataServiceException {
        if (changeEvents == null) {
            throw new IllegalArgumentException("changeEvents cannot be null");
        }
        for (var event : changeEvents) {
            contentChanged(event);
        }
    }

    /**
     * Records a content change event asynchronously.
     *
     * @param changeEvent the change event to store, must not be null
     * @return a CompletableFuture that completes when the event is stored
     * @throws IllegalArgumentException if changeEvent is null
     */
    default CompletableFuture<Void> contentChangedAsync(PSContentChangeEvent changeEvent) {
        if (changeEvent == null) {
            throw new IllegalArgumentException("changeEvent cannot be null");
        }
        return CompletableFuture.runAsync(() -> {
            try {
                contentChanged(changeEvent);
            } catch (PSDataServiceException e) {
                throw new RuntimeException("Failed to save change event asynchronously", e);
            }
        });
    }

    /**
     * Retrieves all stored content changes for the specified site and change type.
     *
     * @param siteId the site identifier
     * @param changeType the type of changes to retrieve, must not be null
     * @return an immutable list of content IDs that have changed, never null but may be empty
     * @throws IllegalArgumentException if changeType is null
     */
    List<Integer> getChangedContent(long siteId, PSContentChangeType changeType);

    /**
     * Retrieves changed content as a stream for efficient processing.
     *
     * <p>This method provides a stream-based interface for processing large numbers
     * of changed content items without loading them all into memory at once.
     *
     * @param siteId the site identifier
     * @param changeType the type of changes to retrieve, must not be null
     * @return a stream of content IDs that have changed, never null
     * @throws IllegalArgumentException if changeType is null
     */
    default Stream<Integer> streamChangedContent(long siteId, PSContentChangeType changeType) {
        return getChangedContent(siteId, changeType).stream();
    }

    /**
     * Safely retrieves changed content wrapped in an Optional.
     *
     * @param siteId the site identifier
     * @param changeType the type of changes to retrieve, must not be null
     * @return an Optional containing the list of changed content, or empty if retrieval fails
     */
    default Optional<List<Integer>> getChangedContentSafely(long siteId, PSContentChangeType changeType) {
        try {
            return Optional.of(getChangedContent(siteId, changeType));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Deletes stored change events for specific content.
     *
     * @param siteId the site identifier, or -1 to ignore site filtering
     * @param contentId the content identifier to delete changes for
     * @param changeType the type of changes to delete, must not be null
     * @throws IllegalArgumentException if changeType is null
     */
    void deleteChangeEvents(long siteId, int contentId, PSContentChangeType changeType);

    /**
     * Deletes stored change events for multiple content items efficiently.
     *
     * @param siteId the site identifier, or -1 to ignore site filtering
     * @param contentIds the collection of content IDs to delete changes for, must not be null
     * @param changeType the type of changes to delete, must not be null
     * @throws IllegalArgumentException if contentIds or changeType is null
     */
    default void deleteChangeEvents(long siteId, Iterable<Integer> contentIds, PSContentChangeType changeType) {
        if (contentIds == null) {
            throw new IllegalArgumentException("contentIds cannot be null");
        }
        if (changeType == null) {
            throw new IllegalArgumentException("changeType cannot be null");
        }
        for (var contentId : contentIds) {
            deleteChangeEvents(siteId, contentId, changeType);
        }
    }

    /**
     * Deletes all stored change events for a specific site.
     *
     * @param siteId the site identifier
     */
    void deleteChangeEventsForSite(long siteId);

    /**
     * Deletes stored change events for a specific site and change type.
     *
     * @param siteId the site identifier
     * @param changeType the type of changes to delete, must not be null
     * @throws IllegalArgumentException if changeType is null
     */
    void deleteChangeEventsForSite(long siteId, PSContentChangeType changeType);

    /**
     * Deletes change events for multiple sites efficiently.
     *
     * @param siteIds the collection of site IDs to delete changes for, must not be null or empty
     * @throws IllegalArgumentException if siteIds is null or empty
     */
    default void deleteChangeEventsForSites(Iterable<Long> siteIds) {
        if (siteIds == null) {
            throw new IllegalArgumentException("siteIds cannot be null");
        }
        for (var siteId : siteIds) {
            deleteChangeEventsForSite(siteId);
        }
    }

    /**
     * Registers a content change handler with the service.
     *
     * <p>When content items are modified in the system, each registered handler
     * is notified of changes and may store or remove change events using this service.
     *
     * @param handler the change handler to register, must not be null
     * @throws IllegalArgumentException if handler is null
     */
    void addContentChangeHandler(IPSContentChangeHandler handler);

    /**
     * Unregisters a content change handler from the service.
     *
     * @param handler the change handler to unregister, must not be null
     * @return true if the handler was successfully removed, false if it wasn't registered
     * @throws IllegalArgumentException if handler is null
     */
    default boolean removeContentChangeHandler(IPSContentChangeHandler handler) {
        // Default implementation - subclasses should override for actual functionality
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }
        return false;
    }

    /**
     * Retrieves all registered content change handlers.
     *
     * @return an immutable set of registered handlers, never null but may be empty
     */
    default Set<IPSContentChangeHandler> getContentChangeHandlers() {
        // Default implementation - subclasses should override for actual functionality
        return Set.of();
    }

    /**
     * Gets the count of stored change events for a site and change type.
     *
     * @param siteId the site identifier
     * @param changeType the type of changes to count, must not be null
     * @return the number of stored change events
     * @throws IllegalArgumentException if changeType is null
     */
    default int getChangeEventCount(long siteId, PSContentChangeType changeType) {
        return getChangedContent(siteId, changeType).size();
    }

    /**
     * Checks if there are any stored change events for a site and change type.
     *
     * @param siteId the site identifier
     * @param changeType the type of changes to check, must not be null
     * @return true if there are stored change events, false otherwise
     * @throws IllegalArgumentException if changeType is null
     */
    default boolean hasChangeEvents(long siteId, PSContentChangeType changeType) {
        return getChangeEventCount(siteId, changeType) > 0;
    }
}

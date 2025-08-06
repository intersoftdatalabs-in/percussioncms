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

import com.percussion.cms.PSEditorChangeEvent;
import com.percussion.cms.PSRelationshipChangeEvent;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Functional interface for handling changes to content items and relationships in the system.
 *
 * <p>This interface provides modern Java 11 capabilities for content change handling:
 * <ul>
 *   <li>Asynchronous event processing with CompletableFuture</li>
 *   <li>Safe event handling with Optional return types</li>
 *   <li>Enhanced validation and error handling</li>
 *   <li>Functional programming support</li>
 * </ul>
 *
 * <p>Implementations should be thread-safe as multiple events may be processed
 * concurrently. All methods provide both synchronous and asynchronous variants
 * for optimal performance in different scenarios.
 *
 * @author JaySeletz
 * @since Java 11 Modernization
 */
@FunctionalInterface
public interface IPSContentChangeHandler {

    /**
     * Handles a content item change event.
     *
     * <p>This method is called when content items are modified in the system.
     * Implementations should process the event efficiently and handle any errors gracefully.
     *
     * @param event the editor change event, must not be null
     * @throws PSDataServiceException if there's an error processing the event data
     * @throws PSNotFoundException if referenced content cannot be found
     * @throws IllegalArgumentException if event is null
     */
    void handleEvent(PSEditorChangeEvent event) throws PSDataServiceException, PSNotFoundException;

    /**
     * Handles a relationship change event.
     *
     * <p>This method is called when relationships between content items are modified.
     * The default implementation delegates to the main handleEvent method for backward compatibility.
     *
     * @param event the relationship change event, must not be null
     * @throws PSDataServiceException if there's an error processing the event data
     * @throws PSNotFoundException if referenced content cannot be found
     * @throws IllegalArgumentException if event is null
     */
    default void handleEvent(PSRelationshipChangeEvent event) throws PSDataServiceException, PSNotFoundException {
        Objects.requireNonNull(event, "Relationship change event cannot be null");
        // Default implementation - subclasses should override for specific relationship handling
        // For backward compatibility, we don't throw an exception here
    }

    /**
     * Safely handles a content item change event, returning an Optional result.
     *
     * <p>This method provides exception-safe event handling, returning an empty Optional
     * if the event cannot be processed successfully.
     *
     * @param event the editor change event, must not be null
     * @return an Optional containing true if the event was handled successfully,
     *         or empty if an error occurred
     */
    default Optional<Boolean> handleEventSafely(PSEditorChangeEvent event) {
        try {
            Objects.requireNonNull(event, "Editor change event cannot be null");
            handleEvent(event);
            return Optional.of(true);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Safely handles a relationship change event, returning an Optional result.
     *
     * @param event the relationship change event, must not be null
     * @return an Optional containing true if the event was handled successfully,
     *         or empty if an error occurred
     */
    default Optional<Boolean> handleEventSafely(PSRelationshipChangeEvent event) {
        try {
            Objects.requireNonNull(event, "Relationship change event cannot be null");
            handleEvent(event);
            return Optional.of(true);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Handles a content item change event asynchronously.
     *
     * <p>This method provides non-blocking event processing for high-throughput scenarios.
     * The returned CompletableFuture completes when the event has been processed.
     *
     * @param event the editor change event, must not be null
     * @return a CompletableFuture that completes when the event is handled
     * @throws IllegalArgumentException if event is null
     */
    default CompletableFuture<Void> handleEventAsync(PSEditorChangeEvent event) {
        Objects.requireNonNull(event, "Editor change event cannot be null");
        return CompletableFuture.runAsync(() -> {
            try {
                handleEvent(event);
            } catch (PSDataServiceException | PSNotFoundException e) {
                throw new RuntimeException("Failed to handle editor change event asynchronously", e);
            }
        });
    }

    /**
     * Handles a relationship change event asynchronously.
     *
     * @param event the relationship change event, must not be null
     * @return a CompletableFuture that completes when the event is handled
     * @throws IllegalArgumentException if event is null
     */
    default CompletableFuture<Void> handleEventAsync(PSRelationshipChangeEvent event) {
        Objects.requireNonNull(event, "Relationship change event cannot be null");
        return CompletableFuture.runAsync(() -> {
            try {
                handleEvent(event);
            } catch (PSDataServiceException | PSNotFoundException e) {
                throw new RuntimeException("Failed to handle relationship change event asynchronously", e);
            }
        });
    }

    /**
     * Creates a composite handler that processes events with multiple handlers in sequence.
     *
     * <p>This factory method allows combining multiple handlers into a single handler
     * that executes them in the order they were provided.
     *
     * @param handlers the handlers to combine, must not be null or empty
     * @return a composite handler that executes all provided handlers
     * @throws IllegalArgumentException if handlers is null or empty
     */
    static IPSContentChangeHandler compose(IPSContentChangeHandler... handlers) {
        Objects.requireNonNull(handlers, "Handlers array cannot be null");
        if (handlers.length == 0) {
            throw new IllegalArgumentException("At least one handler must be provided");
        }

        return new IPSContentChangeHandler() {
            @Override
            public void handleEvent(PSEditorChangeEvent event) throws PSDataServiceException, PSNotFoundException {
                for (var handler : handlers) {
                    handler.handleEvent(event);
                }
            }

            @Override
            public void handleEvent(PSRelationshipChangeEvent event) throws PSDataServiceException, PSNotFoundException {
                for (var handler : handlers) {
                    handler.handleEvent(event);
                }
            }
        };
    }

    /**
     * Creates a handler that only processes events if they meet certain criteria.
     *
     * @param condition a predicate to test events before processing
     * @param handler the handler to execute if the condition is met
     * @return a conditional handler
     * @throws IllegalArgumentException if condition or handler is null
     */
    static IPSContentChangeHandler conditional(
            java.util.function.Predicate<PSEditorChangeEvent> condition,
            IPSContentChangeHandler handler) {
        Objects.requireNonNull(condition, "Condition cannot be null");
        Objects.requireNonNull(handler, "Handler cannot be null");

        return event -> {
            if (condition.test(event)) {
                handler.handleEvent(event);
            }
        };
    }
}

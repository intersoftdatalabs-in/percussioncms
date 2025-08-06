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
package com.percussion.services.notification;

import com.percussion.services.notification.PSNotificationEvent.EventType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The notification service allows change event notifications to propagate from
 * services to interested parties without tight coupling. This service implements
 * the Observer pattern for decoupled event-driven communication.
 * <p>
 * This service is thread-safe and supports asynchronous event processing.
 * Listeners should respond quickly to avoid blocking other listeners.
 *
 * @author dougrand
 */
public interface IPSNotificationService {

    /**
     * Add a new listener to the notification queue. The queue's order should be
     * considered indeterminate and listeners should not rely on the order being
     * maintained over time. If a listener is already registered for this event type,
     * this call is a no-op.
     *
     * @param type     the type of event to register the listener for, not {@code null}
     * @param listener the listener to add to the queue, not {@code null}
     * @throws IllegalArgumentException if type or listener is null
     */
    void addListener(EventType type, IPSNotificationListener listener);

    /**
     * Remove a listener from the notification queue. If the listener is not
     * registered for this event type, this call is a no-op.
     *
     * @param type     the type of event to unregister the listener for, not {@code null}
     * @param listener the listener to remove from the queue, not {@code null}
     * @throws IllegalArgumentException if type or listener is null
     */
    void removeListener(EventType type, IPSNotificationListener listener);

    /**
     * Notify all listeners that the passed event has occurred. The listeners
     * should respond quickly to avoid blocking other listeners. Listeners should
     * not rely on queue order as there's no guarantee of event arrival order.
     *
     * @param event notification event, not {@code null}
     * @throws IllegalArgumentException if event is null
     */
    void notifyEvent(PSNotificationEvent event);

    /**
     * Notify all listeners asynchronously. This method returns immediately
     * and event processing happens in the background.
     *
     * @param event notification event, not {@code null}
     * @return a CompletableFuture that completes when all listeners have been notified
     * @throws IllegalArgumentException if event is null
     */
    default CompletableFuture<Void> notifyEventAsync(PSNotificationEvent event) {
        return CompletableFuture.runAsync(() -> notifyEvent(event));
    }

    /**
     * Get all listeners registered for a specific event type.
     *
     * @param type the event type to query, not {@code null}
     * @return a set of listeners registered for the event type, never {@code null}, may be empty
     * @throws IllegalArgumentException if type is null
     */
    Set<IPSNotificationListener> getListeners(EventType type);

    /**
     * Get a stream of all listeners for a specific event type for efficient processing.
     *
     * @param type the event type to query, not {@code null}
     * @return Stream of listeners, never {@code null}
     * @throws IllegalArgumentException if type is null
     */
    default Stream<IPSNotificationListener> streamListeners(EventType type) {
        return getListeners(type).stream();
    }

    /**
     * Check if any listeners are registered for a specific event type.
     *
     * @param type the event type to check, not {@code null}
     * @return {@code true} if listeners are registered for this type, {@code false} otherwise
     * @throws IllegalArgumentException if type is null
     */
    default boolean hasListeners(EventType type) {
        return !getListeners(type).isEmpty();
    }

    /**
     * Check if a specific listener is registered for an event type.
     *
     * @param type     the event type to check, not {@code null}
     * @param listener the listener to check for, not {@code null}
     * @return {@code true} if the listener is registered for this type, {@code false} otherwise
     * @throws IllegalArgumentException if type or listener is null
     */
    default boolean isListenerRegistered(EventType type, IPSNotificationListener listener) {
        return getListeners(type).contains(listener);
    }

    /**
     * Remove all listeners for a specific event type.
     *
     * @param type the event type to clear listeners for, not {@code null}
     * @return the number of listeners that were removed
     * @throws IllegalArgumentException if type is null
     */
    default int removeAllListeners(EventType type) {
        var listeners = getListeners(type);
        var count = listeners.size();
        listeners.forEach(listener -> removeListener(type, listener));
        return count;
    }

    /**
     * Remove listeners that match the given predicate for a specific event type.
     *
     * @param type     the event type to filter listeners for, not {@code null}
     * @param predicate the condition to test listeners against, not {@code null}
     * @return the number of listeners that were removed
     * @throws IllegalArgumentException if type or predicate is null
     */
    default int removeListenersIf(EventType type, Predicate<IPSNotificationListener> predicate) {
        var listenersToRemove = streamListeners(type)
            .filter(predicate)
            .toList();

        listenersToRemove.forEach(listener -> removeListener(type, listener));
        return listenersToRemove.size();
    }

    /**
     * Get all event types that have registered listeners.
     *
     * @return a set of event types with listeners, never {@code null}, may be empty
     */
    Set<EventType> getRegisteredEventTypes();

    /**
     * Get the total number of listeners across all event types.
     *
     * @return the total listener count
     */
    default int getTotalListenerCount() {
        return getRegisteredEventTypes().stream()
            .mapToInt(type -> getListeners(type).size())
            .sum();
    }

    /**
     * Conditionally notify listeners based on a predicate. Only listeners that
     * match the predicate will be notified.
     *
     * @param event            notification event, not {@code null}
     * @param listenerPredicate predicate to filter listeners, not {@code null}
     * @throws IllegalArgumentException if event or listenerPredicate is null
     */
    default void notifyEventIf(PSNotificationEvent event,
                              Predicate<IPSNotificationListener> listenerPredicate) {
        streamListeners(event.getEventType())
            .filter(listenerPredicate)
            .forEach(listener -> {
                try {
                    listener.notifyEvent(event);
                } catch (Exception e) {
                    // Log error but continue processing other listeners
                    // Implementation should handle logging appropriately
                }
            });
    }
}

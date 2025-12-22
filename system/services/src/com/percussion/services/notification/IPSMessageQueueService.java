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

package com.percussion.services.notification;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * A service that provides message queuing for various message types.
 * Only one listener can listen to a distinct message type (Class).
 * This service supports asynchronous message processing with type-safe listeners.
 *
 * @author adamgent
 */
public interface IPSMessageQueueService {

    /**
     * Default message priority when none is specified.
     */
    int DEFAULT_PRIORITY = 4;

    /**
     * High priority for urgent messages.
     */
    int HIGH_PRIORITY = 1;

    /**
     * Low priority for background processing.
     */
    int LOW_PRIORITY = 9;

    /**
     * Sends a message to the queue with default priority.
     * <p>
     * The exact concrete class of the message is used to determine what
     * {@link IPSMessageQueueListener listener} to dispatch to.
     * If there is no {@link IPSMessageQueueListener listener} listening for that concrete type
     * then a warning may be logged.
     * <p>
     * The call is asynchronous and should return immediately.
     *
     * @param <T> The type of message
     * @param message the message to send to the queue, must be {@link Serializable}, not {@code null}
     * @throws IllegalArgumentException if message is null
     * @see #addListener(Class, IPSMessageQueueListener)
     */
    default <T extends Serializable> void sendMessage(T message) {
        sendMessage(message, DEFAULT_PRIORITY);
    }

    /**
     * Sends a message to the queue with specified priority.
     * <p>
     * The exact concrete class of the message is used to determine what
     * {@link IPSMessageQueueListener listener} to dispatch to.
     * If there is no {@link IPSMessageQueueListener listener} listening for that concrete type
     * then a warning may be logged.
     * <p>
     * The call is asynchronous and should return immediately.
     *
     * @param <T> The type of message
     * @param message the message to send to the queue, must be {@link Serializable}, not {@code null}
     * @param priority the priority which is based on JMS, {@code null} indicates default priority
     * @throws IllegalArgumentException if message is null
     * @see #addListener(Class, IPSMessageQueueListener)
     */
    <T extends Serializable> void sendMessage(T message, Integer priority);

    /**
     * Sends a message asynchronously and returns a CompletableFuture for tracking completion.
     *
     * @param <T> The type of message
     * @param message the message to send to the queue, not {@code null}
     * @param priority the priority, {@code null} indicates default priority
     * @return a CompletableFuture that completes when the message has been processed
     * @throws IllegalArgumentException if message is null
     */
    default <T extends Serializable> CompletableFuture<Void> sendMessageAsync(T message, Integer priority) {
        return CompletableFuture.runAsync(() -> sendMessage(message, priority));
    }

    /**
     * Sends a high-priority message to the queue.
     *
     * @param <T> The type of message
     * @param message the urgent message to send, not {@code null}
     * @throws IllegalArgumentException if message is null
     */
    default <T extends Serializable> void sendUrgentMessage(T message) {
        sendMessage(message, HIGH_PRIORITY);
    }

    /**
     * Sends a low-priority message to the queue for background processing.
     *
     * @param <T> The type of message
     * @param message the background message to send, not {@code null}
     * @throws IllegalArgumentException if message is null
     */
    default <T extends Serializable> void sendBackgroundMessage(T message) {
        sendMessage(message, LOW_PRIORITY);
    }

    /**
     * Adds a listener to a distinct concrete type (Class).
     * There may only be one listener per message type.
     *
     * @param <T> the type of message that the listener will receive
     * @param messageType the type of message that the listener will receive,
     *                    should not be an abstract or interface class, not {@code null}
     * @param listener the message listener, not {@code null}
     * @throws IllegalArgumentException if messageType or listener is null
     */
    <T extends Serializable> void addListener(Class<T> messageType, IPSMessageQueueListener<T> listener);

    /**
     * Removes the listener for the given type. If there is no listener registered
     * for this type, this call is a no-op.
     *
     * @param <T> the type of message that the listener would have received
     * @param messageType the message type to unregister, not {@code null}
     * @throws IllegalArgumentException if messageType is null
     */
    <T extends Serializable> void removeListener(Class<T> messageType);

    /**
     * Gets the listener for a specific message type.
     *
     * @param <T> the type of message
     * @param messageType the message type to query, not {@code null}
     * @return an Optional containing the listener if registered, empty otherwise
     * @throws IllegalArgumentException if messageType is null
     */
    <T extends Serializable> Optional<IPSMessageQueueListener<T>> getListener(Class<T> messageType);

    /**
     * Check if a listener is registered for a specific message type.
     *
     * @param messageType the message type to check, not {@code null}
     * @return {@code true} if a listener is registered for this type, {@code false} otherwise
     * @throws IllegalArgumentException if messageType is null
     */
    default boolean hasListener(Class<? extends Serializable> messageType) {
        return getListener(messageType).isPresent();
    }

    /**
     * Get all message types that have registered listeners.
     *
     * @return a set of message types with listeners, never {@code null}, may be empty
     */
    Set<Class<? extends Serializable>> getRegisteredMessageTypes();

    /**
     * Get a stream of all registered message types for efficient processing.
     *
     * @return Stream of message types, never {@code null}
     */
    default Stream<Class<? extends Serializable>> streamRegisteredMessageTypes() {
        return getRegisteredMessageTypes().stream();
    }

    /**
     * Get the total number of registered listeners.
     *
     * @return the number of registered listeners
     */
    default int getListenerCount() {
        return getRegisteredMessageTypes().size();
    }

    /**
     * Remove all registered listeners.
     *
     * @return the number of listeners that were removed
     */
    default int removeAllListeners() {
        var messageTypes = getRegisteredMessageTypes();
        var count = messageTypes.size();
        messageTypes.forEach(this::removeListener);
        return count;
    }

    /**
     * Check if the message queue service is active and accepting messages.
     *
     * @return {@code true} if the service is active, {@code false} otherwise
     */
    default boolean isActive() {
        return true; // Default implementation assumes service is always active
    }

    /**
     * Get statistics about the message queue service.
     *
     * @return a string representation of queue statistics
     */
    default String getQueueStatistics() {
        return String.format("Message Queue Statistics: %d registered listener types", getListenerCount());
    }
}

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

import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.utils.guid.IPSGuid;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Helper for notifying events with Java 11 modernization. Wraps the notification service
 * into a series of methods that dispatch specific events with enhanced type safety and validation.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Optional-based safe service access</li>
 * <li>CompletableFuture support for asynchronous notifications</li>
 * <li>Factory methods for type-safe event creation</li>
 * <li>Utility class design with private constructor</li>
 * </ul>
 *
 * @author dougrand
 */
public final class PSNotificationHelper {

    /**
     * Private constructor to prevent instantiation.
     */
    private PSNotificationHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * General method to notify an event with enhanced validation.
     *
     * @param type the type of the event, never {@code null}
     * @param target the target of the event, see {@link EventType} for details,
     *               never {@code null}
     * @throws IllegalArgumentException if type or target is null
     */
    public static void notifyEvent(EventType type, Serializable target) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(target, "target cannot be null");

        var notificationService = PSNotificationServiceLocator.getNotificationService();
        var event = PSNotificationEvent.create(type, target);
        notificationService.notifyEvent(event);
    }

    /**
     * Safely notify an event, returning an Optional result for error handling.
     * This method catches exceptions and returns them wrapped in an Optional.
     *
     * @param type the type of the event, never {@code null}
     * @param target the target of the event, never {@code null}
     * @return an Optional containing any exception that occurred, empty if successful
     * @throws IllegalArgumentException if type or target is null
     */
    public static Optional<Exception> notifyEventSafely(EventType type, Serializable target) {
        try {
            notifyEvent(type, target);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    /**
     * Asynchronously notify an event. This method returns immediately
     * and processes the event in the background.
     *
     * @param type the type of the event, never {@code null}
     * @param target the target of the event, never {@code null}
     * @return a CompletableFuture that completes when processing is done
     * @throws IllegalArgumentException if type or target is null
     */
    public static CompletableFuture<Void> notifyEventAsync(EventType type, Serializable target) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(target, "target cannot be null");

        return CompletableFuture.runAsync(() -> notifyEvent(type, target));
    }

    /**
     * Notify an object invalidation using factory method for type safety.
     *
     * @param id the id of the object being invalidated, never {@code null}
     * @throws IllegalArgumentException if id is null
     */
    public static void notifyInvalidation(IPSGuid id) {
        Objects.requireNonNull(id, "id cannot be null");
        var notificationService = PSNotificationServiceLocator.getNotificationService();
        var event = PSNotificationEvent.createObjectInvalidationEvent(id);
        notificationService.notifyEvent(event);
    }

    /**
     * Safely notify an object invalidation, returning error information if needed.
     *
     * @param id the id of the object being invalidated, never {@code null}
     * @return an Optional containing any exception that occurred, empty if successful
     * @throws IllegalArgumentException if id is null
     */
    public static Optional<Exception> notifyInvalidationSafely(IPSGuid id) {
        try {
            notifyInvalidation(id);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    /**
     * Asynchronously notify an object invalidation.
     *
     * @param id the id of the object being invalidated, never {@code null}
     * @return a CompletableFuture that completes when processing is done
     * @throws IllegalArgumentException if id is null
     */
    public static CompletableFuture<Void> notifyInvalidationAsync(IPSGuid id) {
        Objects.requireNonNull(id, "id cannot be null");
        return CompletableFuture.runAsync(() -> notifyInvalidation(id));
    }

    /**
     * Notify a file modification using factory method for type safety.
     *
     * @param file the file being modified, never {@code null}
     * @throws IllegalArgumentException if file is null
     */
    public static void notifyFile(File file) {
        Objects.requireNonNull(file, "file cannot be null");
        var notificationService = PSNotificationServiceLocator.getNotificationService();
        var event = PSNotificationEvent.createFileEvent(file);
        notificationService.notifyEvent(event);
    }

    /**
     * Safely notify a file modification, returning error information if needed.
     *
     * @param file the file being modified, never {@code null}
     * @return an Optional containing any exception that occurred, empty if successful
     * @throws IllegalArgumentException if file is null
     */
    public static Optional<Exception> notifyFileSafely(File file) {
        try {
            notifyFile(file);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    /**
     * Asynchronously notify a file modification.
     *
     * @param file the file being modified, never {@code null}
     * @return a CompletableFuture that completes when processing is done
     * @throws IllegalArgumentException if file is null
     */
    public static CompletableFuture<Void> notifyFileAsync(File file) {
        Objects.requireNonNull(file, "file cannot be null");
        return CompletableFuture.runAsync(() -> notifyFile(file));
    }

    /**
     * Notify that Core Server is initialized, but no packages have been installed yet.
     * This method sends both CORE_SERVER_INITIALIZED and CORE_SERVER_POST_INIT events.
     *
     * @param serverRoot The root directory (identifier) of Server, never {@code null}
     * @throws IllegalArgumentException if serverRoot is null
     */
    public static void notifyServerInitComplete(File serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        var notificationService = PSNotificationServiceLocator.getNotificationService();

        // Send initialization complete event
        var initEvent = PSNotificationEvent.createServerEvent(EventType.CORE_SERVER_INITIALIZED);
        notificationService.notifyEvent(initEvent);

        // Send post-initialization event
        var postInitEvent = PSNotificationEvent.createServerEvent(EventType.CORE_SERVER_POST_INIT);
        notificationService.notifyEvent(postInitEvent);
    }

    /**
     * Safely notify server initialization complete, returning error information if needed.
     *
     * @param serverRoot The root directory (identifier) of Server, never {@code null}
     * @return an Optional containing any exception that occurred, empty if successful
     * @throws IllegalArgumentException if serverRoot is null
     */
    public static Optional<Exception> notifyServerInitCompleteSafely(File serverRoot) {
        try {
            notifyServerInitComplete(serverRoot);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    /**
     * Asynchronously notify server initialization complete.
     *
     * @param serverRoot The root directory (identifier) of Server, never {@code null}
     * @return a CompletableFuture that completes when processing is done
     * @throws IllegalArgumentException if serverRoot is null
     */
    public static CompletableFuture<Void> notifyServerInitCompleteAsync(File serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        return CompletableFuture.runAsync(() -> notifyServerInitComplete(serverRoot));
    }

    /**
     * Notify server shutdown using server event factory method.
     *
     * @param serverRoot The root directory (identifier) of Server, never {@code null}
     * @throws IllegalArgumentException if serverRoot is null
     */
    public static void notifyServerShutdown(File serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        var notificationService = PSNotificationServiceLocator.getNotificationService();
        var event = PSNotificationEvent.createServerEvent(EventType.CORE_SERVER_SHUTDOWN);
        notificationService.notifyEvent(event);
    }

    /**
     * Safely notify server shutdown, returning error information if needed.
     *
     * @param serverRoot The root directory (identifier) of Server, never {@code null}
     * @return an Optional containing any exception that occurred, empty if successful
     * @throws IllegalArgumentException if serverRoot is null
     */
    public static Optional<Exception> notifyServerShutdownSafely(File serverRoot) {
        try {
            notifyServerShutdown(serverRoot);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    /**
     * Asynchronously notify server shutdown.
     *
     * @param serverRoot The root directory (identifier) of Server, never {@code null}
     * @return a CompletableFuture that completes when processing is done
     * @throws IllegalArgumentException if serverRoot is null
     */
    public static CompletableFuture<Void> notifyServerShutdownAsync(File serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        return CompletableFuture.runAsync(() -> notifyServerShutdown(serverRoot));
    }

    /**
     * Notify content change using factory method for type safety.
     *
     * @param contentGuid the GUID of the changed content, never {@code null}
     * @throws IllegalArgumentException if contentGuid is null
     */
    public static void notifyContentChange(IPSGuid contentGuid) {
        Objects.requireNonNull(contentGuid, "contentGuid cannot be null");
        var notificationService = PSNotificationServiceLocator.getNotificationService();
        var event = PSNotificationEvent.createContentChangeEvent(contentGuid);
        notificationService.notifyEvent(event);
    }

    /**
     * Safely notify content change, returning error information if needed.
     *
     * @param contentGuid the GUID of the changed content, never {@code null}
     * @return an Optional containing any exception that occurred, empty if successful
     * @throws IllegalArgumentException if contentGuid is null
     */
    public static Optional<Exception> notifyContentChangeSafely(IPSGuid contentGuid) {
        try {
            notifyContentChange(contentGuid);
            return Optional.empty();
        } catch (Exception e) {
            return Optional.of(e);
        }
    }

    /**
     * Asynchronously notify content change.
     *
     * @param contentGuid the GUID of the changed content, never {@code null}
     * @return a CompletableFuture that completes when processing is done
     * @throws IllegalArgumentException if contentGuid is null
     */
    public static CompletableFuture<Void> notifyContentChangeAsync(IPSGuid contentGuid) {
        Objects.requireNonNull(contentGuid, "contentGuid cannot be null");
        return CompletableFuture.runAsync(() -> notifyContentChange(contentGuid));
    }

    /**
     * Check if the notification service is available before sending notifications.
     *
     * @return {@code true} if the service is available, {@code false} otherwise
     */
    public static boolean isNotificationServiceAvailable() {
        return PSNotificationServiceLocator.isNotificationServiceAvailable();
    }

    /**
     * Get the notification service safely for advanced usage.
     *
     * @return an Optional containing the notification service if available, empty otherwise
     */
    public static Optional<IPSNotificationService> getNotificationServiceSafely() {
        return PSNotificationServiceLocator.getNotificationServiceSafely();
    }
}

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

// REFACTORED: CP-JAVA11
package com.percussion.services.notification;

import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.exception.PSDataServiceException;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A notification listener is informed of changes that have been reported to the
 * notification service. This interface supports both synchronous and asynchronous
 * event processing patterns.
 *
 * <p>Implementations should be lightweight and respond quickly to avoid blocking
 * other listeners. For heavy processing, consider using the asynchronous methods
 * or storing state and handling the response in a separate thread.</p>
 *
 * @author dougrand
 */
@FunctionalInterface
public interface IPSNotificationListener {

   /**
    * Notify the listener of an event. An implementer of this method should take
    * care to not spend a great deal of time responding to the notification.
    * Instead, state should be stored and the actual response handled
    * asynchronously.
    *
    * <p>A listener can count on only being called for the event types that it was
    * registered to. Once it knows the type, it should check the information
    * passed to see if the event should be handled. It should not modify any
    * data passed with the event.</p>
    *
    * @param notification the notification event, never {@code null}
    * @throws PSDataServiceException if there's a data service error during processing
    * @throws PSNotFoundException if a required resource is not found during processing
    * @throws IllegalArgumentException if notification is null
    */
   void notifyEvent(PSNotificationEvent notification) throws PSDataServiceException, PSNotFoundException;

   /**
    * Notify the listener asynchronously. This default implementation processes
    * the event in a separate thread and returns a CompletableFuture.
    *
    * @param notification the notification event, never {@code null}
    * @return a CompletableFuture that completes when processing is done
    * @throws IllegalArgumentException if notification is null
    */
   default CompletableFuture<Void> notifyEventAsync(PSNotificationEvent notification) {
      Objects.requireNonNull(notification, "Notification cannot be null");

      return CompletableFuture.runAsync(() -> {
         try {
            notifyEvent(notification);
         } catch (PSDataServiceException | PSNotFoundException e) {
            throw new RuntimeException("Error processing notification asynchronously", e);
         }
      });
   }

   /**
    * Check if this listener can handle the specified event type.
    * Default implementation returns true for all event types.
    *
    * @param eventType the event type to check, never {@code null}
    * @return {@code true} if this listener can handle the event type, {@code false} otherwise
    * @throws IllegalArgumentException if eventType is null
    */
   default boolean canHandle(PSNotificationEvent.EventType eventType) {
      Objects.requireNonNull(eventType, "Event type cannot be null");
      return true;
   }

   /**
    * Get a descriptive name for this listener, useful for debugging and logging.
    * Default implementation returns the class simple name.
    *
    * @return a descriptive name for this listener, never {@code null}
    */
   default String getListenerName() {
      return this.getClass().getSimpleName();
   }
}

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
package com.percussion.services.notification.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.system.utils.PSBaseBean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Modern thread-safe notification service implementation using Java 11 patterns.
 *
 * <p>This implementation provides comprehensive event notification capabilities with
 * thread-safe operations, efficient listener management, and robust error handling.
 * It uses ConcurrentHashMap for event type mapping and CopyOnWriteArrayList for
 * listener collections to ensure thread safety and performance.</p>
 *
 * @author dougrand
 */
@PSBaseBean("sys_notificationService")
public final class PSNotificationService implements IPSNotificationService {

   private static final Logger log = LogManager.getLogger(PSNotificationService.class);

   /**
    * Thread-safe map storing listeners by event type.
    * Uses CopyOnWriteArrayList for listener collections to optimize for read-heavy operations.
    */
   private final Map<EventType, Collection<IPSNotificationListener>> listenerMap =
         new ConcurrentHashMap<>(16, 0.75f, 2);

   @Override
   public void notifyEvent(PSNotificationEvent event) {
      Objects.requireNonNull(event, "Notification event cannot be null");

      var listeners = getListeners(event.getEventType());

      if (log.isDebugEnabled()) {
         log.debug("Notifying {} listeners for event type: {}", listeners.size(), event.getEventType());
      }

      for (var listener : listeners) {
         try {
            listener.notifyEvent(event);
         } catch (Exception e) {
            log.error("Error notifying listener {}: {}",
                  listener.getClass().getSimpleName(),
                  PSExceptionUtils.getMessageForLog(e));
            log.debug("Notification error details", e);
         }
      }
   }

   @Override
   public void addListener(EventType type, IPSNotificationListener listener) {
      Objects.requireNonNull(type, "Event type cannot be null");
      Objects.requireNonNull(listener, "Listener cannot be null");

      var listeners = getOrCreateListenerCollection(type);

      // CopyOnWriteArrayList handles concurrent modifications safely
      if (!listeners.contains(listener)) {
         listeners.add(listener);
         log.debug("Added listener {} for event type: {}",
               listener.getClass().getSimpleName(), type);
      }
   }

   @Override
   public void removeListener(EventType type, IPSNotificationListener listener) {
      Objects.requireNonNull(type, "Event type cannot be null");
      Objects.requireNonNull(listener, "Listener cannot be null");

      var listeners = listenerMap.get(type);
      if (listeners != null) {
         var removed = listeners.remove(listener);
         if (removed) {
            log.debug("Removed listener {} for event type: {}",
                  listener.getClass().getSimpleName(), type);
         }
      }
   }

   @Override
   public Set<IPSNotificationListener> getListeners(EventType type) {
      Objects.requireNonNull(type, "Event type cannot be null");

      var listeners = listenerMap.get(type);
      return listeners != null ? Set.copyOf(listeners) : Set.of();
   }

   @Override
   public Set<EventType> getRegisteredEventTypes() {
      return Set.copyOf(listenerMap.keySet());
   }

   /**
    * Gets or creates a listener collection for the specified event type.
    * Uses double-checked locking for thread-safe lazy initialization.
    *
    * @param type the event type, never {@code null}
    * @return the listener collection for the event type, never {@code null}
    */
   private Collection<IPSNotificationListener> getOrCreateListenerCollection(EventType type) {
      return listenerMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
   }

   /**
    * Gets statistics about the notification service state.
    *
    * @return a map of event types to listener counts for monitoring purposes
    */
   public Map<EventType, Integer> getListenerStatistics() {
      var stats = new HashMap<EventType, Integer>();
      listenerMap.forEach((type, listeners) -> stats.put(type, listeners.size()));
      return Map.copyOf(stats);
   }

   /**
    * Clears all listeners for all event types. Use with caution.
    */
   public void clearAllListeners() {
      var clearedCount = getTotalListenerCount();
      listenerMap.clear();
      log.info("Cleared all {} listeners from notification service", clearedCount);
   }

   /**
    * Gets a summary of the notification service state for debugging.
    *
    * @return formatted string describing current state
    */
   public String getServiceSummary() {
      var totalListeners = getTotalListenerCount();
      var eventTypes = getRegisteredEventTypes().size();

      return String.format("NotificationService: %d event types, %d total listeners",
            eventTypes, totalListeners);
   }
}

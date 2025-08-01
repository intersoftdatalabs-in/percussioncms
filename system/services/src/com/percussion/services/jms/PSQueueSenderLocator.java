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
package com.percussion.services.jms;

import com.percussion.services.PSBaseServiceLocator;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Service locator for JMS Queue Senders using modern Java 11 patterns.
 * Provides thread-safe access to queue sender instances with enhanced caching
 * and validation for reliable messaging infrastructure.
 *
 * @author Percussion Software
 */
public final class PSQueueSenderLocator {

   /**
    * Default bean name for the main queue sender.
    */
   public static final String DEFAULT_QUEUE_SENDER_BEAN = "sys_queueSender";

   /**
    * Thread-safe cache for queue sender instances by bean name.
    */
   private static final ConcurrentHashMap<String, IPSQueueSender> QUEUE_SENDER_CACHE =
       new ConcurrentHashMap<>();

   /**
    * Atomic reference to the default queue sender instance.
    */
   private static final AtomicReference<IPSQueueSender> DEFAULT_SENDER_REF =
       new AtomicReference<>();

   /**
    * Private constructor to prevent instantiation.
    */
   private PSQueueSenderLocator() {
      // Utility class - no instantiation
   }

   /**
    * Gets the queue sender from the supplied bean name with enhanced caching and validation.
    *
    * @param beanName the name of the Spring Bean of the Queue Sender, never null or empty
    * @return the Queue Sender, never null in a correct configuration
    * @throws IllegalArgumentException if beanName is null or empty
    * @throws IllegalStateException if the bean cannot be found or initialized
    */
   public static IPSQueueSender getQueueSender(String beanName) {
      Objects.requireNonNull(beanName, "Bean name cannot be null");
      if (beanName.trim().isEmpty()) {
         throw new IllegalArgumentException("Bean name cannot be empty");
      }

      return QUEUE_SENDER_CACHE.computeIfAbsent(beanName, name -> {
         var sender = (IPSQueueSender) PSBaseServiceLocator.getBean(name);
         Objects.requireNonNull(sender, "Queue sender bean '" + name + "' cannot be null");
         return sender;
      });
   }

   /**
    * Gets the default queue sender with lazy initialization and thread-safe caching.
    *
    * @return the default Queue Sender, never null
    * @throws IllegalStateException if the default sender cannot be initialized
    */
   public static IPSQueueSender getDefaultQueueSender() {
      var sender = DEFAULT_SENDER_REF.get();
      if (sender == null) {
         sender = DEFAULT_SENDER_REF.updateAndGet(current ->
            current != null ? current : getQueueSender(DEFAULT_QUEUE_SENDER_BEAN));
      }
      return sender;
   }

   /**
    * Gets a queue sender with safe access, returning Optional for null-safe operations.
    *
    * @param beanName the name of the Spring Bean of the Queue Sender
    * @return Optional containing the Queue Sender if found, empty if not available
    */
   public static Optional<IPSQueueSender> findQueueSender(String beanName) {
      if (beanName == null || beanName.trim().isEmpty()) {
         return Optional.empty();
      }

      try {
         return Optional.of(getQueueSender(beanName));
      } catch (Exception e) {
         // Log the exception if needed, but return empty Optional
         return Optional.empty();
      }
   }

   /**
    * Creates a supplier for lazy queue sender retrieval.
    *
    * @param beanName the bean name for the queue sender
    * @return Supplier that provides the queue sender when called
    * @throws IllegalArgumentException if beanName is null or empty
    */
   public static Supplier<IPSQueueSender> createQueueSenderSupplier(String beanName) {
      Objects.requireNonNull(beanName, "Bean name cannot be null");
      if (beanName.trim().isEmpty()) {
         throw new IllegalArgumentException("Bean name cannot be empty");
      }

      return () -> getQueueSender(beanName);
   }

   /**
    * Clear the cache for testing or reconfiguration purposes.
    * This method is primarily intended for unit testing scenarios.
    */
   public static void clearCache() {
      QUEUE_SENDER_CACHE.clear();
      DEFAULT_SENDER_REF.set(null);
   }

   /**
    * Check if a queue sender is currently cached.
    *
    * @param beanName the bean name to check
    * @return true if the sender is cached, false otherwise
    */
   public static boolean isCached(String beanName) {
      return beanName != null && QUEUE_SENDER_CACHE.containsKey(beanName);
   }

   /**
    * Get the number of cached queue sender instances.
    *
    * @return the cache size
    */
   public static int getCacheSize() {
      return QUEUE_SENDER_CACHE.size();
   }
}

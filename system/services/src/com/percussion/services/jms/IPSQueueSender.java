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

import com.percussion.services.publisher.IPSEdition;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * A JMS client for sending messages to message queue destinations with modern Java 11 patterns.
 * Provides comprehensive message queuing capabilities with priority-based delivery, batch operations,
 * and enhanced validation for reliable messaging infrastructure.
 *
 * @author Percussion Software
 */
public interface IPSQueueSender {

   /**
    * Message priority enumeration with enhanced utility methods for type-safe priority handling.
    */
   enum Priority {
      /** Highest priority for out-of-bound messages like start/end publishing jobs */
      HIGHEST(IPSEdition.Priority.HIGHEST.getValue() + 1, "Highest priority for system messages"),

      /** High priority for urgent operations */
      HIGH(IPSEdition.Priority.HIGH.getValue(), "High priority for urgent operations"),

      /** Medium priority for standard operations */
      MEDIUM(IPSEdition.Priority.MEDIUM.getValue(), "Medium priority for standard operations"),

      /** Low priority for background operations */
      LOW(IPSEdition.Priority.LOW.getValue(), "Low priority for background operations"),

      /** Lowest priority for non-critical operations */
      LOWEST(IPSEdition.Priority.LOWEST.getValue(), "Lowest priority for non-critical operations");

      private final int value;
      private final String description;

      Priority(int value, String description) {
         this.value = value;
         this.description = description;
      }

      /**
       * Get the numeric value for this priority.
       *
       * @return the priority value
       */
      public int getValue() {
         return value;
      }

      /**
       * Get a human-readable description of this priority.
       *
       * @return the description, never null
       */
      public String getDescription() {
         return description;
      }

      /**
       * Check if this priority is higher than another priority.
       *
       * @param other the priority to compare against
       * @return true if this priority is higher, false otherwise
       */
      public boolean isHigherThan(Priority other) {
         return this.value > other.value;
      }

      /**
       * Find a Priority by its numeric value.
       *
       * @param value the priority value to search for
       * @return Optional containing the matching Priority, empty if not found
       */
      public static Optional<Priority> fromValue(int value) {
         return Stream.of(values())
            .filter(priority -> priority.value == value)
            .findFirst();
      }
   }

   /**
    * Legacy priority constants for backward compatibility.
    * @deprecated Use {@link Priority} enum instead
    */
   @Deprecated
   int PRIORITY_HIGHEST = Priority.HIGHEST.getValue();

   /**
    * Legacy priority constants for backward compatibility.
    * @deprecated Use {@link Priority} enum instead
    */
   @Deprecated
   int PRIORITY_LOWEST = Priority.LOWEST.getValue();

   /**
    * Sends a message to the queue with the specified priority using enhanced validation.
    *
    * @param msg the message to send, never null
    * @param priority the priority for the message, should be between PRIORITY_LOWEST and PRIORITY_HIGHEST
    * @throws IllegalArgumentException if msg is null or priority is invalid
    */
   void sendMessage(Serializable msg, int priority);
   
   /**
    * Sends a message to the queue with type-safe priority handling.
    *
    * @param msg the message to send, never null
    * @param priority the priority enum for the message, never null
    * @throws IllegalArgumentException if msg or priority is null
    */
   default void sendMessage(Serializable msg, Priority priority) {
      Objects.requireNonNull(msg, "Message cannot be null");
      Objects.requireNonNull(priority, "Priority cannot be null");
      sendMessage(msg, priority.getValue());
   }

   /**
    * Sends a message to the queue with medium priority.
    *
    * @param msg the message to send, never null
    * @throws IllegalArgumentException if msg is null
    */
   default void sendMessage(Serializable msg) {
      sendMessage(msg, Priority.MEDIUM);
   }

   /**
    * Sends a collection of messages to the queue with the specified priority using enhanced validation.
    *
    * @param msgs the collection of messages to send, never null but may be empty
    * @param priority the priority for the messages, should be between PRIORITY_LOWEST and PRIORITY_HIGHEST
    * @throws IllegalArgumentException if msgs is null or priority is invalid
    */
   void sendMessages(List<? extends Serializable> msgs, int priority);
   
   /**
    * Sends a collection of messages to the queue with type-safe priority handling.
    *
    * @param msgs the collection of messages to send, never null but may be empty
    * @param priority the priority enum for the messages, never null
    * @throws IllegalArgumentException if msgs or priority is null
    */
   default void sendMessages(Collection<? extends Serializable> msgs, Priority priority) {
      Objects.requireNonNull(msgs, "Messages collection cannot be null");
      Objects.requireNonNull(priority, "Priority cannot be null");
      sendMessages(List.copyOf(msgs), priority.getValue());
   }

   /**
    * Sends a sequence of messages to the queue with the specified priority using iterator pattern.
    *
    * @param msgs the iterator of messages to send, never null but may be empty
    * @param priority the priority for the messages, should be between PRIORITY_LOWEST and PRIORITY_HIGHEST
    * @throws IllegalArgumentException if msgs is null or priority is invalid
    */
   void sendMessages(Iterator<? extends Serializable> msgs, int priority);
   
   /**
    * Sends a stream of messages to the queue with type-safe priority handling.
    *
    * @param msgs the stream of messages to send, never null but may be empty
    * @param priority the priority enum for the messages, never null
    * @throws IllegalArgumentException if msgs or priority is null
    */
   default void sendMessages(Stream<? extends Serializable> msgs, Priority priority) {
      Objects.requireNonNull(msgs, "Messages stream cannot be null");
      Objects.requireNonNull(priority, "Priority cannot be null");
      var messageList = msgs.collect(java.util.stream.Collectors.toList());
      sendMessages(messageList, priority.getValue());
   }

   /**
    * Sends a collection of messages to the queue with medium priority.
    *
    * @param msgs the collection of messages to send, never null but may be empty
    * @throws IllegalArgumentException if msgs is null
    */
   default void sendMessages(List<? extends Serializable> msgs) {
      sendMessages(msgs, Priority.MEDIUM);
   }

   /**
    * Sends a single message asynchronously with the specified priority.
    *
    * @param msg the message to send, never null
    * @param priority the priority enum for the message, never null
    * @return CompletableFuture that completes when the message is sent
    * @throws IllegalArgumentException if msg or priority is null
    */
   default CompletableFuture<Void> sendMessageAsync(Serializable msg, Priority priority) {
      Objects.requireNonNull(msg, "Message cannot be null");
      Objects.requireNonNull(priority, "Priority cannot be null");

      return CompletableFuture.runAsync(() -> sendMessage(msg, priority));
   }

   /**
    * Sends a collection of messages asynchronously with the specified priority.
    *
    * @param msgs the collection of messages to send, never null but may be empty
    * @param priority the priority enum for the messages, never null
    * @return CompletableFuture that completes when all messages are sent
    * @throws IllegalArgumentException if msgs or priority is null
    */
   default CompletableFuture<Void> sendMessagesAsync(Collection<? extends Serializable> msgs, Priority priority) {
      Objects.requireNonNull(msgs, "Messages collection cannot be null");
      Objects.requireNonNull(priority, "Priority cannot be null");

      return CompletableFuture.runAsync(() -> sendMessages(msgs, priority));
   }

   /**
    * Validates that a priority value is within acceptable bounds.
    *
    * @param priority the priority value to validate
    * @return true if the priority is valid, false otherwise
    */
   default boolean isValidPriority(int priority) {
      return priority >= PRIORITY_LOWEST && priority <= PRIORITY_HIGHEST;
   }
}

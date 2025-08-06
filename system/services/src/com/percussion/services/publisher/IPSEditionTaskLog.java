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
package com.percussion.services.publisher;

import com.percussion.utils.guid.IPSGuid;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * This represents a single running of a single edition task. As each task is
 * run, the data is recorded in these records. This interface provides modern
 * Java 11 patterns for task execution logging with enhanced validation and
 * safe access methods.
 *
 * @author dougrand
 */
public interface IPSEditionTaskLog {

   /**
    * The reference ID is a unique value generated for each task and each item
    * published. The reference ID will not recycle within any reasonable time period.
    *
    * @return the reference ID, may be 0 for a new object that hasn't been initialized
    */
   long getReferenceId();

   /**
    * Set the reference ID.
    *
    * @param referenceId the reference ID to set
    */
   void setReferenceId(long referenceId);

   /**
    * The Hibernate version for optimistic locking and tracking persistent objects.
    *
    * @return the version, {@code null} if the object hasn't been persisted yet
    */
   Integer getVersion();

   /**
    * Get the version safely with Optional wrapper.
    *
    * @return Optional containing the version, or empty if not persisted
    */
   default Optional<Integer> getVersionSafely() {
      return Optional.ofNullable(getVersion());
   }

   /**
    * Set the version.
    *
    * @param version the version to set
    */
   void setVersion(Integer version);

   /**
    * A reference to the specific job that this task was run for.
    * 
    * @return the job ID, 0 only if uninitialized
    */
   long getJobId();

   /**
    * Set the job ID.
    *
    * @param jobId the job ID to set
    */
   void setJobId(long jobId);

   /**
    * Get the execution status of the task.
    *
    * @return {@code true} if the task executed successfully, {@code false} otherwise
    */
   boolean getStatus();

   /**
    * Set the execution status.
    *
    * @param status the status to set
    */
   void setStatus(boolean status);

   /**
    * Get the elapsed time to run the task in milliseconds.
    *
    * @return the elapsed time in milliseconds, may be {@code null}
    */
   Integer getElapsed();

   /**
    * Get the elapsed time safely with Optional wrapper.
    *
    * @return Optional containing the elapsed time, or empty if not set
    */
   default Optional<Integer> getElapsedSafely() {
      return Optional.ofNullable(getElapsed());
   }

   /**
    * Get the elapsed time as a Duration for modern time handling.
    *
    * @return Optional containing the elapsed time as Duration, or empty if not set
    */
   default Optional<Duration> getElapsedDuration() {
      return getElapsedSafely().map(Duration::ofMillis);
   }

   /**
    * Set the elapsed time.
    *
    * @param elapsed the elapsed time in milliseconds to set
    */
   void setElapsed(Integer elapsed);

   /**
    * Set the elapsed time using Duration for modern time handling.
    *
    * @param duration the elapsed duration, may be {@code null}
    */
   default void setElapsedDuration(Duration duration) {
      setElapsed(duration != null ? (int) duration.toMillis() : null);
   }

   /**
    * The referenced task that was executed.
    * 
    * @return the task ID, never {@code null} if the instance has been initialized
    */
   IPSGuid getTaskId();

   /**
    * Set the task ID.
    *
    * @param taskId the task ID to set, never {@code null}
    * @throws IllegalArgumentException if taskId is null
    */
   void setTaskId(IPSGuid taskId);

   /**
    * Get the message, if any, from the executed task. May be a failure
    * message or just feedback from the execution.
    *
    * @return the message, may be {@code null} or empty
    */
   String getMessage();

   /**
    * Get the message safely with Optional wrapper.
    *
    * @return Optional containing the message, or empty if not set
    */
   default Optional<String> getMessageSafely() {
      return Optional.ofNullable(getMessage())
                     .filter(msg -> !msg.trim().isEmpty());
   }

   /**
    * Set the message.
    *
    * @param message the message to set, may be {@code null}
    */
   void setMessage(String message);

   /**
    * Check if this task log represents a successful execution.
    *
    * @return true if the task executed successfully
    */
   default boolean isSuccessful() {
      return getStatus();
   }

   /**
    * Check if this task log represents a failed execution.
    *
    * @return true if the task failed
    */
   default boolean isFailed() {
      return !getStatus();
   }

   /**
    * Check if this task log has timing information.
    *
    * @return true if elapsed time is available
    */
   default boolean hasElapsedTime() {
      return getElapsedSafely().isPresent();
   }

   /**
    * Check if this task log has a message (success or failure).
    *
    * @return true if a message is present
    */
   default boolean hasMessage() {
      return getMessageSafely().isPresent();
   }

   /**
    * Check if this task log is fully initialized with required data.
    *
    * @return true if all required fields are properly set
    */
   default boolean isInitialized() {
      return getReferenceId() > 0 &&
             getJobId() > 0 &&
             getTaskId() != null;
   }

   /**
    * Get a summary string of this task execution for logging purposes.
    *
    * @return summary string containing key execution details
    */
   default String getSummary() {
      var status = isSuccessful() ? "SUCCESS" : "FAILED";
      var elapsed = getElapsedSafely().map(e -> e + "ms").orElse("unknown");
      var message = getMessageSafely().orElse("no message");

      return String.format("Task[ref=%d, job=%d, status=%s, elapsed=%s]: %s",
         getReferenceId(), getJobId(), status, elapsed, message);
   }

   /**
    * Compare this task log with another by reference ID for ordering.
    *
    * @param other the other task log to compare to, never {@code null}
    * @return comparison result based on reference ID
    * @throws IllegalArgumentException if other is null
    */
   default int compareByReferenceId(IPSEditionTaskLog other) {
      Objects.requireNonNull(other, "Other task log cannot be null");
      return Long.compare(getReferenceId(), other.getReferenceId());
   }
}

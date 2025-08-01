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
package com.percussion.services.publisher;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Each publishing job that runs has a status record. This keeps track of when
 * the job started and ended, what edition it was created for, and it forms the
 * parent record to aggregate all of the item and edition task logging for the
 * job. This interface provides modern Java 11 patterns for publishing job
 * status tracking with enhanced validation and safe access methods.
 *
 * @author dougrand
 */
public interface IPSPubStatus {

   /**
    * The ending state of a publishing job with enhanced utility methods.
    */
   enum EndingState {
      /** Starting status for a publishing job */
      STARTED("Job started"),

      /** The status for a publishing job is finished normally */
      COMPLETED("Job completed successfully"),

      /** The status for a publishing job is finished, but with failed items */
      COMPLETED_W_FAILURE("Job completed with failures"),

      /** The status for a publishing job is canceled by a user */
      CANCELED_BY_USER("Job canceled by user"),

      /** The status for a publishing job is terminated abnormally */
      ABORTED("Job aborted"),

      /** The status for a publishing job is terminated abnormally as new datasource not found */
      RESTARTNEEDED("Job needs restart - datasource not found");

      private final String description;

      EndingState(String description) {
         this.description = description;
      }

      /**
       * Get a human-readable description of this state.
       *
       * @return description string, never null
       */
      public String getDescription() {
         return description;
      }

      /**
       * Check if this state indicates the job is still running.
       *
       * @return true if the job is in progress
       */
      public boolean isRunning() {
         return this == STARTED;
      }

      /**
       * Check if this state indicates the job completed successfully.
       *
       * @return true if the job completed successfully
       */
      public boolean isSuccessful() {
         return this == COMPLETED;
      }

      /**
       * Check if this state indicates the job failed or was terminated.
       *
       * @return true if the job failed
       */
      public boolean isFailure() {
         return Set.of(COMPLETED_W_FAILURE, CANCELED_BY_USER, ABORTED, RESTARTNEEDED).contains(this);
      }

      /**
       * Check if this state indicates the job was terminated externally.
       *
       * @return true if the job was canceled or aborted
       */
      public boolean isTerminated() {
         return Set.of(CANCELED_BY_USER, ABORTED).contains(this);
      }

      /**
       * Check if this state indicates the job is finished (completed or failed).
       *
       * @return true if the job is no longer running
       */
      public boolean isFinished() {
         return this != STARTED;
      }
   }

   /**
    * Get the status ID which identifies a particular job.
    *
    * @return the status ID
    */
   long getStatusId();

   /**
    * Get the end date, which is when the job completed.
    *
    * @return the end date, may be {@code null} if it hasn't finished or shutdown abnormally
    */
   Date getEndDate();

   /**
    * Get the end date as LocalDateTime for modern time handling.
    *
    * @return Optional containing the end date as LocalDateTime, or empty if not finished
    */
   default Optional<LocalDateTime> getEndDateAsLocalDateTime() {
      return Optional.ofNullable(getEndDate())
                     .map(date -> Instant.ofEpochMilli(date.getTime())
                                         .atZone(ZoneId.systemDefault())
                                         .toLocalDateTime());
   }

   /**
    * Get the start date, which is when the job started.
    *
    * @return the start date
    */
   Date getStartDate();

   /**
    * Get the start date as LocalDateTime for modern time handling.
    *
    * @return Optional containing the start date as LocalDateTime, or empty if not set
    */
   default Optional<LocalDateTime> getStartDateAsLocalDateTime() {
      return Optional.ofNullable(getStartDate())
                     .map(date -> Instant.ofEpochMilli(date.getTime())
                                         .atZone(ZoneId.systemDefault())
                                         .toLocalDateTime());
   }

   /**
    * Get the publishing server ID.
    *
    * @return the publishing server ID, may be {@code null}
    */
   Long getPubServerId();

   /**
    * Get the publishing server ID safely with Optional wrapper.
    *
    * @return Optional containing the server ID, or empty if not set
    */
   default Optional<Long> getPubServerIdSafely() {
      return Optional.ofNullable(getPubServerId());
   }

   /**
    * Get the associated edition ID.
    *
    * @return the edition ID
    */
   long getEditionId();

   /**
    * Get the ending status of the publishing job.
    *
    * @return the ending state, never {@code null}. If it is {@link EndingState#STARTED},
    *         then it may still be running or may have ended abnormally
    */
   EndingState getEndingState();

   /**
    * Get the number of delivered items.
    *
    * @return the count of items delivered
    */
   int getDeliveredCount();

   /**
    * Get the number of removed items.
    *
    * @return the count of items removed
    */
   int getRemovedCount();

   /**
    * Get the number of items that failed from either assembly or delivery.
    *
    * @return the count of items that failed
    */
   int getFailedCount();

   /**
    * Get the total number of items processed (delivered + removed + failed).
    *
    * @return the total item count
    */
   default int getTotalItemCount() {
      return getDeliveredCount() + getRemovedCount() + getFailedCount();
   }

   /**
    * Calculate the success rate as a percentage.
    *
    * @return success rate between 0.0 and 100.0, or 0.0 if no items processed
    */
   default double getSuccessRate() {
      var total = getTotalItemCount();
      if (total == 0) return 0.0;

      var successful = getDeliveredCount() + getRemovedCount();
      return (double) successful / total * 100.0;
   }

   /**
    * Check if this job processed any items.
    *
    * @return true if any items were processed
    */
   default boolean hasProcessedItems() {
      return getTotalItemCount() > 0;
   }

   /**
    * Check if this job had any failures.
    *
    * @return true if there were failed items
    */
   default boolean hasFailures() {
      return getFailedCount() > 0;
   }

   /**
    * Is the publishing job hidden from the list view?
    *
    * @return {@code true} if this job is hidden from the list view
    */
   boolean isHidden();

   /**
    * Gets the name and port of the server in the format of {@code <server-name>:<port>}.
    *
    * @return the server name and port, may be {@code null}
    */
   String getServer();

   /**
    * Get the server name and port safely with Optional wrapper.
    *
    * @return Optional containing the server info, or empty if not set
    */
   default Optional<String> getServerSafely() {
      return Optional.ofNullable(getServer())
                     .filter(server -> !server.trim().isEmpty());
   }

   /**
    * Calculate the duration of the publishing job.
    *
    * @return Optional containing the job duration, or empty if not finished or dates unavailable
    */
   default Optional<Duration> getJobDuration() {
      var startDate = getStartDateAsLocalDateTime();
      var endDate = getEndDateAsLocalDateTime();

      if (startDate.isPresent() && endDate.isPresent()) {
         return Optional.of(Duration.between(startDate.get(), endDate.get()));
      }
      return Optional.empty();
   }

   /**
    * Check if the job is currently running.
    *
    * @return true if the job is in progress
    */
   default boolean isRunning() {
      return getEndingState().isRunning();
   }

   /**
    * Check if the job completed successfully.
    *
    * @return true if the job completed without major issues
    */
   default boolean isSuccessful() {
      return getEndingState().isSuccessful();
   }

   /**
    * Check if the job failed or was terminated.
    *
    * @return true if the job failed
    */
   default boolean isFailed() {
      return getEndingState().isFailure();
   }

   /**
    * Check if the job was terminated externally (canceled or aborted).
    *
    * @return true if the job was canceled or aborted
    */
   default boolean wasTerminated() {
      return getEndingState().isTerminated();
   }

   /**
    * Check if the job is finished (either completed or failed).
    *
    * @return true if the job is no longer running
    */
   default boolean isFinished() {
      return getEndingState().isFinished();
   }

   /**
    * Get a summary string of this publishing job for logging purposes.
    *
    * @return summary string containing key job details
    */
   default String getSummary() {
      var state = getEndingState().getDescription();
      var duration = getJobDuration()
         .map(d -> String.format("%d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart()))
         .orElse("unknown");
      var successRate = String.format("%.1f%%", getSuccessRate());

      return String.format("Job[id=%d, edition=%d, %s, duration=%s, items=%d, success=%s]",
         getStatusId(), getEditionId(), state, duration, getTotalItemCount(), successRate);
   }

   /**
    * Compare this job status with another by start date for chronological ordering.
    *
    * @param other the other job status to compare to, never {@code null}
    * @return comparison result based on start date, nulls last
    * @throws IllegalArgumentException if other is null
    */
   default int compareByStartDate(IPSPubStatus other) {
      Objects.requireNonNull(other, "Other job status cannot be null");

      var thisDate = getStartDate();
      var otherDate = other.getStartDate();

      if (thisDate == null && otherDate == null) return 0;
      if (thisDate == null) return 1;
      if (otherDate == null) return -1;

      return thisDate.compareTo(otherDate);
   }

   /**
    * Compare this job status with another by status ID for ordering.
    *
    * @param other the other job status to compare to, never {@code null}
    * @return comparison result based on status ID
    * @throws IllegalArgumentException if other is null
    */
   default int compareByStatusId(IPSPubStatus other) {
      Objects.requireNonNull(other, "Other job status cannot be null");
      return Long.compare(getStatusId(), other.getStatusId());
   }
}

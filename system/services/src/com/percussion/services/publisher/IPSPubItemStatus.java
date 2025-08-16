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

import com.percussion.services.publisher.IPSSiteItem.Operation;
import com.percussion.services.publisher.IPSSiteItem.Status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Each published item is recorded in the database. This object allows that 
 * information to be retrieved. This interface provides modern Java 11 patterns
 * for published item status tracking with enhanced validation and safe access methods.
 *
 * @author dougrand
 */
public interface IPSPubItemStatus {

   /**
    * The reference ID, this is the unique ID per row.
    *
    * @return the reference ID
    */
   long getReferenceId();

   /**
    * Get the status ID (job ID of the run).
    *
    * @return the status/job ID
    */
   long getStatusId();

   /**
    * Get the content ID published.
    *
    * @return the content ID
    */
   int getContentId();

   /**
    * Get the revision ID of the item published.
    *
    * @return the revision ID
    */
   int getRevisionId();

   /**
    * Get the folder ID if present.
    *
    * @return the folder ID, may be {@code null}
    */
   Integer getFolderId();

   /**
    * Get the folder ID safely with Optional wrapper.
    *
    * @return Optional containing the folder ID, or empty if not set
    */
   default Optional<Integer> getFolderIdSafely() {
      return Optional.ofNullable(getFolderId());
   }

   /**
    * Get the template ID used to publish the item.
    *
    * @return the template ID, may be {@code null}
    */
   Long getTemplateId();

   /**
    * Get the template ID safely with Optional wrapper.
    *
    * @return Optional containing the template ID, or empty if not set
    */
   default Optional<Long> getTemplateIdSafely() {
      return Optional.ofNullable(getTemplateId());
   }

   /**
    * Get the published location. This location is relative to the publish root
    * of the published site.
    *
    * @return the path of the published item, may be {@code null} (e.g. for DB publish)
    */
   String getLocation();

   /**
    * Get the published location safely with Optional wrapper.
    *
    * @return Optional containing the location, or empty if not set
    */
   default Optional<String> getLocationSafely() {
      return Optional.ofNullable(getLocation())
                     .filter(loc -> !loc.trim().isEmpty());
   }

   /**
    * Get the date the item was assembled and delivered.
    *
    * @return the publish date
    */
   Date getDate();

   /**
    * Get the publish date as LocalDateTime for modern time handling.
    *
    * @return Optional containing the publish date as LocalDateTime, or empty if not set
    */
   default Optional<LocalDateTime> getDateAsLocalDateTime() {
      return Optional.ofNullable(getDate())
                     .map(date -> Instant.ofEpochMilli(date.getTime())
                                         .atZone(ZoneId.systemDefault())
                                         .toLocalDateTime());
   }

   /**
    * Get the operation type (publish/unpublish).
    *
    * @return the operation
    */
   Operation getOperation();

   /**
    * Check if this is a publish operation.
    *
    * @return true if this is a publish operation
    */
   default boolean isPublishOperation() {
      return getOperation() == Operation.PUBLISH;
   }

   /**
    * Check if this is an unpublish operation.
    *
    * @return true if this is an unpublish operation
    */
   default boolean isUnpublishOperation() {
      return getOperation() == Operation.UNPUBLISH;
   }

   /**
    * Gets the reference ID of the origin of the unpublishing operation.
    * 
    * @return the unpublish reference ID, may be {@code null} if it is a publishing operation
    */
   Long getUnpublishRefId();

   /**
    * Get the unpublish reference ID safely with Optional wrapper.
    *
    * @return Optional containing the unpublish reference ID, or empty if not set
    */
   default Optional<Long> getUnpublishRefIdSafely() {
      return Optional.ofNullable(getUnpublishRefId());
   }

   /**
    * The assembly URL. This is going to be a synthetic assembly URL as the
    * actual assembly is done "internally" via a service call.
    * 
    * @return the assembly URL, may be {@code null}
    */
   String getAssemblyUrl();

   /**
    * Get the assembly URL safely with Optional wrapper.
    *
    * @return Optional containing the assembly URL, or empty if not set
    */
   default Optional<String> getAssemblyUrlSafely() {
      return Optional.ofNullable(getAssemblyUrl())
                     .filter(url -> !url.trim().isEmpty());
   }

   /**
    * Get the elapsed time in milliseconds.
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
    * Get the status of the operation.
    *
    * @return the operation status
    */
   Status getStatus();

   /**
    * Check if the operation was successful.
    *
    * @return true if the status indicates success
    */
   default boolean isSuccessful() {
      return getStatus() == Status.SUCCESS;
   }

   /**
    * Check if the operation failed.
    *
    * @return true if the status indicates failure
    */
   default boolean isFailed() {
      return getStatus() == Status.FAILURE;
   }

   /**
    * Check if the operation is pending.
    *
    * @return true if the status indicates pending
    */
   default boolean isPending() {
      return getStatus() == Status.PENDING;
   }

   /**
    * Is the record hidden from the list view?
    *
    * @return {@code true} if this record is hidden from the list view
    */
   boolean isHidden();

   /**
    * Get the status message.
    *
    * @return the message, may be {@code null}
    */
   String getMessage();

   /**
    * Get the status message safely with Optional wrapper.
    *
    * @return Optional containing the message, or empty if not set
    */
   default Optional<String> getMessageSafely() {
      return Optional.ofNullable(getMessage())
                     .filter(msg -> !msg.trim().isEmpty());
   }

   /**
    * Get the delivery type used to deliver the content.
    *
    * @return the delivery type, may be {@code null}
    */
   String getDeliveryType();

   /**
    * Get the delivery type safely with Optional wrapper.
    *
    * @return Optional containing the delivery type, or empty if not set
    */
   default Optional<String> getDeliveryTypeSafely() {
      return Optional.ofNullable(getDeliveryType())
                     .filter(type -> !type.trim().isEmpty());
   }

   /**
    * Check if this item has timing information available.
    *
    * @return true if elapsed time is available
    */
   default boolean hasElapsedTime() {
      return getElapsedSafely().isPresent();
   }

   /**
    * Check if this item has a published location.
    *
    * @return true if location is available (not database publish)
    */
   default boolean hasLocation() {
      return getLocationSafely().isPresent();
   }

   /**
    * Check if this item has an associated unpublish operation.
    *
    * @return true if unpublish reference ID is present
    */
   default boolean hasUnpublishRef() {
      return getUnpublishRefIdSafely().isPresent();
   }

   /**
    * Get a summary string of this publish status for logging purposes.
    *
    * @return summary string containing key publishing details
    */
   default String getSummary() {
      var operation = getOperation().toString().toLowerCase();
      var status = getStatus().toString().toLowerCase();
      var elapsed = getElapsedSafely().map(e -> e + "ms").orElse("unknown");
      var location = getLocationSafely().orElse("database");

      return String.format("Item[ref=%d, content=%d:%d, %s %s in %s] -> %s",
         getReferenceId(), getContentId(), getRevisionId(),
         operation, status, elapsed, location);
   }

   /**
    * Compare this item status with another by reference ID for ordering.
    *
    * @param other the other item status to compare to, never {@code null}
    * @return comparison result based on reference ID
    * @throws IllegalArgumentException if other is null
    */
   default int compareByReferenceId(IPSPubItemStatus other) {
      Objects.requireNonNull(other, "Other item status cannot be null");
      return Long.compare(getReferenceId(), other.getReferenceId());
   }

   /**
    * Compare this item status with another by publish date for chronological ordering.
    *
    * @param other the other item status to compare to, never {@code null}
    * @return comparison result based on publish date, nulls last
    * @throws IllegalArgumentException if other is null
    */
   default int compareByDate(IPSPubItemStatus other) {
      Objects.requireNonNull(other, "Other item status cannot be null");

      var thisDate = getDate();
      var otherDate = other.getDate();

      if (thisDate == null && otherDate == null) return 0;
      if (thisDate == null) return 1;
      if (otherDate == null) return -1;

      return thisDate.compareTo(otherDate);
   }
}

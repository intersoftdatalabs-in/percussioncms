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

import com.percussion.services.publisher.data.PSPubItem;
import com.percussion.services.publisher.data.PSSiteItem;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A site item represents the state of a single published item to a site. Site
 * items as shown by this interface are actually composites of 
 * {@link PSSiteItem} and {@link PSPubItem}. These are joined on the 
 * reference id. However, updates to {@link PSSiteItem} must take into account
 * that new records from {@link PSPubItem} must be joined as updates occur.
 * This interface provides modern Java 11 patterns for site item management
 * with enhanced validation and safe access methods.
 *
 * @author dougrand
 */
public interface IPSSiteItem {

   /**
    * The operation that was performed on the item with utility methods.
    */
   enum Operation {
      /** The item was published */
      PUBLISH("Published"),
      /** The item was unpublished */
      UNPUBLISH("Unpublished");

      private final String description;

      Operation(String description) {
         this.description = description;
      }

      /**
       * Get a human-readable description of this operation.
       *
       * @return description string, never null
       */
      public String getDescription() {
         return description;
      }

      /**
       * Check if this is a publish operation.
       *
       * @return true if this is a publish operation
       */
      public boolean isPublish() {
         return this == PUBLISH;
      }

      /**
       * Check if this is an unpublish operation.
       *
       * @return true if this is an unpublish operation
       */
      public boolean isUnpublish() {
         return this == UNPUBLISH;
      }
   }

   /**
    * The status of the operation with enhanced utility methods.
    */
   enum Status {
      /** The operation succeeded */
      SUCCESS("Operation successful"),

      /** The operation failed */
      FAILURE("Operation failed"),

      /** The item has been published or unpublished due to job cancellation by user */
      CANCELLED("Operation cancelled by user"),

      /** The item is still being processed or orphaned due to server abort */
      UNDEFINED("Operation status undefined");

      private final String description;

      Status(String description) {
         this.description = description;
      }

      /**
       * Get a human-readable description of this status.
       *
       * @return description string, never null
       */
      public String getDescription() {
         return description;
      }

      /**
       * Check if this status indicates success.
       *
       * @return true if the operation was successful
       */
      public boolean isSuccessful() {
         return this == SUCCESS;
      }

      /**
       * Check if this status indicates failure.
       *
       * @return true if the operation failed
       */
      public boolean isFailure() {
         return this == FAILURE;
      }

      /**
       * Check if this status indicates cancellation.
       *
       * @return true if the operation was cancelled
       */
      public boolean isCancelled() {
         return this == CANCELLED;
      }

      /**
       * Check if this status is undefined (pending or orphaned).
       *
       * @return true if the status is undefined
       */
      public boolean isUndefined() {
         return this == UNDEFINED;
      }

      /**
       * Check if this status indicates a completed operation (success or failure).
       *
       * @return true if the operation is completed
       */
      public boolean isCompleted() {
         return Set.of(SUCCESS, FAILURE, CANCELLED).contains(this);
      }
   }
   
   /**
    * Get the site ID.
    *
    * @return the site ID
    */
   long getSiteId();

   /**
    * Gets the status (or publishing job) ID.
    *
    * @return the ID of the publishing run
    */
   long getStatusId();
   
   /**
    * Set a new site ID.
    *
    * @param siteid the site ID
    */
   void setSiteId(long siteid);
   
   /**
    * Get the published item's content ID.
    *
    * @return the content ID, may be {@code null}
    */
   Integer getContentId();

   /**
    * Get the content ID safely with Optional wrapper.
    *
    * @return Optional containing the content ID, or empty if not set
    */
   default Optional<Integer> getContentIdSafely() {
      return Optional.ofNullable(getContentId());
   }

   /**
    * Set the published item's content ID.
    *
    * @param contentid the content ID
    */
   void setContentId(Integer contentid);
   
   /**
    * Get the publishing context.
    *
    * @return the context, may be {@code null}
    */
   Integer getContext();

   /**
    * Get the context safely with Optional wrapper.
    *
    * @return Optional containing the context, or empty if not set
    */
   default Optional<Integer> getContextSafely() {
      return Optional.ofNullable(getContext());
   }

   /**
    * Set the publishing context.
    *
    * @param context the context
    */
   void setContext(Integer context);
   
   /**
    * Get the published template ID.
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
    * Set the published template ID.
    *
    * @param templateid the new template ID
    */
   void setTemplateId(Long templateid);
   
   /**
    * Get the Hibernate version value, only used for Hibernate and should be
    * generally ignored.
    *
    * @return the version ID of the site item, may be {@code null}
    */
   Integer getVersionId();

   /**
    * Get the version ID safely with Optional wrapper.
    *
    * @return Optional containing the version ID, or empty if not set
    */
   default Optional<Integer> getVersionIdSafely() {
      return Optional.ofNullable(getVersionId());
   }

   /**
    * Set the version ID.
    *
    * @param versionid the version ID
    */
   void setVersionId(Integer versionid);

   /**
    * The revision of the published content item.
    *
    * @return the revision, may be {@code null}
    */
   Integer getRevisionId();

   /**
    * Get the revision ID safely with Optional wrapper.
    *
    * @return Optional containing the revision ID, or empty if not set
    */
   default Optional<Integer> getRevisionIdSafely() {
      return Optional.ofNullable(getRevisionId());
   }

   /**
    * Set the revision.
    *
    * @param revisionid the revision ID
    */
   void setRevisionId(Integer revisionid);

   /**
    * Get the publication date.
    *
    * @return the publication date, may be {@code null}
    */
   Date getDate();

   /**
    * Get the publication date as LocalDateTime for modern time handling.
    *
    * @return Optional containing the publication date as LocalDateTime, or empty if not set
    */
   default Optional<LocalDateTime> getDateAsLocalDateTime() {
      return Optional.ofNullable(getDate())
                     .map(date -> Instant.ofEpochMilli(date.getTime())
                                         .atZone(ZoneId.systemDefault())
                                         .toLocalDateTime());
   }

   /**
    * Set the publication date.
    *
    * @param pubdate the publication date
    */
   void setDate(Date pubdate);

   /**
    * Get the operation (publish/unpublish).
    *
    * @return the operation
    */
   Operation getOperation();

   /**
    * Set the operation.
    *
    * @param puboperation the operation
    */
   void setOperation(Operation puboperation);

   /**
    * Get the status.
    *
    * @return the status
    */
   Status getStatus();

   /**
    * Set the status.
    *
    * @param pubstatus the status
    */
   void setStatus(Status pubstatus);

   /**
    * Get the location of the published item.
    *
    * @return the published item location, may be {@code null}
    */
   String getLocation();

   /**
    * Get the location safely with Optional wrapper.
    *
    * @return Optional containing the location, or empty if not set
    */
   default Optional<String> getLocationSafely() {
      return Optional.ofNullable(getLocation())
                     .filter(loc -> !loc.trim().isEmpty());
   }

   /**
    * Set the location of the published item.
    *
    * @param location the location
    */
   void setLocation(String location);

   /**
    * Get the folder ID.
    *
    * @return the folder ID, or {@code null} if no folder ID was set
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
    * Set the folder ID.
    *
    * @param id the folder ID, or {@code null} if none used
    */
   void setFolderId(Integer id);
   
   /**
    * Get the content URL.
    *
    * @return the content URL, may be {@code null}
    */
   String getContentUrl();

   /**
    * Get the content URL safely with Optional wrapper.
    *
    * @return Optional containing the content URL, or empty if not set
    */
   default Optional<String> getContentUrlSafely() {
      return Optional.ofNullable(getContentUrl())
                     .filter(url -> !url.trim().isEmpty());
   }

   /**
    * Set the content URL.
    *
    * @param contenturl the content URL
    */
   void setContentUrl(String contenturl);

   /**
    * Get the elapsed time for the assembly.
    *
    * @return the elapsed time in milliseconds, may be {@code null}
    */
   Integer getElapsedTime();

   /**
    * Get the elapsed time safely with Optional wrapper.
    *
    * @return Optional containing the elapsed time, or empty if not set
    */
   default Optional<Integer> getElapsedTimeSafely() {
      return Optional.ofNullable(getElapsedTime());
   }

   /**
    * Get the elapsed time as a Duration for modern time handling.
    *
    * @return Optional containing the elapsed time as Duration, or empty if not set
    */
   default Optional<Duration> getElapsedDuration() {
      return getElapsedTimeSafely().map(Duration::ofMillis);
   }

   /**
    * Set the elapsed time.
    *
    * @param elapsetime the elapsed time in milliseconds
    */
   void setElapsedTime(Integer elapsetime);

   /**
    * Set the elapsed time using Duration for modern time handling.
    *
    * @param duration the elapsed duration, may be {@code null}
    */
   default void setElapsedDuration(Duration duration) {
      setElapsedTime(duration != null ? (int) duration.toMillis() : null);
   }

   /**
    * Get the actual site item record.
    *
    * @return the site item record
    */
   PSSiteItem getSiteItem();
   
   /**
    * Get the delivery type.
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
    * Gets the page number of the item. It is 1-based number for paginated items,
    * but it is always 0 for non-paginated items.
    *
    * @return the page number
    */
   int getPage();

   /**
    * Check if this is a paginated item.
    *
    * @return true if the page number is greater than 0
    */
   default boolean isPaginated() {
      return getPage() > 0;
   }

   /**
    * Check if this is a publish operation.
    *
    * @return true if this is a publish operation
    */
   default boolean isPublishOperation() {
      return getOperation().isPublish();
   }

   /**
    * Check if this is an unpublish operation.
    *
    * @return true if this is an unpublish operation
    */
   default boolean isUnpublishOperation() {
      return getOperation().isUnpublish();
   }

   /**
    * Check if this operation was successful.
    *
    * @return true if the status indicates success
    */
   default boolean isSuccessful() {
      return getStatus().isSuccessful();
   }

   /**
    * Check if this operation failed.
    *
    * @return true if the status indicates failure
    */
   default boolean isFailed() {
      return getStatus().isFailure();
   }

   /**
    * Check if this operation was cancelled.
    *
    * @return true if the status indicates cancellation
    */
   default boolean isCancelled() {
      return getStatus().isCancelled();
   }

   /**
    * Check if this operation is completed (success, failure, or cancelled).
    *
    * @return true if the operation is completed
    */
   default boolean isCompleted() {
      return getStatus().isCompleted();
   }

   /**
    * Check if this site item has timing information.
    *
    * @return true if elapsed time is available
    */
   default boolean hasElapsedTime() {
      return getElapsedTimeSafely().isPresent();
   }

   /**
    * Check if this site item has a published location.
    *
    * @return true if location is available
    */
   default boolean hasLocation() {
      return getLocationSafely().isPresent();
   }

   /**
    * Get a summary string of this site item for logging purposes.
    *
    * @return summary string containing key site item details
    */
   default String getSummary() {
      var operation = getOperation().getDescription().toLowerCase();
      var status = getStatus().getDescription().toLowerCase();
      var elapsed = getElapsedTimeSafely().map(e -> e + "ms").orElse("unknown");
      var location = getLocationSafely().orElse("database");
      var contentId = getContentIdSafely().map(String::valueOf).orElse("unknown");

      return String.format("SiteItem[site=%d, content=%s, %s %s in %s] -> %s",
         getSiteId(), contentId, operation, status, elapsed, location);
   }

   /**
    * Compare this site item with another by site ID and content ID for ordering.
    *
    * @param other the other site item to compare to, never {@code null}
    * @return comparison result based on site ID, then content ID
    * @throws IllegalArgumentException if other is null
    */
   default int compareBySiteAndContent(IPSSiteItem other) {
      Objects.requireNonNull(other, "Other site item cannot be null");

      var siteComparison = Long.compare(getSiteId(), other.getSiteId());
      if (siteComparison != 0) {
         return siteComparison;
      }

      var thisContentId = getContentIdSafely().orElse(0);
      var otherContentId = other.getContentIdSafely().orElse(0);
      return Integer.compare(thisContentId, otherContentId);
   }
}

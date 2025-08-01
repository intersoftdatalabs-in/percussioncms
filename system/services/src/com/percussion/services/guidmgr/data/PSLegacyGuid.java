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
package com.percussion.services.guidmgr.data;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.utils.xml.IPSXmlSerialization;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Legacy GUID implementation for backward compatibility with legacy content store.
 *
 * <p>This class creates pseudo-GUIDs that are unique within a single database but
 * not globally unique. It maintains backward compatibility while providing modern
 * Java 11 features and improved thread safety.
 *
 * <p><strong>Important Limitations:</strong>
 * <ul>
 *   <li>These GUIDs cannot be used across JVM invocations</li>
 *   <li>Child GUIDs may resolve to different objects after restart</li>
 *   <li>GUIDs become invalid after content type changes</li>
 *   <li>Should not be stored persistently</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 * @deprecated This class will be deprecated in future versions as part of legacy content migration
 */
@Deprecated(since = "Java 11 Migration")
public final class PSLegacyGuid extends PSGuid {

   private static final long serialVersionUID = -3200949933035613891L;

   /**
    * Undefined revision constant using bit mask for compatibility.
    */
   public static final int UNDEFINED_REVISION = (int) BIT24;

   /**
    * Thread-safe counter for child ID allocation.
    * Initial value must be > 0 for webservice compatibility.
    */
   private static final AtomicInteger CHILD_ID_COUNTER = new AtomicInteger(1);

   /**
    * Thread-safe mapping of content type + child ID tuples to allocated IDs.
    */
   private static final Map<List<Long>, Long> CHILD_IDS_ALLOCATED =
       new ConcurrentHashMap<>();

   /**
    * Thread-safe reverse mapping from allocated ID to original key.
    */
   private static final Map<Long, List<Long>> KEYS_FROM_IDS =
       new ConcurrentHashMap<>();

   /**
    * Creates a legacy GUID for a content item.
    *
    * @param contentId the content ID of the item
    * @param revision the revision of the item, or -1 if undefined
    */
   public PSLegacyGuid(int contentId, int revision) {
      var actualRevision = revision == -1 ? UNDEFINED_REVISION : revision;
      assemble(actualRevision, PSTypeEnum.LEGACY_CONTENT, contentId);
   }
   
   /**
    * Creates a legacy GUID for a content child.
    *
    * @param contentTypeId reference to the content type ID from the content editor
    * @param childId reference to the mapper ID from the content editor for the child
    * @param sysId the primary key for the child object
    */
   public PSLegacyGuid(long contentTypeId, int childId, int sysId) {
      var virtualSite = mapChildType(contentTypeId, childId);
      assemble(virtualSite, PSTypeEnum.LEGACY_CHILD, sysId);
   }

   /**
    * Creates a legacy GUID from a regular GUID with validation.
    *
    * @param guid the source GUID, must not be null and must be a legacy type
    * @throws IllegalArgumentException if guid is null or not a legacy type
    */
   public PSLegacyGuid(PSGuid guid) {
      Objects.requireNonNull(guid, "guid cannot be null");

      var guidType = guid.getType();
      if (guidType != PSTypeEnum.LEGACY_CONTENT.getOrdinal() &&
          guidType != PSTypeEnum.LEGACY_CHILD.getOrdinal()) {
         throw new IllegalArgumentException(
             "GUID type must be either LEGACY_CONTENT or LEGACY_CHILD, but was: " + guidType);
      }

      var type = guidType == PSTypeEnum.LEGACY_CONTENT.getOrdinal()
          ? PSTypeEnum.LEGACY_CONTENT
          : PSTypeEnum.LEGACY_CHILD;
      assemble(guid.getHostId(), type, guid.getUUID());
   }
   
   /**
    * Reconstitutes a legacy GUID from a long value with defaults.
    *
    * @param value the GUID value; if no type specified, LEGACY_CONTENT is used
    * @throws IllegalArgumentException if an invalid legacy type is specified
    */
   public PSLegacyGuid(long value) {
      m_guid = value;

      if (getType() == 0) {
         setType(PSTypeEnum.LEGACY_CONTENT.getOrdinal());
      } else {
         validateLegacyType();
      }

      if (getHostId() == 0) {
         setHostId(UNDEFINED_REVISION);
      }
   }
   
   /**
    * Constructs a legacy GUID from its string representation.
    * 
    * @param raw the string representation of a legacy GUID, must not be blank
    * @throws IllegalArgumentException if raw is null or blank
    */
   public PSLegacyGuid(String raw) {
      super(Objects.requireNonNull(raw, "raw GUID string cannot be null"));
   }
   
   /**
    * Constructs a legacy GUID from a PSLocator.
    *
    * @param locator the source locator, must not be null
    */
   public PSLegacyGuid(PSLocator locator) {
      this(Objects.requireNonNull(locator, "locator cannot be null").getId(),
           locator.getRevision());
   }
   
   /**
    * Safely creates a legacy GUID from a long value.
    *
    * @param value the GUID value to validate
    * @return an Optional containing the PSLegacyGuid, or empty if invalid
    */
   public static Optional<PSLegacyGuid> ofNullable(long value) {
      try {
         return Optional.of(new PSLegacyGuid(value));
      } catch (IllegalArgumentException e) {
         return Optional.empty();
      }
   }
   
   /**
    * Safely creates a legacy GUID from a PSGuid.
    *
    * @param guid the source GUID
    * @return an Optional containing the PSLegacyGuid, or empty if conversion fails
    */
   public static Optional<PSLegacyGuid> ofNullable(PSGuid guid) {
      try {
         return guid != null ? Optional.of(new PSLegacyGuid(guid)) : Optional.empty();
      } catch (IllegalArgumentException e) {
         return Optional.empty();
      }
   }
   
   /**
    * Determines if this GUID represents a child.
    *
    * @return true if this is a child GUID
    */
   public boolean isChildGuid() {
      return getType() == PSTypeEnum.LEGACY_CHILD.getOrdinal();
   }
   
   /**
    * Gets the original content type ID value for child GUIDs.
    *
    * @return the original content type ID
    * @throws IllegalStateException if this is not a child GUID or mapping is lost
    */
   @IPSXmlSerialization(suppress = true)
   public long getContentTypeId() {
      return getKeyFromId(getHostId())
          .map(keys -> keys.get(0))
          .orElseThrow(() -> new IllegalStateException(
              "Cannot retrieve content type from unknown child ID: " + getHostId()));
   }
   
   /**
    * Gets the original child mapper ID value for child GUIDs.
    *
    * @return the original child mapper ID
    * @throws IllegalStateException if this is not a child GUID or mapping is lost
    */
   @IPSXmlSerialization(suppress = true)
   public int getChildId() {
      return getKeyFromId(getHostId())
          .map(keys -> keys.get(1).intValue())
          .orElseThrow(() -> new IllegalStateException(
              "Cannot retrieve child mapper ID from unknown child ID: " + getHostId()));
   }

   /**
    * Gets the content ID.
    *
    * @return the original content ID
    */
   @IPSXmlSerialization(suppress = true)
   public int getContentId() {
      return getUUID();
   }
   
   /**
    * Gets the revision ID.
    *
    * @return the original revision ID, or -1 if undefined
    */
   @IPSXmlSerialization(suppress = true)
   public int getRevision() {
      var hostId = (int) getHostId();
      return hostId == UNDEFINED_REVISION ? -1 : hostId;
   }

   /**
    * Sets the revision ID.
    *
    * @param revisionId the revision ID to set
    */
   @IPSXmlSerialization(suppress = true)
   public void setRevision(int revisionId) {
      setHostId(revisionId);
   }

   /**
    * Creates a PSLocator from this legacy GUID.
    *
    * @return a valid locator
    * @throws IllegalStateException if this is a child GUID
    */
   public PSLocator getLocator() {
      if (isChildGuid()) {
         throw new IllegalStateException("Cannot create locator from child GUID");
      }
      return new PSLocator(getContentId(), getRevision());
   }

   /**
    * Thread-safe mapping of content type and child ID to allocated ID.
    */
   private long mapChildType(long contentTypeId, int childId) {
      var key = List.of(contentTypeId, (long) childId);
      return CHILD_IDS_ALLOCATED.computeIfAbsent(key, k -> {
         var allocatedId = (long) CHILD_ID_COUNTER.getAndIncrement();
         KEYS_FROM_IDS.put(allocatedId, k);
         return allocatedId;
      });
   }

   /**
    * Retrieves the original content type and child ID for an allocated ID.
    */
   private Optional<List<Long>> getKeyFromId(long id) {
      return Optional.ofNullable(KEYS_FROM_IDS.get(id));
   }

   /**
    * Validates that the GUID type is a supported legacy type.
    */
   private void validateLegacyType() {
      var type = getType();
      if (type != PSTypeEnum.LEGACY_CONTENT.getOrdinal() &&
          type != PSTypeEnum.LEGACY_CHILD.getOrdinal()) {
         throw new IllegalArgumentException(
             "Only LEGACY_CHILD and LEGACY_CONTENT types are supported, but was: " + type);
      }
   }
}

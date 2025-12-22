// REFACTORED: CP-JAVA11
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

package com.percussion.services.audit.data;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * Class representing an audit entry, used to save information regarding the
 * modification of a design object.
 *
 * <p>This entity tracks audit events for design objects with modern Java 11 features
 * including LocalDateTime for date handling and Stream API for enum operations.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
@Entity
@Table(name = "PSX_DESIGN_AUDIT_LOG")
public class PSAuditLogEntry implements Serializable {
   /**
    * Unique identifier for this entry
    */
   @Id
   @Column(name = "AUDIT_ID", nullable = false)   
   private long id;
   
   /**
    * The date of this audit entry using modern LocalDateTime
    */
   @Column(name = "AUDIT_DATE", nullable = false)
   private Date auditDate; // Keep Date for JPA compatibility but provide LocalDateTime accessors

   /**
    * The UUID of the object for which the audit event occurred.
    */
   @SuppressWarnings("unused")
   @Column(name = "OBJECT_ID", nullable = false)
   private long objectId;
   
   /**
    * The short value of the type of object for which the audit event occurred. 
    */
   @Column(name = "OBJECT_TYPE", nullable = false)
   private int objectType;
   
   /**
    * The long value of the GUID of the object for which the audit event 
    * occurred.
    */
   @Column(name = "OBJECT_GUID", nullable = false)
   private long objectGuid;
   
   /**
    * The name of the user that generated this entry
    */
   @Column(name = "USERNAME", nullable = false)
   private String userName;
   
   /**
    * The type of action, one of the <code>AuditTypes</code> enum values.
    */
   @Column(name = "ACTION", nullable = false)
   private String action;
   
   /**
    * Get the date when this event was generated.
    * 
    * @return The date, may be <code>null</code>.
    * @deprecated Use {@link #getLocalDateTime()} for modern date handling
    */
   @Deprecated
   public Date getDate() {
      return auditDate;
   }

   /**
    * Get the date when this event was generated as LocalDateTime.
    *
    * @return The LocalDateTime, wrapped in Optional if present
    */
   public Optional<LocalDateTime> getLocalDateTime() {
      return Optional.ofNullable(auditDate)
         .map(date -> date.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
   }

   /**
    * Get the GUID uniquely identifying this event.
    * 
    * @return The GUID, may be <code>null</code>.
    */
   public IPSGuid getGUID() {
      return new PSGuid(PSTypeEnum.INTERNAL, id);
   }

   /**
    * Get the GUID of the audited object.
    * 
    * @return The GUID, may be <code>null</code>.
    */
   public IPSGuid getObjectGUID() {
      return new PSGuid(PSTypeEnum.valueOf(objectType), objectGuid);
   }

   /**
    * Get the name of the user that caused this event.
    * 
    * @return The name, may be <code>null</code> or empty.
    */
   public String getUserName() {
      return userName;
   }

   /**
    * Set the audit date.  See {@link #getDate()}.
    * 
    * @param date The date of the event, may not be <code>null</code>.
    * @deprecated Use {@link #setLocalDateTime(LocalDateTime)} for modern date handling
    */
   @Deprecated
   public void setDate(Date date) {
      this.auditDate = Objects.requireNonNull(date, "auditDate may not be null");
   }

   /**
    * Set the audit date using LocalDateTime.
    *
    * @param dateTime The LocalDateTime of the event, may not be <code>null</code>.
    */
   public void setLocalDateTime(LocalDateTime dateTime) {
      Objects.requireNonNull(dateTime, "dateTime may not be null");
      this.auditDate = Date.from(dateTime.atZone(ZoneOffset.UTC).toInstant());
   }

   /**
    * Set the event GUID.  See {@link #getGUID()}.
    * 
    * @param guid The GUID, may not be <code>null</code>.
    */
   public void setGUID(IPSGuid guid) {
      Objects.requireNonNull(guid, "guid may not be null");
      this.id = guid.longValue();
   }

   /**
    * Set the audited object GUID.  See {@link #getObjectGUID()}.
    * 
    * @param guid The GUID, may not be <code>null</code>.
    */
   public void setObjectGUID(IPSGuid guid) {
      Objects.requireNonNull(guid, "guid may not be null");
      this.objectGuid = guid.longValue();
      this.objectId = guid.getUUID();
      this.objectType = guid.getType();
   }

   /**
    * Set the name of the user causing the event.  See {@link #getUserName()}.
    * 
    * @param name The name of the user, not <code>null</code> or empty.
    */
   public void setUserName(String name) {
      if (StringUtils.isBlank(name)) {
         throw new IllegalArgumentException("name may not be null or empty");
      }
      this.userName = name;
   }

   /**
    * Get the action type.
    *
    * @return Returns the action type, never null.
    */
   public AuditTypes getAction() {
      return AuditTypes.valueFromString(action);
   }

   /**
    * Set the audit action type.
    *
    * @param auditAction The action to set, may not be null.
    */
   public void setAction(AuditTypes auditAction) {
      this.action = Objects.requireNonNull(auditAction, "auditAction may not be null")
         .getStringValue();
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof PSAuditLogEntry)) {
         return false;
      }
      if (this == obj) {
         return true;
      }

      var other = (PSAuditLogEntry) obj;

      // Need special handling of date field as Hibernate creates a Timestamp
      // instance with a milliseconds value
      var thisTime = getTruncatedTime(auditDate);
      var otherTime = getTruncatedTime(other.auditDate);

      return new EqualsBuilder()
         .append(id, other.id)
         .append(objectGuid, other.objectGuid)
         .append(action, other.action)
         .append(userName, other.userName)
         .append(thisTime, otherTime)
         .isEquals();
   }

   @Override
   public int hashCode() {
      return getGUID().getUUID();
   }

   /**
    * Get the value of the supplied date as a long, first truncating any 
    * millisecond component using modern time API.
    *
    * @param date The date to truncate, may be <code>null</code>.
    * 
    * @return The millisecond value of the truncated date, or <code>0</code> if
    * the date is <code>null</code>.
    */
   private long getTruncatedTime(Date date) {
      if (date == null) {
         return 0;
      }

      return date.toInstant()
         .atZone(ZoneOffset.UTC)
         .toLocalDateTime()
         .withNano(0) // Remove nanosecond component
         .atZone(ZoneOffset.UTC)
         .toInstant()
         .toEpochMilli();
   }

   @Override
   public String toString() {
      return String.format("%d - %s - %s - %d",
         id, getUserName(), getObjectGUID(), getTruncatedTime(auditDate));
   }

   /**
    * Represents the type of audit event using modern enum patterns.
    */
   public enum AuditTypes {
      /**
       * Represents a save event.
       */
      SAVE("save"),
      
      /**
       * Represents a delete event.
       */
      DELETE("delete");
      
      /**
       * The value of the audit type
       */
      private final String value;

      /**
       * Constructor for the audit type.
       *
       * @param value The internal value of the type.
       */
      AuditTypes(String value) {
         this.value = Objects.requireNonNull(value, "value may not be null");
      }
      
      /**
       * Get the type's internal value.
       *
       * @return The value, never null.
       */
      public String getStringValue() {
         return value;
      }
      
      /**
       * Obtain an instance of this enum based on the internal string value using Stream API.
       *
       * @param type The internal string value as obtained by 
       * {@link #getStringValue()}, not <code>null</code>, must be valid.
       * 
       * @return The instance, never <code>null</code>.
       * @throws IllegalArgumentException if the type is invalid
       */
      public static AuditTypes valueFromString(String type) {
         Objects.requireNonNull(type, "type may not be null");

         return Arrays.stream(values())
            .filter(auditType -> auditType.value.equals(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + type));
      }
   }
}

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
package com.percussion.services.catalog.data;

import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.locking.data.PSObjectLock;
import com.percussion.services.locking.data.PSObjectLockSummary;
import com.percussion.services.security.PSPermissions;
import com.percussion.services.security.data.PSUserAccessLevel;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Container which holds common information available for all design objects with enhanced Java 11 support.
 *
 * <p>This class provides a comprehensive summary view of design objects including identification,
 * descriptive information, permissions, and locking status. It serves as a lightweight representation
 * suitable for object browsing, selection, and administrative operations.
 *
 * <p>Key features:
 * <ul>
 *   <li>Comprehensive object identification with GUID and type information</li>
 *   <li>Enhanced null safety with Optional wrappers</li>
 *   <li>Permission and locking status management</li>
 *   <li>Factory methods for convenient object creation</li>
 *   <li>Immutable design patterns where appropriate</li>
 * </ul>
 *
 * <p>Common use cases:
 * <ul>
 *   <li>Object browsing in administrative interfaces</li>
 *   <li>Search result summaries</li>
 *   <li>Permission and locking status displays</li>
 *   <li>Deployment and migration object identification</li>
 * </ul>
 *
 * @since Java 11 Modernization
 */
public class PSObjectSummary implements IPSCatalogSummary {

   static {
      PSXmlSerializationHelper.addType(PSObjectSummary.class);
      PSXmlSerializationHelper.addType("locked", PSObjectLockSummary.class);
   }
   
   /**
    * The object's ID, never {@code null}.
    */
   private long id;
   
   /**
    * The object's type, never {@code null}.
    */
   private PSTypeEnum type;
   
   /**
    * The object's name, never {@code null} or empty.
    */
   private String name;
   
   /**
    * The object's display label, defaults to the name if not supplied, never
    * {@code null} or empty.
    */
   private String label;
   
   /**
    * The object's description, may be {@code null} or empty.
    */
   private String description;
   
   /**
    * Flag to act as a latch indicating if permissions have been explicitly
    * set on this object, initially {@code false}, set to {@code true}
    * by the first call to {@link #setPermissions(PSUserAccessLevel)}.  This
    * value does not persist across serializations of this object.
    */
   private transient boolean m_arePermissionsValid = false;
   
   /**
    * The permissions of the requestor to the object which this summary
    * represents, never {@code null}.
    */
   private PSUserAccessLevel permissions = new PSUserAccessLevel(null);
   
   /**
    * Holds the lock information if this object is locked, {@code null}
    * otherwise.
    */
   private PSObjectLockSummary locked = null;

   /**
    * Default constructor required for serialization frameworks.
    */
   public PSObjectSummary() {
      // Required for serialization
   }
   
   /**
    * Convenience constructor for basic object summary creation.
    *
    * @param id the GUID of the object, not {@code null}
    * @param name the name of the object, not {@code null} or empty
    * @throws IllegalArgumentException if parameters are invalid
    */
   public PSObjectSummary(IPSGuid id, String name) {
      this(id, name, null, null);
   }

   /**
    * Convenience constructor with description.
    *
    * @param id the GUID of the object, not {@code null}
    * @param name the name of the object, not {@code null} or empty
    * @param description the description of the object, may be {@code null} or empty
    * @throws IllegalArgumentException if parameters are invalid
    */
   public PSObjectSummary(IPSGuid id, String name, String description) {
      this(id, name, null, description);
   }
   
   /**
    * Construct a new summary with all parameters using enhanced validation.
    *
    * @param id the GUID of the object, not {@code null}
    * @param name the name of the object, not {@code null} or empty
    * @param label the display label, defaults to name if blank
    * @param description the description, may be {@code null} or empty
    * @throws IllegalArgumentException if id is null or name is blank
    */
   public PSObjectSummary(IPSGuid id, String name, String label, String description) {
      Objects.requireNonNull(id, "id cannot be null");
      if (StringUtils.isBlank(name)) {
         throw new IllegalArgumentException("name cannot be null or empty");
      }

      this.id = id.longValue();
      this.type = PSTypeEnum.valueOf(id.getType());
      this.name = name;
      this.label = StringUtils.isBlank(label) ? name : label;
      this.description = description;
   }

   /**
    * Create an object summary using factory method with enhanced validation.
    *
    * @param id the GUID of the object, not {@code null}
    * @param name the name of the object, not {@code null} or empty
    * @return a new PSObjectSummary instance
    * @throws IllegalArgumentException if parameters are invalid
    */
   public static PSObjectSummary of(IPSGuid id, String name) {
      return new PSObjectSummary(id, name);
   }

   /**
    * Create an object summary with description using factory method.
    *
    * @param id the GUID of the object, not {@code null}
    * @param name the name of the object, not {@code null} or empty
    * @param description the description, may be {@code null} or empty
    * @return a new PSObjectSummary instance
    * @throws IllegalArgumentException if parameters are invalid
    */
   public static PSObjectSummary of(IPSGuid id, String name, String description) {
      return new PSObjectSummary(id, name, description);
   }

   /**
    * Create a complete object summary using factory method.
    *
    * @param id the GUID of the object, not {@code null}
    * @param name the name of the object, not {@code null} or empty
    * @param label the display label, defaults to name if blank
    * @param description the description, may be {@code null} or empty
    * @return a new PSObjectSummary instance
    * @throws IllegalArgumentException if parameters are invalid
    */
   public static PSObjectSummary of(IPSGuid id, String name, String label, String description) {
      return new PSObjectSummary(id, name, label, description);
   }

   /**
    * Construct a new summary from an existing catalog summary for convenience.
    *
    * @param s the source summary, not {@code null}
    */
   public PSObjectSummary(IPSCatalogSummary s) {
      Objects.requireNonNull(s, "summary cannot be null");
      IPSGuid g = s.getGUID();
      if (g != null) {
         this.id = g.longValue();
      }
      this.type = s.getTypeEnum();

      this.name = s.getName();
      this.label = StringUtils.isBlank(s.getLabel()) ? s.getName() : s.getLabel();
      this.description = s.getDescription();
   }

   @Override
   public IPSGuid getGUID() {
      return new PSGuid(type, id);
   }

   @Override
   public String getType() {
      return type == null ? null : type.name();
   }

   public PSTypeEnum getTypeEnum() {
      return type;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getLabel() {
      return label != null ? label : name;
   }

   @Override
   public String getDescription() {
      return description;
   }

   /**
    * Set the object's description.
    * @param desc the description, may be {@code null} or empty
    */
   public void setDescription(String desc) {
      this.description = desc;
   }

   /**
    * Set the object's name. Used by callers that construct summaries and then
    * need to adjust the name.
    * @param name the name to set, not {@code null} or empty
    */
   public void setName(String name) {
      if (StringUtils.isBlank(name)) {
         throw new IllegalArgumentException("name cannot be null or empty");
      }
      this.name = name;
   }

   /**
    * Get the description with Optional wrapper for safer access.
    *
    * @return Optional containing the description if present and non-empty, empty otherwise
    */
   public Optional<String> getDescriptionOptional() {
      return Optional.ofNullable(description)
         .filter(desc -> !desc.trim().isEmpty());
   }

   /**
    * Get the permissions with Optional wrapper for safer access.
    *
    * @return Optional containing permissions if valid, empty otherwise
    */
   public Optional<PSUserAccessLevel> getPermissionsOptional() {
      return m_arePermissionsValid ? Optional.ofNullable(permissions) : Optional.empty();
   }

   /**
    * Get the object permissions.
    *
    * @return the permissions, never {@code null}
    */
   public PSUserAccessLevel getPermissions() {
      return permissions;
   }

   /**
    * Set the object permissions with validation.
    *
    * @param permissions the permissions to set, not {@code null}
    * @throws IllegalArgumentException if permissions is null
    */
   public void setPermissions(PSUserAccessLevel permissions) {
      Objects.requireNonNull(permissions, "permissions cannot be null");
      this.permissions = permissions;
      this.m_arePermissionsValid = true;
   }

   /**
    * Check if permissions have been explicitly set on this object.
    *
    * @return true if permissions are valid and have been set
    */
   public boolean arePermissionsValid() {
      return m_arePermissionsValid;
   }

   /**
    * Get the lock information with Optional wrapper for safer access.
    *
    * @return Optional containing lock information if object is locked, empty otherwise
    */
   public Optional<PSObjectLockSummary> getLockedOptional() {
      return Optional.ofNullable(locked);
   }

   /**
    * Get the lock information.
    *
    * @return the lock information if object is locked, {@code null} otherwise
    */
   public PSObjectLockSummary getLocked() {
      return locked;
   }

   /**
    * Set the lock information.
    *
    * @param locked the lock information, may be {@code null}
    */
   public void setLocked(PSObjectLockSummary locked) {
      this.locked = locked;
   }

   /**
    * Check if this object is currently locked.
    *
    * @return true if the object has lock information
    */
   public boolean isLocked() {
      return locked != null;
   }

   /**
    * Check if this object is locked by the specified user.
    *
    * @param userName the user name to check, may be {@code null}
    * @return true if the object is locked by the specified user
    */
   public boolean isLockedBy(String userName) {
      return getLockedOptional()
         .map(lockSummary -> Objects.equals(lockSummary.getLocker(), userName))
         .orElse(false);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSObjectSummary)) return false;

      var other = (PSObjectSummary) obj;
      return Objects.equals(id, other.id) &&
             Objects.equals(type, other.type) &&
             Objects.equals(name, other.name);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, type, name);
   }

   @Override
   public String toString() {
      return new ToStringBuilder(this)
         .append("id", id)
         .append("type", type)
         .append("name", name)
         .append("label", label)
         .append("description", description)
         .append("locked", isLocked())
         .append("permissionsValid", m_arePermissionsValid)
         .toString();
   }

   /**
    * The the lock info from the supplied lock.
    * 
    * @param lock the lock from which to set the lock info, not 
    *    <code>null</code>.
    */
   public void setLockedInfo(PSObjectLock lock) {
      if (lock == null)
         throw new IllegalArgumentException("lock cannot be null");
      
      setLockedInfo(lock.getLockSession(), lock.getLocker(), 
         lock.getRemainingTime());
   }
   
   /**
    * Set the new lock information.
    * 
    * @param session the session that has the object locked, not 
    *    <code>null</code> or empty.
    * @param locker the user who has the object locked, not <code>null</code>
    *    or empty.
    * @param remainingTime the remaining time of the lock, must be > 0.
    */
   public void setLockedInfo(String session, String locker, long remainingTime) {
      locked = new PSObjectLockSummary(session, locker, remainingTime);
   }
}


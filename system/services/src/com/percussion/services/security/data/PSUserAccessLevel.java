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
package com.percussion.services.security.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.percussion.services.security.PSPermissions;
import com.percussion.utils.xml.IPSXmlSerialization;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides the sum of a user's permissions for a given resource.
 *
 * <p>Jackson bean surface (issue #1903 / epic #505): no-arg constructor plus {@link
 * #setPermissions(Collection)} so empty nested graphs deserialize when this type appears under XML.
 * Derived {@code has*Access} predicates are suppressed from serialization.
 */
public class PSUserAccessLevel {
  /**
   * No-arg constructor for Jackson / XML frameworks. Permissions start empty until {@link
   * #setPermissions(Collection)} or the collection constructor is used.
   */
  public PSUserAccessLevel() {
    m_permissions = new HashSet<>();
  }

  /**
   * Construct with the given permission collection.
   *
   * @param permissions The collection of all permissions the user has for a given resource, may be
   *     {@code null} or empty if the user has no permissions.
   */
  public PSUserAccessLevel(Collection<PSPermissions> permissions) {
    m_permissions = new HashSet<>();
    if (permissions != null) {
      m_permissions.addAll(permissions);
    }
  }

  /**
   * @return {@code true} if this access level has read permission, false otherwise.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public boolean hasReadAccess() {
    return m_permissions.contains(PSPermissions.READ);
  }

  /**
   * @return {@code true} if this access level has update permission, false otherwise.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public boolean hasUpdateAccess() {
    return m_permissions.contains(PSPermissions.UPDATE);
  }

  /**
   * @return {@code true} if this access level has delete permission, false otherwise.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public boolean hasDeleteAccess() {
    return m_permissions.contains(PSPermissions.DELETE);
  }

  /**
   * @return {@code true} if this access level has runtime permission, false otherwise.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public boolean hasRuntimeAccess() {
    return m_permissions.contains(PSPermissions.RUNTIME_VISIBLE);
  }

  /**
   * @return {@code true} if this access level has owner permission, false otherwise.
   */
  @IPSXmlSerialization(suppress = true)
  @JsonIgnore
  public boolean hasOwnerAccess() {
    return m_permissions.contains(PSPermissions.OWNER);
  }

  /**
   * Get the set of permissions represented by this object.
   *
   * @return The set, never {@code null}. Modifications to this set will affect this object.
   */
  @JsonProperty
  public Set<PSPermissions> getPermissions() {
    return m_permissions;
  }

  /**
   * Replace the permission set (Jackson / bean restore).
   *
   * @param permissions may be {@code null} or empty
   */
  public void setPermissions(Collection<PSPermissions> permissions) {
    m_permissions = new HashSet<>();
    if (permissions != null) {
      m_permissions.addAll(permissions);
    }
  }

  @Override
  public String toString() {
    return "PSUserAccessLevel{m_permissions=" + m_permissions + '}';
  }

  /**
   * Set of all permissions, never {@code null} after construction, may be empty.
   */
  private Set<PSPermissions> m_permissions;

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSUserAccessLevel)) return false;
      PSUserAccessLevel that = (PSUserAccessLevel) o;
      if(m_permissions.size()!= that.m_permissions.size()) return false;

      return m_permissions.containsAll(that.m_permissions);
   }

   @Override
   public int hashCode() {
      return m_permissions.hashCode();
   }

}

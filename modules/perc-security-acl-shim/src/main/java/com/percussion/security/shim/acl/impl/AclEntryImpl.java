/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.security.shim.acl.impl;

import com.percussion.security.shim.acl.AclEntry;
import com.percussion.security.shim.acl.Permission;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

/**
 * In-memory implementation of {@link AclEntry} with optional negative-permissions flag.
 *
 * <p>Mirrors {@code java.security.acl.AclEntry} behavior closely for compatibility. Entries track
 * their owning {@link Principal}, an optional negative-permissions flag, and an ordered set of
 * {@link Permission} instances. The class is not thread-safe; external synchronization is required
 * when sharing an instance across threads.
 *
 * @author Percussion Software
 */
public class AclEntryImpl implements AclEntry {

  private static final long serialVersionUID = 1L;

  private Principal principal;
  private final Set<Permission> permissions = new LinkedHashSet<>();
  private boolean negative;

  /** Creates a new, empty entry with no principal and no permissions. */
  public AclEntryImpl() {
    // no-op
  }

  /**
   * Sets the principal that owns this entry.
   *
   * @param user the principal to associate with this entry, never {@code null}
   * @return {@code true} if the principal was accepted; {@code false} if a different principal has
   *     already been set on this entry or if {@code user} is {@code null}
   */
  @Override
  public boolean setPrincipal(Principal user) {
    if (user == null) {
      // Clearing principal is not typical in java.security.acl.AclEntry, reject nulls explicitly.
      return false;
    }
    if (this.principal != null && !Objects.equals(this.principal, user)) {
      return false;
    }
    this.principal = user;
    return true;
  }

  /**
   * Returns the principal associated with this entry.
   *
   * @return the principal for this entry, or {@code null} if no principal has been set
   */
  @Override
  public Principal getPrincipal() {
    return principal;
  }

  /**
   * Marks this entry as a negative entry so its permissions are treated as denials. The mark is
   * permanent: once set, the entry cannot be converted back to a positive entry.
   */
  @Override
  public void setNegativePermissions() {
    this.negative = true;
  }

  /**
   * Reports whether this entry is a negative entry.
   *
   * @return {@code true} if {@link #setNegativePermissions()} has been called on this entry
   */
  @Override
  public boolean isNegative() {
    return negative;
  }

  /**
   * Adds the given permission to this entry.
   *
   * @param permission the permission to add, never {@code null}
   * @return {@code true} if the permission was added; {@code false} if it was already present or
   *     {@code permission} was {@code null}
   */
  @Override
  public boolean addPermission(Permission permission) {
    if (permission == null) return false;
    return permissions.add(permission);
  }

  /**
   * Removes the given permission from this entry.
   *
   * @param permission the permission to remove, never {@code null}
   * @return {@code true} if the permission was removed; {@code false} if it was not present or
   *     {@code permission} was {@code null}
   */
  @Override
  public boolean removePermission(Permission permission) {
    if (permission == null) return false;
    return permissions.remove(permission);
  }

  /**
   * Checks whether this entry grants the given permission.
   *
   * @param permission the permission to test, never {@code null}
   * @return {@code true} if this entry currently contains {@code permission}
   */
  @Override
  public boolean checkPermission(Permission permission) {
    if (permission == null) return false;
    return permissions.contains(permission);
  }

  /**
   * Returns an enumeration over the permissions held by this entry.
   *
   * @return a non-null, possibly empty enumeration of the permissions in insertion order; the
   *     returned enumeration is a snapshot and is not affected by later modifications
   */
  @Override
  public Enumeration<Permission> permissions() {
    if (permissions.isEmpty()) {
      return new Vector<Permission>().elements();
    }
    return Collections.enumeration(permissions);
  }

  /**
   * Returns a developer-friendly representation of this entry.
   *
   * @return a non-null string suitable for debugging and logging
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("AclEntryImpl{");
    sb.append("principal=").append(principal != null ? principal.getName() : "null");
    sb.append(", negative=").append(negative);
    sb.append(", permissions=").append(permissions);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Computes a hash code based on the entry's principal, negative flag, and permission set.
   *
   * @return a hash consistent with {@link #equals(Object)}
   */
  @Override
  public int hashCode() {
    // Define equality primarily by principal and negative flag and permission set
    int result = Objects.hash(principal, negative);
    result = 31 * result + permissions.hashCode();
    return result;
  }

  /**
   * Compares this entry with another for equality based on principal, negative flag, and permission
   * set.
   *
   * @param obj the object to compare, may be {@code null}
   * @return {@code true} if {@code obj} is an {@code AclEntryImpl} with the same principal,
   *     negative flag, and permission set
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AclEntryImpl)) return false;
    AclEntryImpl other = (AclEntryImpl) obj;
    return negative == other.negative
        && Objects.equals(principal, other.principal)
        && Objects.equals(permissions, other.permissions);
  }
}

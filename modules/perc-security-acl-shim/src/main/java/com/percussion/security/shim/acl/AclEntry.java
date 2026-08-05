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
package com.percussion.security.shim.acl;

import java.security.Principal;
import java.util.Enumeration;

/**
 * Minimal compatibility interface mirroring the legacy {@code java.security.acl.AclEntry} type used
 * by code that was written against the JDK ACL API.
 *
 * <p>Each entry is associated with a single {@link Principal} and holds an unordered set of {@link
 * Permission} instances. An entry may be marked as <em>negative</em>, in which case the permissions
 * it holds are treated as denials rather than grants.
 *
 * @author Percussion Software
 */
public interface AclEntry {

  /**
   * Sets the principal that owns this entry.
   *
   * @param user the principal to associate with this entry, never {@code null}
   * @return {@code true} if the principal was accepted; {@code false} if a different principal has
   *     already been set on this entry or if {@code user} is {@code null}
   */
  boolean setPrincipal(Principal user);

  /**
   * Returns the principal associated with this entry.
   *
   * @return the principal for this entry, or {@code null} if no principal has been set
   */
  Principal getPrincipal();

  /**
   * Marks this entry as a negative entry so its permissions are treated as denials. The mark is
   * permanent: once set, the entry cannot be converted back to a positive entry.
   */
  void setNegativePermissions();

  /**
   * Reports whether this entry is a negative entry.
   *
   * @return {@code true} if {@link #setNegativePermissions()} has been called on this entry
   */
  boolean isNegative();

  /**
   * Adds the given permission to this entry.
   *
   * @param permission the permission to add, never {@code null}
   * @return {@code true} if the permission was added; {@code false} if it was already present
   */
  boolean addPermission(Permission permission);

  /**
   * Removes the given permission from this entry.
   *
   * @param permission the permission to remove, never {@code null}
   * @return {@code true} if the permission was removed; {@code false} if it was not present
   */
  boolean removePermission(Permission permission);

  /**
   * Checks whether this entry grants the given permission.
   *
   * @param permission the permission to test, never {@code null}
   * @return {@code true} if this entry currently contains {@code permission}
   */
  boolean checkPermission(Permission permission);

  /**
   * Returns an enumeration over the permissions held by this entry.
   *
   * @return a non-null, possibly empty enumeration of the permissions in insertion order; the
   *     returned enumeration is a snapshot and is not affected by later modifications
   */
  Enumeration<Permission> permissions();

  /**
   * Returns a developer-friendly representation of this entry.
   *
   * @return a non-null string suitable for debugging and logging
   */
  String toString();
}

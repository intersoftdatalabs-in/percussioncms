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
import javax.security.auth.Subject;

/**
 * Minimal compatibility interface mirroring the legacy {@code java.security.acl.Acl} type used by
 * code that was written against the JDK ACL API.
 *
 * <p>Semantics: deny overrides allow where applicable; owners control modifications.
 * Implementations of this interface model the access control list for a single {@link #getName()
 * named} resource and maintain an ordered, deduplicated set of {@link AclEntry} instances.
 *
 * <p>This interface intentionally drops the deprecated {@code java.security.acl.Acl} type so that
 * the project can be built and run on JDK 21, which removed the legacy {@code java.security.acl}
 * package.
 *
 * @author Percussion Software
 */
public interface Acl extends Owner {

  /**
   * Returns the name of this ACL.
   *
   * @return the human-readable, non-null name used to identify this ACL
   */
  String getName();

  /**
   * Sets the name of this ACL.
   *
   * @param caller the principal attempting the rename; must be an owner of this ACL, never {@code
   *     null}
   * @param name the new non-null name to assign to this ACL
   * @throws NotOwnerException if {@code caller} is not an owner of this ACL
   */
  void setName(Principal caller, String name) throws NotOwnerException;

  /**
   * Adds the given entry to this ACL.
   *
   * @param caller the principal attempting the modification; must be an owner of this ACL, never
   *     {@code null}
   * @param entry the entry to add, never {@code null}
   * @return {@code true} if the entry was added (or replaced); {@code false} if an identical entry
   *     already exists
   * @throws NotOwnerException if {@code caller} is not an owner of this ACL
   */
  boolean addEntry(Principal caller, AclEntry entry) throws NotOwnerException;

  /**
   * Removes the given entry from this ACL.
   *
   * @param caller the principal attempting the modification; must be an owner of this ACL, never
   *     {@code null}
   * @param entry the entry to remove, never {@code null}
   * @return {@code true} if the entry was removed; {@code false} if no matching entry was found
   * @throws NotOwnerException if {@code caller} is not an owner of this ACL
   */
  boolean removeEntry(Principal caller, AclEntry entry) throws NotOwnerException;

  /**
   * Returns an enumeration over the entries currently held by this ACL.
   *
   * @return a non-null, possibly empty enumeration of the entries in the order they were added;
   *     modifications to the ACL are not reflected in the returned enumeration
   */
  Enumeration<AclEntry> entries();

  /**
   * Checks whether the given subject is granted the given permission by this ACL.
   *
   * @param subject the authenticated subject whose principals should be matched, never {@code null}
   * @param permission the permission to test, never {@code null}
   * @return {@code true} if any matching entry grants {@code permission} and no matching entry
   *     denies it; {@code false} otherwise
   */
  boolean checkPermission(Subject subject, Permission permission);
}

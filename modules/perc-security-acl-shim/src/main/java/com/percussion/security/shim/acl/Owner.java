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
package com.percussion.security.shim.acl;

import java.security.Principal;

/**
 * Minimal compatibility interface mirroring the legacy {@code java.security.acl.Owner} type used by
 * code that was written against the JDK ACL API.
 *
 * <p>An ACL that implements this interface maintains a set of owner principals; only owners may
 * modify the ACL itself (renaming it, adding or removing entries, or deleting other owners).
 *
 * @author Percussion Software
 */
public interface Owner {

  /**
   * Adds a new owner to this ACL.
   *
   * @param caller the principal attempting the modification; must already be an owner, never {@code
   *     null}
   * @param owner the principal to add as a new owner, never {@code null}
   * @return {@code true} if the principal was added as an owner; {@code false} if it was already an
   *     owner
   * @throws NotOwnerException if {@code caller} is not currently an owner of this ACL
   */
  boolean addOwner(Principal caller, Principal owner) throws NotOwnerException;

  /**
   * Removes an owner from this ACL.
   *
   * @param caller the principal attempting the modification; must already be an owner, never {@code
   *     null}
   * @param owner the principal to remove as an owner, never {@code null}
   * @return {@code true} if the principal was removed; {@code false} if it was not an owner
   * @throws NotOwnerException if {@code caller} is not currently an owner of this ACL
   * @throws LastOwnerException if the removal would leave this ACL with no owners
   */
  boolean deleteOwner(Principal caller, Principal owner)
      throws NotOwnerException, LastOwnerException;

  /**
   * Reports whether the given principal is an owner of this ACL.
   *
   * @param owner the principal to test, never {@code null}
   * @return {@code true} if {@code owner} is currently an owner of this ACL
   */
  boolean isOwner(Principal owner);
}

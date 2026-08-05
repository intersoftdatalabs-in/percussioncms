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
 * Compatibility shim for {@code javax.security.acl.Group} used for legacy ACL handling.
 *
 * <p>A group is a named collection of {@link Principal} instances. Group membership drives ACL
 * resolution when a {@link #isMember(Principal) member principal} is checked against an {@link
 * Acl}.
 *
 * @author Percussion Software
 */
public interface Group extends Principal {

  /**
   * Adds a principal to this group.
   *
   * @param user the principal to add as a member, never {@code null}
   * @return {@code true} if the principal was added; {@code false} if it was already a member
   */
  boolean addMember(Principal user);

  /**
   * Removes a principal from this group.
   *
   * @param user the principal to remove from this group, never {@code null}
   * @return {@code true} if the principal was removed; {@code false} if it was not a member
   */
  boolean removeMember(Principal user);

  /**
   * Reports whether the given principal is a member of this group.
   *
   * @param member the principal to test, never {@code null}
   * @return {@code true} if {@code member} is currently a member of this group
   */
  boolean isMember(Principal member);

  /**
   * Returns an enumeration of this group's members.
   *
   * @return a non-null, possibly empty enumeration of the group's members; the returned enumeration
   *     is a snapshot and is not affected by later modifications to the group
   */
  Enumeration<? extends Principal> members();
}

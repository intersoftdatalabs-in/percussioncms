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
package com.percussion.security.shim.acl.impl;

import com.percussion.security.shim.acl.Group;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Default in-memory implementation of {@link Group} used for legacy ACL compatibility.
 *
 * <p>Membership is stored in a {@link HashSet} keyed by the {@link Principal} reference; two
 * distinct principals with the same {@link Principal#getName() name} are considered different
 * members. Instances are not thread-safe; external synchronization is required when sharing an
 * instance across threads.
 *
 * @author Percussion Software
 */
public class GroupImpl implements Group {
  private final String name;
  private final Set<Principal> members = new HashSet<>();

  /**
   * Creates a new group with the given name.
   *
   * @param name the human-readable name of this group, never {@code null}
   */
  public GroupImpl(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean addMember(Principal user) {
    return members.add(user);
  }

  @Override
  public boolean removeMember(Principal user) {
    return members.remove(user);
  }

  @Override
  public boolean isMember(Principal member) {
    return members.contains(member);
  }

  @Override
  public Enumeration<? extends Principal> members() {
    return Collections.enumeration(members);
  }

  @Override
  public String toString() {
    return "GroupImpl{" + "name='" + name + '\'' + ", members=" + members + '}';
  }
}

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

import com.percussion.security.shim.acl.Acl;
import com.percussion.security.shim.acl.AclEntry;
import com.percussion.security.shim.acl.LastOwnerException;
import com.percussion.security.shim.acl.NotOwnerException;
import com.percussion.security.shim.acl.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import javax.security.auth.Subject;

/**
 * In-memory implementation of Acl with owner management and deny-overrides-allow semantics. Mirrors
 * java.security.acl.Acl behavior closely to minimize calling-site changes.
 */
public class AclImpl implements Acl {

  private String name;
  private final Set<Principal> owners = new LinkedHashSet<>();
  private final List<AclEntry> entries = new ArrayList<>();

  /**
   * Create an ACL with an initial owner.
   *
   * @param initialOwner required owner; cannot be null
   * @param name optional ACL name
   */
  public AclImpl(Principal initialOwner, String name) {
    Objects.requireNonNull(initialOwner, "initialOwner must not be null");
    this.owners.add(initialOwner);
    this.name = name;
  }

  @Override
  public boolean addOwner(Principal caller, Principal owner) throws NotOwnerException {
    requireOwner(caller);
    return owners.add(owner);
  }

  @Override
  public boolean deleteOwner(Principal caller, Principal owner)
      throws NotOwnerException, LastOwnerException {
    requireOwner(caller);
    if (!owners.contains(owner)) {
      return false;
    }
    if (owners.size() == 1 && owners.contains(owner)) {
      throw new LastOwnerException("Cannot remove the last remaining owner");
    }
    return owners.remove(owner);
  }

  @Override
  public boolean isOwner(Principal owner) {
    return owners.contains(owner);
  }

  @Override
  public void setName(Principal caller, String name) throws NotOwnerException {
    requireOwner(caller);
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean addEntry(Principal caller, AclEntry entry) throws NotOwnerException {
    requireOwner(caller);
    if (entry == null) return false;
    if (entries.contains(entry)) return false;
    return entries.add(entry);
  }

  @Override
  public boolean removeEntry(Principal caller, AclEntry entry) throws NotOwnerException {
    requireOwner(caller);
    if (entry == null) return false;
    return entries.remove(entry);
  }

  @Override
  public Enumeration<AclEntry> entries() {
    if (entries.isEmpty()) {
      return new Vector<AclEntry>().elements();
    }
    return Collections.enumeration(entries);
  }

  /**
   * Deny overrides allow: - If any matching negative entry contains the permission for the subject,
   * deny. - Else if any matching positive entry contains the permission, allow. - Else deny.
   */
  @Override
  public boolean checkPermission(Subject subject, Permission permission) {
    if (subject == null || permission == null) return false;

    Set<Principal> subjectPrincipals = subject.getPrincipals();
    if (subjectPrincipals == null || subjectPrincipals.isEmpty()) {
      return false;
    }

    boolean anyPositive = false;

    for (AclEntry entry : entries) {
      Principal p = entry.getPrincipal();
      if (p == null) continue;
      if (!subjectPrincipals.contains(p)) continue;

      if (entry.checkPermission(permission)) {
        if (entry.isNegative()) {
          // Explicit deny wins
          return false;
        } else {
          anyPositive = true;
        }
      }
    }
    return anyPositive;
  }

  private void requireOwner(Principal caller) throws NotOwnerException {
    if (!isOwner(caller)) {
      throw new NotOwnerException("Caller is not an owner");
    }
  }

  @Override
  public String toString() {
    return "AclImpl{"
        + "name='"
        + name
        + '\''
        + ", owners="
        + owners
        + ", entries="
        + entries
        + '}';
  }
}

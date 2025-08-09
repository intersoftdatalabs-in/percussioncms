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
 * In-memory implementation of AclEntry with optional negative permissions flag. Mirrors
 * java.security.acl.AclEntry behavior closely for compatibility.
 */
public class AclEntryImpl implements AclEntry {

  private Principal principal;
  private final Set<Permission> permissions = new LinkedHashSet<>();
  private boolean negative;

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

  @Override
  public Principal getPrincipal() {
    return principal;
  }

  @Override
  public void setNegativePermissions() {
    this.negative = true;
  }

  @Override
  public boolean isNegative() {
    return negative;
  }

  @Override
  public boolean addPermission(Permission permission) {
    if (permission == null) return false;
    return permissions.add(permission);
  }

  @Override
  public boolean removePermission(Permission permission) {
    if (permission == null) return false;
    return permissions.remove(permission);
  }

  @Override
  public boolean checkPermission(Permission permission) {
    if (permission == null) return false;
    return permissions.contains(permission);
  }

  @Override
  public Enumeration<Permission> permissions() {
    if (permissions.isEmpty()) {
      return new Vector<Permission>().elements();
    }
    return Collections.enumeration(permissions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("AclEntryImpl{");
    sb.append("principal=").append(principal != null ? principal.getName() : "null");
    sb.append(", negative=").append(negative);
    sb.append(", permissions=").append(permissions);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public int hashCode() {
    // Define equality primarily by principal and negative flag and permission set
    int result = Objects.hash(principal, negative);
    result = 31 * result + permissions.hashCode();
    return result;
  }

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

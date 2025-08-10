// REFACTORED: CP-JAVA11
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
package com.percussion.pathmanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import java.util.List;
import java.util.Objects;

/**
 * Represents the permissions of a folder.
 *
 * @author yubingchen
 */
public class PSFolderPermission extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  /** Access levels for folder permissions. */
  public enum Access {
    /** ADMIN is the least restrictive access level. Includes READ and WRITE. */
    ADMIN,
    /** WRITE is more restrictive than ADMIN, but less than READ. Includes READ. */
    WRITE,
    /** READ is more restrictive than WRITE. */
    READ,
    /** VIEW is the most restrictive access level. */
    VIEW
  }

  /** Principal types for folder permissions. */
  public enum PrincipalType {
    USER,
    ROLE
  }

  /** Represents a user or role that has ADMIN, READ, WRITE, or VIEW permission. */
  public static class Principal extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;
    private PrincipalType type;
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public PrincipalType getType() {
      return type;
    }

    public void setType(PrincipalType type) {
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Principal)) return false;
      Principal principal = (Principal) o;
      return type == principal.type && Objects.equals(name, principal.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, name);
    }
  }

  private Access accessLevel = Access.ADMIN;
  private List<Principal> adminPrincipals;
  private List<Principal> writePrincipals;
  private List<Principal> readPrincipals;
  private List<Principal> viewPrincipals;

  /**
   * Gets the access level applied to unspecified principals. Defaults to {@link Access#ADMIN} if
   * not set.
   *
   * @return the access level, never null
   */
  public Access getAccessLevel() {
    return accessLevel;
  }

  /**
   * Sets the access level applied to unspecified principals.
   *
   * @param access the new access level
   */
  public void setAccessLevel(Access access) {
    this.accessLevel = access;
  }

  /**
   * Gets the list of principals with ADMIN access.
   *
   * @return the list, may be null or empty
   */
  public List<Principal> getAdminPrincipals() {
    return adminPrincipals;
  }

  /**
   * Sets the list of principals with ADMIN access.
   *
   * @param principals the new list, may be null or empty
   */
  public void setAdminPrincipals(List<Principal> principals) {
    this.adminPrincipals = principals;
  }

  /**
   * Gets the list of principals with WRITE access.
   *
   * @return the list, may be null or empty
   */
  public List<Principal> getWritePrincipals() {
    return writePrincipals;
  }

  /**
   * Sets the list of principals with WRITE access.
   *
   * @param principals the new list, may be null or empty
   */
  public void setWritePrincipals(List<Principal> principals) {
    this.writePrincipals = principals;
  }

  /**
   * Gets the list of principals with READ access.
   *
   * @return the list, may be null or empty
   */
  public List<Principal> getReadPrincipals() {
    return readPrincipals;
  }

  /**
   * Sets the list of principals with READ access.
   *
   * @param principals the new list, may be null or empty
   */
  public void setReadPrincipals(List<Principal> principals) {
    this.readPrincipals = principals;
  }

  /**
   * Gets the list of principals with VIEW access.
   *
   * @return the list, may be null or empty
   */
  public List<Principal> getViewPrincipals() {
    return viewPrincipals;
  }

  /**
   * Sets the list of principals with VIEW access.
   *
   * @param viewPrincipals the new list, may be null or empty
   */
  public void setViewPrincipals(List<Principal> viewPrincipals) {
    this.viewPrincipals = viewPrincipals;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSFolderPermission)) return false;
    PSFolderPermission that = (PSFolderPermission) o;
    return accessLevel == that.accessLevel
        && Objects.equals(adminPrincipals, that.adminPrincipals)
        && Objects.equals(writePrincipals, that.writePrincipals)
        && Objects.equals(readPrincipals, that.readPrincipals)
        && Objects.equals(viewPrincipals, that.viewPrincipals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        accessLevel, adminPrincipals, writePrincipals, readPrincipals, viewPrincipals);
  }
}

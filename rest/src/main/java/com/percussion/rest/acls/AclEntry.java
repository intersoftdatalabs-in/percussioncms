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

// REFACTORED: CP-JAVA11
package com.percussion.rest.acls;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * REST representation of an ACL entry.
 *
 * <p>Wire getters return plain nullable types (not {@code Optional}) so Jackson/CXF JSON emits
 * scalars and nested objects, not Optional beans. Matches {@link Acl} getter style (issue #3388
 * slice 8 / #3420).
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "AclEntry")
public class AclEntry {

  /** Numeric identifier of the entry. */
  private long id;

  /** Human-readable name of the entry. */
  @XmlElement @JsonProperty private String name;

  /** Principal associated with the entry, may be {@code null}. */
  @XmlElement @JsonProperty private Principal principal;

  /** Typed principal classification. */
  @XmlElement @JsonProperty private TypedPrincipal type;

  /** Permission set granted by this entry. */
  @XmlElement @JsonProperty private UserAccessLevelList permissions;

  /** Identifier of the owning ACL. */
  private long aclId;

  /** No-op constructor. */
  public AclEntry() {}

  /**
   * Creates a new entry populated with the supplied values.
   *
   * @param id numeric identifier
   * @param name human-readable name
   * @param principal associated principal
   * @param type typed principal classification
   * @param permissions permissions granted
   * @param aclId owning ACL id
   */
  public AclEntry(
      long id,
      String name,
      Principal principal,
      TypedPrincipal type,
      UserAccessLevelList permissions,
      long aclId) {
    this.id = id;
    this.name = name;
    this.principal = principal;
    this.type = type;
    this.permissions = permissions;
    this.aclId = aclId;
  }

  /**
   * Returns the owning ACL id.
   *
   * @return the ACL id
   */
  public long getAclId() {
    return aclId;
  }

  /**
   * Sets the owning ACL id.
   *
   * @param aclId the new ACL id
   */
  public void setAclId(long aclId) {
    this.aclId = aclId;
  }

  /**
   * Returns the entry id.
   *
   * @return the entry id
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the entry id.
   *
   * @param id the new entry id
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the entry name.
   *
   * @return the name, may be {@code null}
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the entry name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the associated principal.
   *
   * @return the principal, may be {@code null}
   */
  public Principal getPrincipal() {
    return principal;
  }

  /**
   * Sets the associated principal.
   *
   * @param principal the new principal
   */
  public void setPrincipal(Principal principal) {
    this.principal = principal;
  }

  /**
   * Returns the typed principal classification.
   *
   * @return the type, may be {@code null}
   */
  public TypedPrincipal getType() {
    return type;
  }

  /**
   * Sets the typed principal classification.
   *
   * @param type the new type
   */
  public void setType(TypedPrincipal type) {
    this.type = type;
  }

  /**
   * Returns the permissions granted by this entry.
   *
   * @return the permissions, may be {@code null}
   */
  public UserAccessLevelList getPermissions() {
    return permissions;
  }

  /**
   * Sets the permissions granted by this entry.
   *
   * @param permissions the new permissions
   */
  public void setPermissions(UserAccessLevelList permissions) {
    this.permissions = permissions;
  }

  @Override
  public String toString() {
    return "AclEntry{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", principal="
        + principal
        + ", type="
        + type
        + ", permissions="
        + permissions
        + ", aclId="
        + aclId
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AclEntry)) return false;
    var aclEntry = (AclEntry) o;
    return id == aclEntry.id
        && aclId == aclEntry.aclId
        && Objects.equals(name, aclEntry.name)
        && Objects.equals(principal, aclEntry.principal)
        && Objects.equals(type, aclEntry.type)
        && Objects.equals(permissions, aclEntry.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, principal, type, permissions, aclId);
  }
}

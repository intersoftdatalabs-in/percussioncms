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

package com.percussion.rest.acls;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * REST representation of an access control list.
 *
 * <p>Wire getters return plain nullable types (not {@code Optional}) so Jackson/CXF JSON emits
 * scalars and nested objects, not Optional beans ({@code empty}/{@code present}). Matches {@link
 * com.percussion.rest.contenttypes.ContentType} getter style (issue #3388 slice 8 / #3420).
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Acl")
public class Acl {

  /** Numeric identifier of the ACL. */
  private long id;

  /** Optional GUID identifying the ACL. */
  @XmlElement @JsonProperty private Guid guid;

  /** Human-readable name of the ACL. */
  @XmlElement @JsonProperty private String name;

  /** Identifier of the secured object. */
  private long objectId;

  /** Free-form description of the ACL. */
  @XmlElement @JsonProperty private String description;

  /** Entries that make up the ACL. */
  @XmlElement @JsonProperty private AclEntryList aclEntries;

  /** Type identifier of the secured object. */
  private int objectType;

  /** Optional GUID of the secured object. */
  @XmlElement @JsonProperty private Guid objectGuid;

  /** No-op constructor. */
  public Acl() {}

  /**
   * Creates a new ACL populated with the supplied values.
   *
   * @param id numeric identifier of the ACL
   * @param guid GUID of the ACL, may be {@code null}
   * @param name name of the ACL
   * @param objectId identifier of the secured object
   * @param description free-form description
   * @param aclEntries entries on the ACL
   * @param objectType type identifier of the secured object
   * @param objectGuid GUID of the secured object, may be {@code null}
   */
  public Acl(
      long id,
      Guid guid,
      String name,
      long objectId,
      String description,
      AclEntryList aclEntries,
      int objectType,
      Guid objectGuid) {
    this.id = id;
    this.guid = guid;
    this.name = name;
    this.objectId = objectId;
    this.description = description;
    this.aclEntries = aclEntries;
    this.objectType = objectType;
    this.objectGuid = objectGuid;
  }

  /**
   * Returns the ACL's numeric identifier.
   *
   * @return the ACL id
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the ACL's numeric identifier.
   *
   * @param id the new id
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the ACL's GUID.
   *
   * @return the GUID, may be {@code null}
   */
  public Guid getGuid() {
    return guid;
  }

  /**
   * Sets the ACL's GUID.
   *
   * @param guid the new GUID
   */
  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  /**
   * Returns the ACL's name.
   *
   * @return the name, may be {@code null}
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the ACL's name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the identifier of the secured object.
   *
   * @return the object id
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   * Sets the identifier of the secured object.
   *
   * @param objectId the new object id
   */
  public void setObjectId(long objectId) {
    this.objectId = objectId;
  }

  /**
   * Returns the ACL's description.
   *
   * @return the description, may be {@code null}
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the ACL's description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the ACL's entries.
   *
   * @return the entries, may be {@code null}
   */
  public AclEntryList getAclEntries() {
    return aclEntries;
  }

  /**
   * Replaces the ACL's entries.
   *
   * @param aclEntries the new entries
   */
  public void setAclEntries(AclEntryList aclEntries) {
    this.aclEntries = aclEntries;
  }

  /**
   * Returns the type identifier of the secured object.
   *
   * @return the object type
   */
  public int getObjectType() {
    return objectType;
  }

  /**
   * Sets the type identifier of the secured object.
   *
   * @param objectType the new object type
   */
  public void setObjectType(int objectType) {
    this.objectType = objectType;
  }

  /**
   * Returns the GUID of the secured object.
   *
   * @return the object GUID, may be {@code null}
   */
  public Guid getObjectGuid() {
    return objectGuid;
  }

  /**
   * Sets the GUID of the secured object.
   *
   * @param objectGuid the new object GUID
   */
  public void setObjectGuid(Guid objectGuid) {
    this.objectGuid = objectGuid;
  }

  @Override
  public String toString() {
    return "Acl{"
        + "id="
        + id
        + ", guid="
        + guid
        + ", name='"
        + name
        + '\''
        + ", objectId="
        + objectId
        + ", description='"
        + description
        + '\''
        + ", aclEntries="
        + aclEntries
        + ", objectType="
        + objectType
        + ", objectGuid="
        + objectGuid
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Acl)) return false;
    var acl = (Acl) o;
    return id == acl.id
        && objectId == acl.objectId
        && objectType == acl.objectType
        && Objects.equals(guid, acl.guid)
        && Objects.equals(name, acl.name)
        && Objects.equals(description, acl.description)
        && Objects.equals(aclEntries, acl.aclEntries)
        && Objects.equals(objectGuid, acl.objectGuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, guid, name, objectId, description, aclEntries, objectType, objectGuid);
  }
}

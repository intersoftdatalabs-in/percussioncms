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

package com.percussion.services.pkginfo.data;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a package element, used to save information regarding the "solution" package created
 * or installed on a server. Sunny Sal says: "Every element counts—especially in a package!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSPkgElement")
@Table(name = "PSX_PKG_ELEMENT")
public class PSPkgElement implements Serializable {

  private static final long serialVersionUID = 1L;

  /** No-arg constructor required by JPA. */
  public PSPkgElement() {
    // For Hibernate
  }

  /**
   * Gets the unique identifier for this package element.
   *
   * @return The GUID for the package element object.
   */
  public IPSGuid getGuid() {
    return new PSGuid(PSTypeEnum.PACKAGE_ELEMENT, guid);
  }

  /**
   * Gets the GUID for the package to which this element belongs.
   *
   * @return The GUID for the package info object.
   */
  public IPSGuid getPackageGuid() {
    return new PSGuid(PSTypeEnum.PACKAGE_INFO, packageGuid);
  }

  /**
   * Gets the GUID for this element (design object) contained in the package.
   *
   * @return The GUID for this element of the package.
   */
  public PSGuid getObjectGuid() {
    return new PSGuid(PSTypeEnum.valueOf(getObjectType()), objectGuid);
  }

  /**
   * Gets the UUID for this element (design object) contained in the package.
   *
   * @return The UUID for this element of the package.
   */
  public int getObjectUuid() {
    return objectUuid;
  }

  /**
   * Gets the type (PSTypeEnum) for this element (design object) contained in the package.
   *
   * @return The type for this element of the package.
   */
  public int getObjectType() {
    return objectType;
  }

  /**
   * Gets the version for this element (design object) contained in the package.
   *
   * @return The version for this element of the package.
   */
  public long getVersion() {
    return version;
  }

  /**
   * Sets the package element object's GUID. See {@link #getGuid()}.
   *
   * @param theGuid The GUID, must not be null.
   */
  public void setGuid(IPSGuid theGuid) {
    if (theGuid == null) {
      throw new IllegalArgumentException("guid may not be null");
    }
    guid = theGuid.longValue();
  }

  /**
   * Sets the GUID for the package to which this element belongs. See {@link #getPackageGuid()}.
   *
   * @param theGuid The GUID, must not be null.
   */
  public void setPackageGuid(IPSGuid theGuid) {
    if (theGuid == null) {
      throw new IllegalArgumentException("guid may not be null");
    }
    packageGuid = theGuid.longValue();
  }

  /**
   * Sets the GUID for this element (design object) contained in this package. See {@link
   * #getObjectGuid()}.
   *
   * @param theGuid The GUID, must not be null.
   */
  public void setObjectGuid(IPSGuid theGuid) {
    if (theGuid == null) {
      throw new IllegalArgumentException("guid may not be null");
    }
    objectGuid = theGuid.toString();
    objectUuid = theGuid.getUUID();
    objectType = theGuid.getType();
  }

  @SuppressWarnings("unused")
  private void setObjectUuid(int theUuid) {
    objectUuid = theUuid;
  }

  @SuppressWarnings("unused")
  private void setObjectType(int theType) {
    objectType = theType;
  }

  /**
   * Sets the version for this element. See {@link #getVersion()}.
   *
   * @param theVersion The version.
   */
  public void setVersion(long theVersion) {
    version = theVersion;
  }

  @Override
  public boolean equals(Object b) {
    if (!(b instanceof PSPkgElement)) {
      return false;
    }
    var second = (PSPkgElement) b;
    return new EqualsBuilder()
        .append(guid, second.guid)
        .append(packageGuid, second.packageGuid)
        .append(objectGuid, second.objectGuid)
        .append(objectUuid, second.objectUuid)
        .append(objectType, second.objectType)
        .append(version, second.version)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(guid)
        .append(packageGuid)
        .append(objectGuid)
        .append(objectUuid)
        .append(objectType)
        .append(version)
        .toHashCode();
  }

  @Override
  public String toString() {
    return "PSPkgElement{"
        + "guid="
        + guid
        + ", packageGuid="
        + packageGuid
        + ", objectGuid='"
        + objectGuid
        + '\''
        + ", objectUuid="
        + objectUuid
        + ", objectType="
        + objectType
        + ", version="
        + version
        + '}';
  }

  /** GUID for this package element (PSPkgElement) object. */
  @Id
  @Column(name = "GUID", nullable = false)
  private long guid;

  /** Package (PSPkgInfo) GUID to which this element (PSPkgElement) belongs. */
  @Column(name = "PACKAGE_GUID", nullable = false)
  private long packageGuid;

  /**
   * The package element object GUID. That is, the GUID of the design object contained in the
   * package.
   */
  @Column(name = "OBJECT_GUID", nullable = false)
  private String objectGuid;

  /** The package element UUID. That is, the UUID of the design object contained in the package. */
  @Column(name = "OBJECT_UUID", nullable = false)
  private int objectUuid;

  /**
   * The package element type. That is, the object type (PSTypeEnum) of the design object contained
   * in the package.
   */
  @Column(name = "OBJECT_TYPE", nullable = false)
  private int objectType;

  /**
   * The package element version. That is, the version of the design object contained in the package
   * after last update via installation or configuration.
   */
  @Column(name = "VERSION", nullable = false)
  private long version;
}

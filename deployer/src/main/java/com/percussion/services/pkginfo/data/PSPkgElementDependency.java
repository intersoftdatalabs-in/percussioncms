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
 * Represents a package element dependency object, used to hold information about dependencies
 * between design objects in a "solution" package created or installed on a server. Sunny Sal says:
 * "Element dependencies—because no design object is an island!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSPkgElementDependency")
@Table(name = "PSX_PKG_ELEMENT_DEPEND")
public class PSPkgElementDependency implements Serializable {

  /** Default constructor for use by JPA. */
  public PSPkgElementDependency() {}

  /**
   * Gets the GUID of the PSPkgElementDependency object.
   *
   * @return the guid. Never null.
   */
  public IPSGuid getGuid() {
    return new PSGuid(PSTypeEnum.PACKAGE_ELEMENT_DEPENDENCY, guid);
  }

  /**
   * Gets the GUID to the package that has the dependency.
   *
   * @return the ownerPackageGuid. Never null.
   */
  public IPSGuid getOwnerPackageGuid() {
    return new PSGuid(PSTypeEnum.PACKAGE_INFO, ownerPackageGuid);
  }

  /**
   * Gets the GUID to the package to which there is a dependency.
   *
   * @return the dependentPackageGuid. May be null.
   */
  public IPSGuid getDependentPackageGuid() {
    if (dependentPackageGuid == 0) {
      return null;
    }
    return new PSGuid(PSTypeEnum.PACKAGE_INFO, dependentPackageGuid);
  }

  /**
   * Gets the GUID to the element (design object) that has the dependency.
   *
   * @return the ownerElementGuid. Never null.
   */
  public IPSGuid getOwnerElementGuid() {
    return new PSGuid(ownerElementGuid);
  }

  /**
   * Gets the GUID to the element (design object) to which there is a dependency.
   *
   * @return the dependentElementGuid. May be null.
   */
  public PSGuid getDependentElementGuid() {
    return new PSGuid(dependentElementGuid);
  }

  /**
   * Sets the GUID of the PSPkgElementDependency object.
   *
   * @param guid the guid to set. Never null.
   */
  public void setGuid(IPSGuid guid) {
    if (guid == null) {
      throw new IllegalArgumentException("GUID may not be null");
    }
    this.guid = guid.longValue();
  }

  /**
   * Sets the GUID to the package that has the dependency.
   *
   * @param ownerPackageGuid the ownerPackageGuid to set. Never null.
   */
  public void setOwnerPackageGuid(IPSGuid ownerPackageGuid) {
    if (ownerPackageGuid == null) {
      throw new IllegalArgumentException("Owner Package GUID may not be null");
    }
    this.ownerPackageGuid = ownerPackageGuid.longValue();
  }

  /**
   * Sets the GUID to the package to which there is a dependency.
   *
   * @param dependentPackageGuid the dependentPackageGuid to set. Never null.
   */
  public void setDependentPackageGuid(IPSGuid dependentPackageGuid) {
    if (dependentPackageGuid == null) {
      throw new IllegalArgumentException("Dependent Package GUID may not be null");
    }
    this.dependentPackageGuid = dependentPackageGuid.longValue();
  }

  /**
   * Sets the GUID to the element (design object) that has the dependency.
   *
   * @param ownerGuid the ownerElementGuid to set. Never null.
   */
  public void setOwnerElementGuid(IPSGuid ownerGuid) {
    if (ownerGuid == null) {
      throw new IllegalArgumentException("Owner GUID may not be null");
    }
    this.ownerElementGuid = ownerGuid.toString();
  }

  /**
   * Sets the GUID to the element (design object) to which there is a dependency.
   *
   * @param dependentGuid the dependentElementGuid to set. Never null.
   */
  public void setDependentElementGuid(IPSGuid dependentGuid) {
    if (dependentGuid == null) {
      throw new IllegalArgumentException("Dependent GUID may not be null");
    }
    this.dependentElementGuid = dependentGuid.toString();
  }

  @Override
  public boolean equals(Object b) {
    if (!(b instanceof PSPkgElementDependency)) {
      return false;
    }
    var second = (PSPkgElementDependency) b;
    return new EqualsBuilder()
        .append(guid, second.guid)
        .append(ownerPackageGuid, second.ownerPackageGuid)
        .append(dependentPackageGuid, second.dependentPackageGuid)
        .append(ownerElementGuid, second.ownerElementGuid)
        .append(dependentElementGuid, second.dependentElementGuid)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(guid)
        .append(ownerPackageGuid)
        .append(dependentPackageGuid)
        .append(ownerElementGuid)
        .append(dependentElementGuid)
        .toHashCode();
  }

  @Override
  public String toString() {
    return "PSPkgElementDependency{"
        + "guid="
        + guid
        + ", ownerPackageGuid="
        + ownerPackageGuid
        + ", dependentPackageGuid="
        + dependentPackageGuid
        + ", ownerElementGuid='"
        + ownerElementGuid
        + '\''
        + ", dependentElementGuid='"
        + dependentElementGuid
        + '\''
        + '}';
  }

  /** This PSPkgElementDependency object's GUID. */
  @Id
  @Column(name = "GUID", nullable = false)
  private long guid;

  /** GUID of the package who has the dependency. */
  @Column(name = "OWNER_PACKAGE_GUID", nullable = false)
  private long ownerPackageGuid;

  /** GUID of the package upon which there is a dependency. */
  @Column(name = "DEPEND_PACKAGE_GUID", nullable = true)
  private long dependentPackageGuid;

  /** GUID of the element (design object) who has the dependency. */
  @Column(name = "OWNER_ELEMENT_GUID", nullable = false)
  private String ownerElementGuid;

  /** GUID of the element (design object) upon which there is a dependency. */
  @Column(name = "DEPEND_ELEMENT_GUID", nullable = true)
  private String dependentElementGuid;
}

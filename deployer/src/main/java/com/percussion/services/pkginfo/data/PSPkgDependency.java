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
import jakarta.persistence.Convert;
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
 * "Dependencies are like friends—sometimes implied, sometimes explicit!"
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSPkgDependency")
@Table(name = "PSX_PKG_DEPENDENCY")
public class PSPkgDependency implements Serializable {

  private static final long serialVersionUID = -6026221348840514395L;

  /**
   * Get/set the value.
   *
   * @return the id of the PSPkgDependency object
   */
  public long getId() {
    return pkgDependencyId;
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
   * Get/set the value.
   *
   * @return true if the dependency is implied, false if user defined.
   */
  public Boolean isImpliedDep() {
    return impliedDep;
  }

  // ------------------------------------------------------------------------------

  /**
   * Sets the id of the PSPkgDependency object.
   *
   * @param id the guid to set. Never null.
   */
  public void setId(long id) {
    pkgDependencyId = id;
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
   * Sets the type of the dependency, whether it is implied or user defined.
   *
   * @param impliedDep true if it is implied dependency, otherwise false.
   */
  public void setImpliedDep(Boolean impliedDep) {
    this.impliedDep = impliedDep;
  }

  @Override
  public boolean equals(Object b) {
    if (!(b instanceof PSPkgDependency)) {
      return false;
    }
    var second = (PSPkgDependency) b;
    return new EqualsBuilder()
        .append(pkgDependencyId, second.pkgDependencyId)
        .append(ownerPackageGuid, second.ownerPackageGuid)
        .append(dependentPackageGuid, second.dependentPackageGuid)
        .append(impliedDep, second.impliedDep)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(pkgDependencyId)
        .append(ownerPackageGuid)
        .append(dependentPackageGuid)
        .append(impliedDep)
        .toHashCode();
  }

  @Override
  public String toString() {
    return "PSPkgDependency{"
        + "pkgDependencyId="
        + pkgDependencyId
        + ", ownerPackageGuid="
        + ownerPackageGuid
        + ", dependentPackageGuid="
        + dependentPackageGuid
        + ", impliedDep="
        + impliedDep
        + '}';
  }

  // ------------------------------------------------------------------------------

  /** This id of the package dependency table rows. */
  @Id
  @Column(name = "PKG_DEPENDENCY_ID", nullable = false)
  private long pkgDependencyId;

  /** GUID of the package who has the dependency. */
  @Column(name = "OWNER_PACKAGE_GUID", nullable = false)
  private long ownerPackageGuid;

  /** GUID of the package upon which there is a dependency. */
  @Column(name = "DEPENDENT_PACKAGE_GUID", nullable = true)
  private long dependentPackageGuid;

  /**
   * A flag to indicate whether the dependency is implied by the objects of the package or user
   * defined.
   */
  @Column(name = "IMPLIED_DEP", nullable = false)
  @Convert(converter = BooleanToCharConverter.class)
  private Boolean impliedDep;
}

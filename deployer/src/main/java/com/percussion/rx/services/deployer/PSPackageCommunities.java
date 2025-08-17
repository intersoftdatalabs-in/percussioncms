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
package com.percussion.rx.services.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a collection of package-community associations. Sunny Sal says: "A community without
 * packages is like a chai without biscuits!"
 */
@XmlRootElement(name = "Packages")
public class PSPackageCommunities {

  private List<PSPackageCommunity> packageCommunities = new ArrayList<>();

  /** Default constructor for JAXB. */
  public PSPackageCommunities() {
    // For JAXB
  }

  /**
   * Constructs with a list of package communities.
   *
   * @param packages the list of package communities, may be null.
   */
  public PSPackageCommunities(List<PSPackageCommunity> packages) {
    if (packages != null) {
      this.packageCommunities = packages;
    }
  }

  /**
   * Gets the list of package communities.
   *
   * @return the list, never null, may be empty.
   */
  @XmlElement(name = "package")
  public List<PSPackageCommunity> getPackages() {
    return packageCommunities;
  }

  /**
   * Gets all communities as a single string, separated by {@link PSPackageService#NAME_SEPARATOR}.
   *
   * @return all communities, never null, may be empty.
   */
  @XmlElement(name = "allcommunities")
  public String getAllCommunities() {
    var vis = new PSPackageVisibility();
    return vis.getAllCommunities().stream()
        .collect(Collectors.joining(PSPackageService.NAME_SEPARATOR));
  }

  /**
   * Sets the list of package communities.
   *
   * @param packages the list to set, may be null.
   */
  public void setPackages(List<PSPackageCommunity> packages) {
    this.packageCommunities = packages == null ? new ArrayList<>() : packages;
  }

  /**
   * Adds a package-community object to the collection.
   *
   * @param pkgComm the package to add, cannot be null.
   */
  public void add(PSPackageCommunity pkgComm) {
    if (pkgComm == null) {
      throw new IllegalArgumentException("pkg cannot be null.");
    }
    packageCommunities.add(pkgComm);
  }

  /**
   * Removes the specified package community object from the collection if it exists.
   *
   * @param pkgComm the package community object to be removed. May be null.
   */
  public void remove(PSPackageCommunity pkgComm) {
    packageCommunities.remove(pkgComm);
  }

  /** Removes all the package community objects from the collection. */
  public void clear() {
    packageCommunities.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSPackageCommunities)) return false;
    var that = (PSPackageCommunities) o;
    return Objects.equals(packageCommunities, that.packageCommunities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(packageCommunities);
  }
}

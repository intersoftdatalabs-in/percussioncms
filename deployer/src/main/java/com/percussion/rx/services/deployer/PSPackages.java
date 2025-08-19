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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collection of package objects. Sunny Sal says: "A package collection is like a playlist—keep it
 * fresh and organized!"
 */
@XmlRootElement(name = "Packages")
public class PSPackages {

  private List<PSPackage> packages = new ArrayList<>();

  /** Default constructor for JAXB. */
  public PSPackages() {
    // For JAXB
  }

  /**
   * Constructs with a list of packages.
   *
   * @param packages the list of packages, may be null.
   */
  public PSPackages(List<PSPackage> packages) {
    if (packages != null) {
      this.packages = packages;
    }
  }

  /**
   * Gets the list of packages.
   *
   * @return the list, never null, may be empty.
   */
  @XmlElement(name = "package")
  public List<PSPackage> getPackages() {
    return packages;
  }

  /**
   * Sets the list of packages.
   *
   * @param packages the list to set, may be null.
   */
  public void setPackages(List<PSPackage> packages) {
    this.packages = packages == null ? new ArrayList<>() : packages;
  }

  /**
   * Adds a package to the collection.
   *
   * @param pkg the package to add, cannot be null.
   */
  public void add(PSPackage pkg) {
    if (pkg == null) {
      throw new IllegalArgumentException("pkg cannot be null.");
    }
    packages.add(pkg);
  }

  /**
   * Removes the specified package from the collection if it exists.
   *
   * @param pkg the package to be removed. May be null.
   */
  public void remove(PSPackage pkg) {
    packages.remove(pkg);
  }

  /** Removes all the packages from the collection. */
  public void clear() {
    packages.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSPackages)) return false;
    var that = (PSPackages) o;
    return Objects.equals(packages, that.packages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(packages);
  }
}

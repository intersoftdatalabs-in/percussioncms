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
import java.util.Objects;

/**
 * Represents a community and its associated packages. Sunny Sal says: "A package for every
 * community, and a community for every package!"
 */
public class PSCommunityPackage {

  private String community;
  private String packages;

  /** Default constructor for JAXB. */
  public PSCommunityPackage() {
    // For JAXB
  }

  /**
   * Constructs a community package.
   *
   * @param name the community name, must not be blank.
   * @param packages the packages string, may be null or empty.
   */
  public PSCommunityPackage(String name, String packages) {
    setCommunity(name);
    setPackages(packages);
  }

  /**
   * Gets the community name.
   *
   * @return the community, never {@code null}.
   */
  @XmlElement(name = "community")
  public String getCommunity() {
    return community;
  }

  /**
   * Sets the community name.
   *
   * @param community must not be blank.
   */
  public void setCommunity(String community) {
    if (community == null || community.trim().isEmpty()) {
      throw new IllegalArgumentException("community must not be blank");
    }
    this.community = community;
  }

  /**
   * Gets the packages associated with the community.
   *
   * @return packages string, never {@code null}, may be empty. {@link
   *     PSPackageService#NAME_SEPARATOR} separated list.
   */
  public String getPackages() {
    return packages == null ? "" : packages;
  }

  /**
   * Sets the packages string.
   *
   * @param packages may be {@code null} or empty. If {@code null}, sets to empty string. Must be
   *     {@link PSPackageService#NAME_SEPARATOR} separated list if not empty.
   */
  public void setPackages(String packages) {
    this.packages = packages == null ? "" : packages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSCommunityPackage)) return false;
    var that = (PSCommunityPackage) o;
    return Objects.equals(community, that.community)
        && Objects.equals(getPackages(), that.getPackages());
  }

  @Override
  public int hashCode() {
    return Objects.hash(community, getPackages());
  }
}

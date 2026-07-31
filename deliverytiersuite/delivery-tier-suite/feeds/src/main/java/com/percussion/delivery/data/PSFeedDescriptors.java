/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.data;

import tools.jackson.databind.annotation.JsonDeserialize;
import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A transfer class for feed descriptors and connection info. */
public class PSFeedDescriptors {
  @JsonDeserialize(as = ArrayList.class, contentAs = PSFeedDescriptor.class)
  private List<IPSFeedDescriptor> descriptors = new ArrayList<>();

  private String serviceUrl;
  private String serviceUser;
  private String servicePass;
  private boolean servicePassEncrypted;
  private String site;

  /** Default no-arg constructor required by Jackson serialization. */
  public PSFeedDescriptors() {
    super();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PSFeedDescriptors that = (PSFeedDescriptors) o;
    return servicePassEncrypted == that.servicePassEncrypted
        && Objects.equals(descriptors, that.descriptors)
        && Objects.equals(serviceUrl, that.serviceUrl)
        && Objects.equals(serviceUser, that.serviceUser)
        && Objects.equals(servicePass, that.servicePass)
        && Objects.equals(site, that.site);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        descriptors, serviceUrl, serviceUser, servicePass, servicePassEncrypted, site);
  }

  @Override
  public String toString() {
    return "PSFeedDescriptors{"
        + "descriptors="
        + descriptors
        + ", serviceUrl='"
        + serviceUrl
        + '\''
        + ", serviceUser='"
        + serviceUser
        + '\''
        + ", servicePass='"
        + servicePass
        + '\''
        + ", servicePassEncrypted="
        + servicePassEncrypted
        + ", site='"
        + site
        + '\''
        + '}';
  }

  /**
   * Constructs a fully-populated feed descriptors container.
   *
   * @param descriptors the list of feed descriptors, never <code>null</code>
   * @param serviceUrl the metadata service URL, never <code>null</code>
   * @param serviceUser the metadata service user name, never <code>null</code>
   * @param servicePass the metadata service password, never <code>null</code>
   * @param servicePassEncrypted <code>true</code> if the password is encrypted
   * @param site the site this container is associated with, never <code>null</code>
   */
  public PSFeedDescriptors(
      List<IPSFeedDescriptor> descriptors,
      String serviceUrl,
      String serviceUser,
      String servicePass,
      boolean servicePassEncrypted,
      String site) {
    this.descriptors = Objects.requireNonNull(descriptors, "descriptors cannot be null");
    this.serviceUrl = Objects.requireNonNull(serviceUrl, "serviceUrl cannot be null");
    this.serviceUser = Objects.requireNonNull(serviceUser, "serviceUser cannot be null");
    this.servicePass = Objects.requireNonNull(servicePass, "servicePass cannot be null");
    this.site = Objects.requireNonNull(site, "site cannot be null");
    this.servicePassEncrypted = servicePassEncrypted;
  }

  /**
   * Gets the list of feed descriptors.
   *
   * @return the descriptors
   */
  public List<IPSFeedDescriptor> getDescriptors() {
    return descriptors;
  }

  /**
   * Sets the list of feed descriptors.
   *
   * @param descriptors the descriptors to set
   */
  public void setDescriptors(List<IPSFeedDescriptor> descriptors) {
    this.descriptors = descriptors;
  }

  /**
   * Gets the metadata service URL.
   *
   * @return the serviceUrl
   */
  public String getServiceUrl() {
    return serviceUrl;
  }

  /**
   * Sets the metadata service URL.
   *
   * @param serviceUrl the serviceUrl to set
   */
  public void setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  /**
   * Gets the metadata service user name.
   *
   * @return the serviceUser
   */
  public String getServiceUser() {
    return serviceUser;
  }

  /**
   * Sets the metadata service user name.
   *
   * @param serviceUser the serviceUser to set
   */
  public void setServiceUser(String serviceUser) {
    this.serviceUser = serviceUser;
  }

  /**
   * Gets the metadata service password.
   *
   * @return the servicePass
   */
  public String getServicePass() {
    return servicePass;
  }

  /**
   * Sets the metadata service password.
   *
   * @param servicePass the servicePass to set
   */
  public void setServicePass(String servicePass) {
    this.servicePass = servicePass;
  }

  /**
   * Indicates whether the metadata service password is encrypted.
   *
   * @return the servicePassEncrypted
   */
  public boolean isServicePassEncrypted() {
    return servicePassEncrypted;
  }

  /**
   * Sets whether the metadata service password is encrypted.
   *
   * @param servicePassEncrypted the servicePassEncrypted to set
   */
  public void setServicePassEncrypted(boolean servicePassEncrypted) {
    this.servicePassEncrypted = servicePassEncrypted;
  }

  /**
   * Gets the site this container is associated with.
   *
   * @return the site
   */
  public String getSite() {
    return site;
  }

  /**
   * Sets the site this container is associated with.
   *
   * @param site the site to set
   */
  public void setSite(String site) {
    this.site = site;
  }
}

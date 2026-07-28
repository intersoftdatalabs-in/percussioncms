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
package com.percussion.delivery.multitenant;

import com.percussion.delivery.multitenant.IPSTenantAuthorization.Status;
import org.apache.commons.lang3.Validate;

/**
 * Represents the status information returned by a call to the license service.
 *
 * @author natechadwick
 */
public class PSLicenseStatus {

  /** The textual license status returned by the licensing service. */
  private String licenseStatus = "";

  /** The company name associated with the license. */
  private String company = "";

  /** The license type description (for example, evaluation, production). */
  private String licenseType = "";

  /** The activation status of the license. */
  private String activationStatus = "";

  /** The maximum number of sites allowed under the license. */
  private String maxSites = "";

  /** The maximum number of pages allowed under the license. */
  private String maxPages = "";

  /** The maximum number of API calls per period allowed under the license. */
  private String maxApiCalls = "";

  /** Default constructor. */
  public PSLicenseStatus() {}
  ;

  /**
   * Returns the status code for the current license status string.
   *
   * @return the {@link Status} constant that matches the stored {@link #licenseStatus}, never
   *     <code>null</code>.
   */
  public Status getStatusCode() {

    Status ret = Status.UNEXPECTED_ERROR;

    if (this.licenseStatus.equals("SUCCESS")) ret = Status.SUCCESS;
    else if (this.licenseStatus.equals("UNEXPECTED_ERROR")) ret = Status.UNEXPECTED_ERROR;
    else if (this.licenseStatus.equals("EXCEEDED_QUOTA")) ret = Status.EXCEEDED_QUOTA;
    else if (this.licenseStatus.equals("NO_ACCOUNT_EXISTS")) ret = Status.NO_ACCOUNT_EXISTS;
    else if (this.licenseStatus.equals("NOT_ACTIVE")) ret = Status.NOT_ACTIVE;
    else if (this.licenseStatus.equals("SUSPENDED")) ret = Status.SUSPENDED;

    return ret;
  }

  /**
   * Returns the license status string returned by the licensing service.
   *
   * @return the license status string, never <code>null</code>.
   */
  public String getLicenseStatus() {
    return licenseStatus;
  }

  /**
   * Sets the license status string returned by the licensing service.
   *
   * @param licenseStatus the license status string, never <code>null</code>.
   */
  public void setLicenseStatus(String licenseStatus) {
    Validate.notNull(licenseStatus);

    this.licenseStatus = licenseStatus;
  }

  /**
   * Returns the company name associated with the license.
   *
   * @return the company name, never <code>null</code>.
   */
  public String getCompany() {

    return company;
  }

  /**
   * Sets the company name associated with the license.
   *
   * @param company the company name, never <code>null</code>.
   */
  public void setCompany(String company) {

    Validate.notNull(company);

    this.company = company;
  }

  /**
   * Returns the license type description.
   *
   * @return the license type, never <code>null</code>.
   */
  public String getLicenseType() {
    return licenseType;
  }

  /**
   * Sets the license type description.
   *
   * @param licenseType the license type, never <code>null</code>.
   */
  public void setLicenseType(String licenseType) {

    Validate.notNull(licenseType);

    this.licenseType = licenseType;
  }

  /**
   * Returns the activation status of the license.
   *
   * @return the activation status, never <code>null</code>.
   */
  public String getActivationStatus() {
    return activationStatus;
  }

  /**
   * Sets the activation status of the license.
   *
   * @param activationStatus the activation status, never <code>null</code>.
   */
  public void setActivationStatus(String activationStatus) {

    Validate.notNull(activationStatus);

    this.activationStatus = activationStatus;
  }

  /**
   * Returns the maximum number of sites allowed under the license.
   *
   * @return the maximum sites, never <code>null</code>.
   */
  public String getMaxSites() {
    return maxSites;
  }

  /**
   * Sets the maximum number of sites allowed under the license.
   *
   * @param maxSites the maximum sites, never <code>null</code>.
   */
  public void setMaxSites(String maxSites) {
    Validate.notNull(maxSites);

    this.maxSites = maxSites;
  }

  /**
   * Returns the maximum number of pages allowed under the license.
   *
   * @return the maximum pages, never <code>null</code>.
   */
  public String getMaxPages() {
    return maxPages;
  }

  /**
   * Sets the maximum number of pages allowed under the license.
   *
   * @param maxPages the maximum pages, never <code>null</code>.
   */
  public void setMaxPages(String maxPages) {

    Validate.notNull(maxPages);

    this.maxPages = maxPages;
  }

  /**
   * Returns the maximum number of API calls per period allowed under the license.
   *
   * @return the maximum API calls, never <code>null</code>.
   */
  public String getMaxApiCalls() {
    return maxApiCalls;
  }

  /**
   * Sets the maximum number of API calls per period allowed under the license.
   *
   * @param maxApiCalls the maximum API calls, never <code>null</code>.
   */
  public void setMaxApiCalls(String maxApiCalls) {

    Validate.notNull(maxApiCalls);

    this.maxApiCalls = maxApiCalls;
  }
}

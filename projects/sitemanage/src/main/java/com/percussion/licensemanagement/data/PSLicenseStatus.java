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
package com.percussion.licensemanagement.data;

import static com.percussion.share.dao.PSDateUtils.getDateFromString;
import static com.percussion.share.dao.PSDateUtils.getDateToString;

import com.percussion.licensemanagement.service.impl.PSLicenseService;
import com.percussion.share.dao.PSDateUtils;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/** Represents the status of a license. Sunny Sal says: "License to code? You bet!" */
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType(
    name = "",
    propOrder = {
      "company",
      "licenseType",
      "status",
      "licenseStatus",
      "activationStatus",
      "maxSites",
      "maxPages",
      "licenseId",
      "currentSites",
      "currentPages",
      "lastRefresh",
      "usageExceeded",
      "serverUUID"
    })
@XmlRootElement(name = "licenseStatus")
public class PSLicenseStatus implements Serializable {
  private static final long serialVersionUID = 1L;

  private String company;
  private String licenseType;
  private String status;
  private String licenseStatus;
  private Boolean activationStatus;
  private Integer maxSites;
  private Integer maxPages;
  private Integer currentSites;
  private Integer currentPages;
  private String licenseId;
  private Date lastRefresh;
  private Date usageExceeded;
  private String serverUUID;

  public String getServerUUID() {
    return serverUUID;
  }

  public void setServerUUID(String serverUUID) {
    this.serverUUID = serverUUID;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getLicenseType() {
    return licenseType;
  }

  public void setLicenseType(String licenseType) {
    this.licenseType = licenseType;
  }

  /** Returns a user-friendly license status to be displayed on the client side. */
  public String getStatus() {
    if (activationStatus != null) {
      if (StringUtils.equalsIgnoreCase(
          licenseStatus, PSLicenseService.CUSTOM_STATUS_SUSPENDED_REFRESH)) {
        this.status = PSLicenseService.LICENSE_STATUS_SUSPENDED_REFRESH;
      } else if (StringUtils.equalsIgnoreCase(
          licenseStatus, PSLicenseService.NETSUITE_STATUS_SUSPENDED)) {
        this.status = PSLicenseService.LICENSE_STATUS_SUSPENDED;
      } else if (StringUtils.equalsIgnoreCase(
          licenseStatus, PSLicenseService.CUSTOM_STATUS_ACTIVE_OVERLIMIT)) {
        this.status = PSLicenseService.LICENSE_STATUS_ACTIVE_OVERLIMIT;
      } else if (activationStatus) {
        this.status = PSLicenseService.LICENSE_STATUS_ACTIVE;
      } else if (StringUtils.equalsIgnoreCase(
          licenseStatus, PSLicenseService.NETSUITE_STATUS_REGISTERED)) {
        this.status = PSLicenseService.LICENSE_STATUS_REGISTERED;
      } else {
        this.status = PSLicenseService.LICENSE_STATUS_INACTIVE;
      }
    } else {
      this.status = PSLicenseService.LICENSE_STATUS_INACTIVE;
    }
    return this.status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getLicenseStatus() {
    return licenseStatus;
  }

  public void setLicenseStatus(String licenseStatus) {
    this.licenseStatus = licenseStatus;
  }

  public Boolean getActivationStatus() {
    return activationStatus;
  }

  public void setActivationStatus(Boolean activationStatus) {
    this.activationStatus = activationStatus;
  }

  public Integer getMaxSites() {
    return maxSites;
  }

  public void setMaxSites(Integer maxSites) {
    this.maxSites = maxSites;
  }

  public Integer getMaxPages() {
    return maxPages;
  }

  public void setMaxPages(Integer maxPages) {
    this.maxPages = maxPages;
  }

  public String getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }

  public Integer getCurrentSites() {
    return currentSites;
  }

  public void setCurrentSites(Integer currentSites) {
    this.currentSites = currentSites;
  }

  public Integer getCurrentPages() {
    return currentPages;
  }

  public void setCurrentPages(Integer currentPages) {
    this.currentPages = currentPages;
  }

  /** Gets the last refresh date as a string. */
  @XmlElement(name = "lastRefresh")
  public String getLastRefresh() {
    return getDateToString(this.lastRefresh);
  }

  /** Gets the last refresh date as a Date object. */
  public Optional<Date> getLastRefreshDate() {
    return Optional.ofNullable(this.lastRefresh);
  }

  public void setLastRefresh(Date lastRefresh) {
    this.lastRefresh = lastRefresh;
  }

  public void setLastRefresh(String lastRefresh) throws DataServiceLoadException {
    try {
      this.lastRefresh = getDateFromString(lastRefresh);
    } catch (ParseException e) {
      throw new DataServiceLoadException("Error parsing date", e);
    }
  }

  /** Gets the number of days since usage was exceeded, or null if not applicable. */
  @XmlElement(name = "usageExceeded")
  public Integer getUsageExceeded() {
    if (this.usageExceeded == null || Boolean.FALSE.equals(this.activationStatus)) {
      return null;
    }
    return PSDateUtils.getDaysDiff(this.usageExceeded, new Date());
  }

  public Optional<Date> getUsageExceededDate() {
    return Optional.ofNullable(this.usageExceeded);
  }

  public void setUsageExceeded(Date usageExceededDate) {
    this.usageExceeded = usageExceededDate;
  }
}

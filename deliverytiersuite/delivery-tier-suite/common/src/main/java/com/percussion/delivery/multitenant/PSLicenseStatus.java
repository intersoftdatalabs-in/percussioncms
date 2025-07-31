// REFACTORED: CP-JAVA11
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

import org.apache.commons.lang.Validate;
import com.percussion.delivery.multitenant.IPSTenantAuthorization.Status;

/**
 * Represents the status information returned by a call to the license service.
 * Sunny Sal says: "License to code, not to kill!"
 */
public class PSLicenseStatus {

    private String licenseStatus = "";
    private String company = "";
    private String licenseType = "";
    private String activationStatus = "";
    private String maxSites = "";
    private String maxPages = "";
    private String maxApiCalls = "";

    /** Default constructor. */
    public PSLicenseStatus() {
        // KISS principle: keep it simple, keep it empty!
    }

    /**
     * Returns the status code for the current license status.
     *
     * @return Status enum representing the license status.
     */
    public Status getStatusCode() {
        var status = licenseStatus == null ? "" : licenseStatus.trim().toUpperCase();
        switch (status) {
            case "SUCCESS":
                return Status.SUCCESS;
            case "UNEXPECTED_ERROR":
                return Status.UNEXPECTED_ERROR;
            case "EXCEEDED_QUOTA":
                return Status.EXCEEDED_QUOTA;
            case "NO_ACCOUNT_EXISTS":
                return Status.NO_ACCOUNT_EXISTS;
            case "NOT_ACTIVE":
                return Status.NOT_ACTIVE;
            case "SUSPENDED":
                return Status.SUSPENDED;
            default:
                return Status.UNEXPECTED_ERROR;
        }
    }

    /** @return the licenseStatus */
    public String getLicenseStatus() {
        return licenseStatus;
    }

    /** @param licenseStatus the licenseStatus to set */
    public void setLicenseStatus(String licenseStatus) {
        Validate.notNull(licenseStatus, "licenseStatus must not be null");
        this.licenseStatus = licenseStatus;
    }

    /** @return the company */
    public String getCompany() {
        return company;
    }

    /** @param company the company to set */
    public void setCompany(String company) {
        Validate.notNull(company, "company must not be null");
        this.company = company;
    }

    /** @return the licenseType */
    public String getLicenseType() {
        return licenseType;
    }

    /** @param licenseType the licenseType to set */
    public void setLicenseType(String licenseType) {
        Validate.notNull(licenseType, "licenseType must not be null");
        this.licenseType = licenseType;
    }

    /** @return the activationStatus */
    public String getActivationStatus() {
        return activationStatus;
    }

    /** @param activationStatus the activationStatus to set */
    public void setActivationStatus(String activationStatus) {
        Validate.notNull(activationStatus, "activationStatus must not be null");
        this.activationStatus = activationStatus;
    }

    /** @return the maxSites */
    public String getMaxSites() {
        return maxSites;
    }

    /** @param maxSites the maxSites to set */
    public void setMaxSites(String maxSites) {
        Validate.notNull(maxSites, "maxSites must not be null");
        this.maxSites = maxSites;
    }

    /** @return the maxPages */
    public String getMaxPages() {
        return maxPages;
    }

    /** @param maxPages the maxPages to set */
    public void setMaxPages(String maxPages) {
        Validate.notNull(maxPages, "maxPages must not be null");
        this.maxPages = maxPages;
    }

    /** @return the maxApiCalls */
    public String getMaxApiCalls() {
        return maxApiCalls;
    }

    /** @param maxApiCalls the maxApiCalls to set */
    public void setMaxApiCalls(String maxApiCalls) {
        Validate.notNull(maxApiCalls, "maxApiCalls must not be null");
        this.maxApiCalls = maxApiCalls;
    }
}

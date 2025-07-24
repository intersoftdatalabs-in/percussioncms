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

import java.util.Date;

/**
 * Represents information about an authorized tenant and maintains
 * the state of the tenant between requests.
 *
 * @author natechadwick
 */
public class PSTenantInfo implements IPSTenantInfo {
    private String tenantid;
    private long api_counter;
    private Date api_start;
    private Date last_authdate;
    private PSLicenseStatus status;

    @Override
    public void setTenantId(String id) {
        this.tenantid = id;
    }

    @Override
    public String getTenantId() {
        return this.tenantid;
    }

    @Override
    public long getAPIUsage() {
        return this.api_counter;
    }

    @Override
    public void addAPIUsage(long value) {
        this.api_counter += value;
    }

    @Override
    public void clearAPIUsage() {
        this.api_counter = 0;
        this.api_start = new Date();
    }

    @Override
    public void setAPIUsageStart(Date start) {
        this.api_start = start;
    }

    @Override
    public Date getAPIUsageStart() {
        return this.api_start;
    }

    @Override
    public Date getLastAuthorizationCheckDate() {
        return this.last_authdate;
    }

    @Override
    public void setLastAuthorizationCheckDate(Date date) {
        this.last_authdate = date;
    }

    @Override
    public PSLicenseStatus getLicenseStatus() {
        return this.status;
    }

    @Override
    public void setLicenseStatus(PSLicenseStatus status) {
        this.status = status;
    }

}
